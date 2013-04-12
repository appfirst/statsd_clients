require 'socket'
require 'forwardable'
require 'rubygems'
require 'posix_mq'
require 'afstatsd/statsd_metrics'
require 'afstatsd/statsd_aggregator'
require 'monitor'
require 'fcntl'

# = Statsd: A Statsd client (https://github.com/etsy/statsd)
#
# @example Set up a global Statsd client for a server on localhost:9125, 
#                                aggregate 20 seconds worth of metrics
#   $statsd = Statsd.new 'localhost', 8125, 20
# @example Send some stats
#   $statsd.increment 'garets'
#   $statsd.timing 'glork', 320
#   $statsd.gauge 'bork', 100
# @example Use {#time} to time the execution of a block
#   $statsd.time('account.activate') { @account.activate! }
# @example Create a namespaced statsd client and increment 'account.activate'
#   statsd = Statsd.new('localhost').tap{|sd| sd.namespace = 'account'}
#   statsd.increment 'activate'
#
# Statsd instances are thread safe for general usage, by using a thread local
# UDPSocket and carrying no state. The attributes are stateful, and are not
# mutexed, it is expected that users will not change these at runtime in
# threaded environments. If users require such use cases, it is recommend that
# users either mutex around their Statsd object, or create separate objects for
# each namespace / host+port combination.
class Statsd

    # A namespace to prepend to all statsd calls.
    attr_reader :namespace

    # StatsD host. Defaults to 127.0.0.1.  Only used with UDP transport
    attr_reader :host

    # StatsD port. Defaults to 8125.  Only used with UDP transport
    attr_reader :port

    # StatsD namespace prefix, generated from #namespace
    attr_reader :prefix

    # a postfix to append to all metrics
    attr_reader :postfix 

    # count of messages that were dropped due to transmit error
    attr_reader :dropped

    # Turn on debug messages
    attr_accessor :debugging

    class << self
        # Set to a standard logger instance to enable debug logging.
        attr_accessor :logger
    end

    # @param [String] host your statsd host
    # @param [Integer] port your statsd port
    # @param [Integer] interval for aggregatore
    def initialize(host = '127.0.0.1', port = 8125, interval = 20)
        self.host, self.port = host, port
        @prefix = nil
        @postfix = nil
        @aggregator = StatsdAggregator.new(interval)
        set_transport :mq_transport
        self.aggregating = true unless interval == 0
        @dropped = 0
        @debugging = false
    end

    # @param [method] The ruby symbol for the method that gets called to send
    # one metric to the server.  eg: set_transport :udp_transport
    def set_transport(transport)
        @transport = method(transport)
        @aggregator.transport = @transport  # aggregator needs to know
    end

    # @attribute [Boolean] Turn aggregation on or off
    def aggregating= (should_aggregate)
        if should_aggregate
            @aggregator.start(@transport)
        else
            @aggregator.stop
        end
    end

    # is the aggregator running?
    def aggregating
        @aggregator.running
    end

    # @attribute [w] namespace
    #   Writes are not thread safe.
    def namespace=(namespace)
        @namespace = namespace
        @prefix = "#{namespace}."
    end

    # @attribute [w] postfix
    #   A value to be appended to the stat name after a '.'. If the value is
    #   blank then the postfix will be reset to nil (rather than to '.').
    def postfix=(pf)
        case pf
        when nil, false, '' then @postfix = nil
        else @postfix = ".#{pf}"
        end
    end

    # @attribute [Numeric] interval
    #   Set aggregation interval
    def interval=(iv)
        @aggregator.set_interval(iv)
    end

    # @attribute [w] host
    #   Writes are not thread safe.
    def host=(host)
        @host = host || '127.0.0.1'
    end

    # @attribute [w] port
    #   Writes are not thread safe.
    def port=(port)
        @port = port || 8125
    end

    # Sends an increment (count = 1) for the given stat to the statsd server.
    #
    # @param [String] stat stat name
    # @param [Numeric] sample_rate sample rate, 1 for always
    # @param [String] optional note (AppFirst extension to StatsD)
    # @see #count
    def increment(stat, sample_rate=1, note="")
        count stat, 1, sample_rate, note
    end

    # Sends a decrement (count = -1) for the given stat to the statsd server.
    #
    # @param [String] stat stat name
    # @param [Numeric] sample_rate sample rate, 1 for always
    # @param [String] optional note (AppFirst extension to StatsD)
    # @see #count
    def decrement(stat, sample_rate=1, note="")
        count stat, -1, sample_rate, note
    end

    # Sends an arbitrary count for the given stat to the statsd server.
    #
    # @param [String] stat stat name
    # @param [Integer] count count
    # @param [Numeric] sample_rate sample rate, 1 for always
    # @param [String] optional note (AppFirst extension to StatsD)
    def count(stat, count, sample_rate=1, note="")
        if sample_rate == 1 or rand < sample_rate
            send_metric StatsdMetrics::CMetric.new(expand_name(stat), count.round, sample_rate, note)
        end
    end

    # Sends an arbitary gauge value for the given stat to the statsd server.
    #
    # This is useful for recording things like available disk space,
    # memory usage, and the like, which have different semantics than
    # counters.
    #
    # @param [String] stat stat name.
    # @param [Numeric] value gauge value.
    # @param [String] optional note (AppFirst extension to StatsD)
    # @example Report the current user count:
    #   $statsd.gauge('user.count', User.count)
    def gauge(stat, value, note="")
        send_metric StatsdMetrics::GMetric.new(expand_name(stat), value, note)
    end

    # Sends an arbitary set value for the given stat to the statsd server.
    #
    # This is for recording counts of unique events, which are useful to
    # see on graphs to correlate to other values.  For example, a deployment
    # might get recorded as a set, and be drawn as annotations on a CPU history
    # graph.
    #
    # @param [String] stat stat name.
    # @param [Numeric] value event value.
    # @param [String] optional note (AppFirst extension to StatsD)
    # @example Report a deployment happening:
    #   $statsd.set('deployment', DEPLOYMENT_EVENT_CODE)
    def set(stat, value, note="")
        send_metric StatsdMetrics::SMetric.new(expand_name(stat), value, note)
    end

    # Sends a timing (in ms) for the given stat to the statsd server. The
    # sample_rate determines what percentage of the time this report is sent. The
    # statsd server then uses the sample_rate to correctly track the average
    # timing for the stat.
    #
    # @param [String] stat stat name
    # @param [Integer] ms timing in milliseconds
    # @param [Numeric] sample_rate sample rate, 1 for always
    # @param [String] optional note (AppFirst extension to StatsD)
    def timing(stat, ms, sample_rate=1, note="")
        if sample_rate == 1 or rand < sample_rate
            send_metric StatsdMetrics::TMetric.new(expand_name(stat), ms.round, sample_rate, note)
        end    
    end

    # Reports execution time of the provided block using {#timing}.
    #
    # @param [String] stat stat name
    # @param [Numeric] sample_rate sample rate, 1 for always
    # @param [String] optional note (AppFirst extension to StatsD)
    # @yield The operation to be timed
    # @see #timing
    # @example Report the time (in ms) taken to activate an account
    #   $statsd.time('account.activate') { @account.activate! }
    def time(stat, sample_rate=1, note="")
        start = Time.now
        result = yield
        timing(stat, ((Time.now - start) * 1000).round, sample_rate, note)
        result
    end

    protected

    def send_metric(metric)
        # All the metric types above funnel to here.  We will send or aggregate.
        if aggregating
            @aggregator.add metric
        else 
            @transport.call(metric)
        end
    end

    def expand_name(name)
        # Replace Ruby module scoping with '.' and reserved chars (: | @) with underscores.
        name = name.to_s.gsub('::', '.').tr(':|@', '_') 
        "#{prefix}#{name}#{postfix}"
    end

    def udp_transport(metric)
        if @debugging
            puts "socket < #{metric}\n" #debug
        end    
        self.class.logger.debug { "Statsd: #{metric}" } if self.class.logger
        socket.send(metric.to_s, 0, @host, @port)
        rescue => boom
            #puts "socket send error"
            @dropped +=1
            self.class.logger.debug { "Statsd: #{boom.class} #{boom}" } if self.class.logger
            nil
    end

    STATSD_SEVERITY = 3
    def mq_transport(metric)
        if @debugging
            puts "MQ < #{metric}\n" #debug
        end    
        self.class.logger.debug { "Statsd: #{metric}" } if self.class.logger
        if not @mq 
            begin
                @mq = POSIX_MQ.new("/afcollectorapi", Fcntl::O_WRONLY | Fcntl::O_NONBLOCK)
            rescue => boom
                self.class.logger.debug { "Statsd: MQ open error #{boom.class} #{boom}" } if self.class.logger
                # failed to open MQ.  Fall back to UPD transport.  Note:  Current message will be lost.
                @dropped += 1
                # puts "fallback to udp"
                set_transport :udp_transport
                return nil
            end    
        end
        begin
            @mq.send(metric.to_s, STATSD_SEVERITY)
        rescue => boom
            # just drop it on the floor
            @dropped += 1
            #puts "MQ send error: #{boom.class} #{boom}"
            self.class.logger.debug { "Statsd: MQ Send Error#{boom.class} #{boom}" } if self.class.logger
            nil
        end    
    end

    def both_transport(metric)
        mq_transport(metric)
        udp_transport(metric)
    end
    
    private

    def socket
        Thread.current[:statsd_socket] ||= UDPSocket.new
    end
    
end     # class Statsd


