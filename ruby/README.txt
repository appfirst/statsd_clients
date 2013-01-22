A statsd client for use with the AppFirst collector.  Based on the statsd 
client by reinh: https://github.com/reinh/statsd (version 1.2.0)

This version does not inlcude the batch capability of the original, but does
include an aggregation capability.  Data is transported to the AppFirst 
collector via a POSIX Message Queue.  The Posix Message Queue is not part 
of the standard ruby library, and must be installed prior to using this 
statsd client.  'gem install posix_mq' should do the job (version 2.0.0)

If the message queue is not found, the client will fallback to using a UDP 
socket as a backup transport method.  For a detailed discussion of statsd, 
see https://github.com/etsy/statsd

Usage:

At startup, instantiate a Statsd client object:

	$statsd = Statsd.new 
    
               or 
                
	$statsd = Statsd.new 'localhost', 8125, 20
	
This will create a new client, with optional UDP address and port numbers for the 
fallback case.  The client object in this example is a global for convenience, 
but this is not necessary.  The third parameter is the timing interval for 
metric aggregation, and defaults to 20 sec.  

The aggregation feature will collect statistics over a period of time, and aggregate 
them to reduce the amount of data that needs to be transffered.  For example, if an
individual counter is incrmented 100 times during the aggregation interval, only one 
message will be sent, with a count of 100, rather than 100 messages with count of one.
The drawback to using this method is that it introduces latency in reporting the metrics.

Aggregation can be disabled by setting the interval to 0, or by setting the 
aggregating attribute to false:

	$statsd.aggregating = false.
	
The message queue is configured by the AppFirst collector.  At the current 
time, it is set accept about 200 messages per second.  If overrun, 
messages will be dropped.  This is another reason to use the aggregation 
capability.  Dropped messages are counted and can be accessed with the 
$statsd.dropped attribute.

The namespace attribute, if specified, will be prepended to all metrics.  

	$statsd.namespace = 'system.component'

This statsd client can also send data the 'standard' way using a UDP socket:

    $statsd.set_transport(:udp_transport)
    
Once these attributes are set, the client is threadsafe for general usage.

Then throughout your code, add one-liners to report metrics to your AppFirst 
application or dashboard.  

Examples:
 
	$statsd.increment 'foo'  						# increment counter foo.
	$statsd.decrement 'manchu'						# decrement counter manchu.
	$statsd.gauge 'buffers_left' buffer_pool.count	# report a value as a guage
	$statsd.timing 'cart_delay' ((cart_start - Time.now) * 1000).round  
													# report a time
    $statsd.time(cart_processing)  					# will report the execution  
													# 			time of	a block

