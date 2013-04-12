![Appfirst](http://www.appfirst.com/img/appfirst-logo.png)

A Statsd Python Client for use with the [AppFirst](http://www.appfirst.com) collector
====================================
[Statsd is a scheme developed by Etsy](https://github.com/etsy/statsd) that provides 
a lightweight method of gathering statistics from your applications.  The Appfirst 
version includes an aggregation capability to reduce the amount of data that must be transported, 
and data is sent to the AppFirst collector that is running on the same server
as your application, using a posix message queue.  By default, both of those features are enabled, 
but the aggregation can be disabled, and the client can be configured to use UDP (like the Etsy-standard) 
instead of the message queue.

Installation:
------------
Place the following files in your project directory:

    client.py
    afclient.py
    local_settings.py    
    
Configuration:
-------------

Pull in the AppFirst Statsd Client (probably in your main module):

    from afclient import Statsd

This will instantiate a default Statsd client with default configuration.  By default, 
the client will use the message queue transport and aggregation will be enabled.
	
The aggregation feature will collect statistics over a period of time, and aggregate 
them to reduce the amount of data that needs to be transferred.  For example, if an
individual counter is incremented 1000 times during the aggregation interval, only one 
message will be sent with a count of 1000, rather than 1000 messages with count of 1.
The tradeoff is that latency is introduced in reporting the metrics.

Aggregation is turned on by deafult, and we highly recommend it, but it can be disabled 
by setting the interval to 0, or by setting the aggregating attribute to false:

	Statsd.set_aggregation(False)
	
The message queue is configured by the AppFirst collector.  At the current 
time, it is set accept about 200 messages per second.  If overrun, 
messages will be dropped.  This is another reason to use the aggregation 
capability.  

This statsd client can also send data the 'Etsy standard' way using a UDP socket.  This 
can be useful if transistioning from some other statsd implementation to the AppFirst 
implementation:

    Statsd.set_transport(UDPTransport())

The IP address and udp port number are configured in local_settings.py.  Once these 
configuration attributes are set, the client is threadsafe for general usage.

Usage:
-----
Now comes the fun part.  Just sprinkle these one-liners throughout your 
application code to report metrics to your AppFirst application or dashboard:

Examples:
--------- 
	Statsd.increment("foo")  				         # increment event counter 'foo'.
	Statsd.decrement("manchu")				         # decrement event counter 'manchu'.
	Statsd.update_stats("mustache", 10) 		     # report that event 'mustache' ocurred 10 times.
	Statsd.gauge("buffers_left", buffer_pool.count)  # report a value as a guage
    Statsd.timing("cart_process_time", process_time) # report an execution time 

The easiset statsd metric to use is *increment*.  You don't have to track anything yourself in your application.  Just fire off that one-liner 
for every event, you want to monitor, and the upstream apparatus will take care of everything for you.
    
If you are already keeping track of something, you should report that with a *gauge*. 
    
To report how long something took to execute, use the *timing* metric.