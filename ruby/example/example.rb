require 'afstatsd'

#$statsd = Statsd.new 'localhost', 8125, 0, 'udp'
$statsd = Statsd.new     # use defaults

$statsd.namespace = 'test.ruby'

$statsd.increment 'counter1'
$statsd.increment 'counter1'
$statsd.decrement 'counter1'        #counters accumulate

$statsd.gauge 'gauge1', 1024
$statsd.gauge 'gauge1', 1025
$statsd.gauge 'gauge1', 1026
$statsd.gauge 'gauge1', 1027
$statsd.gauge 'gauge1', 1028        # gauges get overwritten when aggregated

$statsd.time('timing1'){sleep 0.1}
$statsd.time('timing1'){sleep 0.2}
$statsd.time('timing1'){sleep 0.3}
$statsd.time('timing1'){sleep 0.4}    # timings get averaged when aggregated


=begin

100.times do
    $statsd.increment 'sampled', 0.1
end

$statsd.set 'set1', 1099

for i in 10..19 do
    $statsd.increment "counter#{i}"  # create a group of counters
end

1000.times do
    $statsd.increment 'fast'            # don't do this if aggregation is off
end

15.times do
    sleep 2
    $statsd.increment 'slow'
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
            $statsd.increment 'inthethread'
            sleep(0.01)
        end
        puts "thread #{j} says: I took #{((Time.now - start)*1000).round} ms"
    end
end
threads.each { |t| t.join }

puts "total time: #{((Time.now - start)*1000).round} ms"
=end

puts "#{$statsd.dropped} messages dropped"
