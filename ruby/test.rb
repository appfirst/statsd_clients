require './afstatsd.rb'

#statsd = Statsd.new 'statsd_server.my_company.com', 8125, 20

statsd = Statsd.new     # use defaults
statsd.namespace = 'ruby.clark'

statsd.increment 'counter1'
statsd.increment 'counter1'
statsd.decrement 'counter1'        #counters accumulate

statsd.gauge 'gauge1', 1024
statsd.gauge 'gauge1', 1025
statsd.gauge 'gauge1', 1026
statsd.gauge 'gauge1', 1027      
statsd.gauge 'gauge1', 1028        # gauges get averaged when aggregated

statsd.time('timing1' ){sleep 0.01}        
statsd.time('timing1' ){sleep 0.02}
statsd.time('timing1' ){sleep 0.03}
statsd.time('timing1' ){sleep 0.04}    # timings get averaged when aggregated


=begin

100.times do 
    #statsd.increment 'sampled', 0.1, 'sampled'
    statsd.increment 'sampled'
end

statsd.set 'set1', 1099, "ez"    

for i in 10..19 do
    statsd.increment "counter#{i}"  # create a group of counters
end    

1000.times do 
    statsd.increment 'fast'            # don't do this if aggregation is off
end    

# In this test program, this will give the aggregator time to run.
15.times do
    sleep 2
    statsd.increment 'slow'             
end

=end

=begin
# test for thread safety
threads = []
start = Time.now
for i in 0..9 do
    threads << Thread.new(i) do |j| 
        start = Time.now
        1000000.times do
            statsd.increment 'inthethread'
#            sleep(0.01)
        end
        puts "thread #{j} says: I took #{((Time.now - start)*1000).round} ms"
    end
end
threads.each { |t| t.join }

puts "total time: #{((Time.now - start)*1000).round} ms"
=end

puts "#{statsd.dropped} messages dropped"
