# Statsd Aggregator
#
# Used to aggregate metrics in a threaded environment.  Only one of these 
# should be created, in the main thread.
# For each thread, we create 2 buffers.  The thread will be writing to 
# one, while the aggregator reads from the other.  The aggregator will
# control which set is which.
  
  
 class StatsdAggregator 
    attr_accessor :transport

    def initialize(interval=20)
        @interval = interval
        @timer = nil
        @mutex = Mutex.new    
        @running = false
        @left_buffers = {}        # 2 buffer groups
        @right_buffers = {}       #   each buffer group is a hash
        @rbufs = @left_buffers    # buffer group currently being read from
        @wbufs = @right_buffers   # buffer group currently being written to
        at_exit do
            if @running
                flush_buffers 
                swap_buffers
                flush_buffers
            end 
        end
    end    

    def start(transport)
        @transport = transport
        return if @running  # already started
        # Spin up a thread to periodically send the aggregated stats.
        # Divide the interval in half to allow other threads to finish
        # their writes after we swap, and before we start reading.
        @timer = Thread.new do
            loop do
                sleep @interval/2
                swap_buffers
                sleep @interval/2
                flush_buffers
            end
        end
        @running = true
        #puts "aggregation started.  Interval=#{@interval}"
    end
    
    def stop
        return if not @running    # already stopped
        flush_buffers                    
        @timer.kill if @timer
        @timer = nil
        @running = false
        #puts "aggregation stopped"
    end
  
    def set_interval(interval)
        @interval = interval
    end
    
    # the following methods are thread safe
    
    def running
        @running
    end

    # this is the only method that should be used by child threads.
    def add(metric)
        # We should have a write buffer assigned to our thread.  
        # Create one if not.
        unless write_buffer = @wbufs[Thread.current]
            #puts "Thread #{Thread.current}: creating write_buffer"
            write_buffer = {}
            # get a lock before we mess with the global hash
            @mutex.synchronize do
                @wbufs[Thread.current] = write_buffer
            end
        end
        if m = write_buffer[metric.name]
            # if we are already collecting this metric, just aggregate the new value
            m.aggregate metric.value
        else
            # otherwise, add this metric to the aggregation buffer
            #puts "Thread #{Thread.current}: creating metric"
            write_buffer[metric.name] = metric
        end
        #puts "Thread #{Thread.current}: Added metric: #{metric}"
    end
    
    private
    
    # Next two methods are called at different times during the interval,
    # so any writes in progress after the swap will have time to complete.

    def swap_buffers 
        if @rbufs == @left_buffers
            @rbufs = @right_buffers
            @wbufs = @left_buffers
        else
            @rbufs = @left_buffers
            @wbufs = @right_buffers
        end
    end
    
    def flush_buffers
        # Each thread has it's own read buffer.  If it's empty, the
        # thread might be dead.  We'll delete it's read buffer.
        @rbufs.delete_if { |k, rb| rb.empty? }
        
        # If not empty, aggregate all the data across all the threads, 
        # then send.
        send_buffer = {}
        @rbufs.each_value do |rb|
            rb.each_value do |metric|
                if m = send_buffer[metric.name]
                    m.aggregate metric.value
                else    
                    send_buffer[metric.name] = metric
                end    
            end
            # once we've aggregated all the metrics from this 
            # thread, clear out the buffer, but don't remove it.        
            rb.clear
        end    
        #puts "nothing to send" if send_buffer.empty? 
        send_buffer.each_value do |metric|
            @transport.call(metric)
        end    
    end
    
end    # class StatsdAggregator