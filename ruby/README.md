![Appfirst](http://www.appfirst.com/img/appfirst-logo.png)

A statsd Ruby client for use with the [AppFirst](http://www.appfirst.com) collector.  
===============================================================================
[Statsd is a scheme developed by Etsy](https://github.com/etsy/statsd) that provides 
a lightweight method of gathering statistics from your applications.  The Appfirst 
version is based on the [statsd Ruby client by reinh](https://github.com/reinh/statsd), 
but does not inlcude the batch capability of the original.  Instead, it 
includes an aggregation capability to reduce the amount of data that must be transported.  

Installation:
-------------
Data is transported to the AppFirst collector via a POSIX Message Queue.  The Posix 
Message Queue is not part of the standard ruby library, and must be installed prior 
to using this statsd client.  This should do the job:

    >gem install posix_mq

Now just copy the .rb files you find here to your working directory.  Test.rb is just 
an example you can crib from - you don't need to keep it.

Configuration:
--------------
Pull in the AppFirst Statsd Client (probably in your main module):

    require './afstatd.rb'

At startup, instantiate a Statsd client object:

    $statsd = Statsd.new 
    
or 
                
    $statsd = Statsd.new 'localhost', 8125, 20
	
This will create a new client, with optional UDP address and port numbers for the 
fallback case.  The client object in this example is a global for convenience, 
but this is not a requirement.  The third parameter is the timing interval for 
metric aggregation, and defaults to 20 sec.  

The aggregation feature will collect statistics over a period of time, and aggregate 
them to reduce the amount of data that needs to be transferred.  For example, if an
individual counter is incremented 1000 times during the aggregation interval, only one 
message will be sent with a count of 1000, rather than 1000 messages with count of 1.
The tradeoff is that latency is introduced in reporting the metrics.

Aggregation is turned on by deafult, and we highly recommend it, but it can be disabled 
by setting the interval to 0, or by setting the aggregating attribute to false:

	$statsd.aggregating = false.
	
The message queue is configured by the AppFirst collector.  At the current 
time, it is set accept about 200 messages per second.  If overrun, 
messages will be dropped.  This is another reason to use the aggregation 
capability.  Dropped messages are counted and can be accessed with the 
`$statsd.dropped` attribute.

The namespace attribute, if specified, will be prepended to all metrics.  

	$statsd.namespace = 'system.component'

This statsd client can also send data the 'Etsy standard' way using a UDP socket.  This 
can be useful if transistioning from some other statsd implementation to the AppFirst 
implementation:

    $statsd.set_transport(:udp_transport)

If the message queue is not found, the client will revert to using a UDP 
socket as a backup transport method.  The only reason this should happen is if 
the AppFirst collector is not installed.
    
Once these configuration attributes are set, the client is threadsafe for general usage.

Usage:
------
Now comes the fun part.  Just sprinkle these one-liners throughout your 
application code to report metrics to your AppFirst application or dashboard:

Examples:
--------- 
	$statsd.increment 'foo'  						        # increment counter foo.
	$statsd.decrement 'manchu'						        # decrement counter manchu.
	$statsd.gauge 'buffers_left' buffer_pool.count	        # report a value as a guage
    $statsd.time 'cart_process_time' {cart_processing}      # will report the execution time of	a block

