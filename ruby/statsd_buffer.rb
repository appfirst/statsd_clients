# Class used to aggregate statsd metrics.  Buffers up and aggregates the messages, and kicks them out at a specified interval.
# Note that the timer is the time from the end of the last flush to the start of the next.  It will drift by the time
# it takes to flush the buffer.  Keeps it simple.

class StatsdBuffer

	attr_accessor :transport

	def initialize(interval=5)
		@buf = {}
		@interval = interval
		@timer = nil
		@mutex = Mutex.new	
		@buffering = false
	end	
  
	def start_buffering(transport)
		return if @buffering  # already started
		@transport = transport
		@timer = Thread.new {loop {sleep @interval; flush_buffer}}
		@buffering = true
		#puts "buffering started.  Interval=#{@interval}"
	end
	
	def stop_buffering
		return if not @buffering	# already stopped
		flush_buffer					
		@timer.kill if @timer
		@timer = nil
		@buffering = false
		#puts "buffering stopped"
	end
  
	def buffering
		@buffering
	end
	

	def set_interval(interval)
		@interval = interval
	end
    
	def add(metric)
		@mutex.synchronize do
			if m = @buf[metric.name] 
				m.aggregate metric.value
			else
				@buf[metric.name] = metric
			end	
		end
	end

	def flush_buffer
		tmp = {}
		@mutex.synchronize do
			tmp = @buf
			@buf = {}
		end 
		tmp.each_value do |m|
			@transport.call(m)
		end
	end
	
	def pop_each(&block)
		tmp = {}
		@mutex.synchronize do
			tmp = @buf
			@buf = {}
		end 
		tmp.each_value do |m|
			yield m
		end	
	end
end
