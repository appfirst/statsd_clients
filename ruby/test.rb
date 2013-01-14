require './afstatsd.rb'

statsd = Statsd.new '127.0.0.1', 8125, 9
statsd.namespace = 'ruby.clark'


statsd.increment 'counter1'
statsd.increment 'counter1'
statsd.decrement 'counter1'		#counters accumulate
statsd.gauge 'gauge1', 1024
statsd.gauge 'gauge1', 1025
statsd.gauge 'gauge1', 1026
statsd.gauge 'gauge1', 1027		# gauges overwrite previous value
statsd.set 'set1', 1099, "ez"	
#100.times do 
#	statsd.increment 'sampled', 0.1, 'sampled'
#end
statsd.time('timing1' ){sleep 0.01}		
statsd.time('timing1' ){sleep 0.02}
statsd.time('timing1' ){sleep 0.03}
statsd.time('timing1' ){sleep 0.04}	# timings get averaged when aggregated

for i in 10..19 do
	statsd.increment "counter#{i}"  # create a group of counters
end	

1000.times do 
	statsd.increment 'fast'			# don't do this if aggregation is off
end	


# In this test program, this will give the aggregator time to run.
5.times do
	sleep 2
	statsd.increment 'slow'			# 
end


=begin
# test for thread safety
threads = []
start = Time.now
for i in 0..9 do
	threads << Thread.new(i) do |j| 
		start = Time.now
		10000.times do
			statsd.increment 'inthethread'
			#sleep(0.01)
		end
		puts "thread #{j} says: I took #{((Time.now - start)*1000).round} ms"
	end
end
threads.each { |t| t.join }

puts "total time: #{((Time.now - start)*1000).round} ms"

sleep 10  # give the aggregator time to fire
=end



puts "#{statsd.dropped} messages dropped"
