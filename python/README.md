![Appfirst](http://www.appfirst.com/img/appfirst-logo.png)
Statsd Python Clients by [AppFirst](http://www.appfirst.com)
====================================

For Statsd, here's a definition I like from [Native StatsD integration with Gauges and Percentiles](http://blog.librato.com/2012/05/new-statsd-integration-with-gauges-and.html) by Mike Heffner from Librato.

> StatsD is a simple network daemon that continuously receives metrics pushed over UDP and periodically sends aggregate metrics to upstream services like Graphite and Librato Metrics. Because it uses UDP, clients (for example, web applications) can ship metrics to it very fast with little to no overhead. This means that a user can capture multiple metrics for every request to a web application, even at a rate of thousands of requests per second. Request-level metrics are aggregated over a flush interval (default 10 seconds) and pushed to an upstream metrics service.

There are currently 3 types of stats: [Counting](#couting), [Timing](#timing) and [Gauge](#gauge).) Etsy has defined the [concepts](https://github.com/etsy/statsd#concepts). 

Use Statsd Client
------------------

To use Statsd client, simply:

	from client import Statsd
	Statsd.increment("some.counter.bucket")
	
By doing this, you sent a message that will look like this:

	some_bucket:1|c
	
That's it! Use it anywhere in your code to get statistical data over time. The server will do everything for you. Currently there are several implementations out there, such as the original [etsy/statsd](https://github.com/etsy/statsd) implementation and [AppFirst Collector][collector]'s seamless integration. Here is [a list of them](http://joemiller.me/2011/09/21/list-of-statsd-server-implementations/).

Now we have an even simpler way if you just want to **count** the invocation or **time** the life cycle of a **function**.

	from client import Statsd

	@Statsd.time("some.counter.bucket")
	@Statsd.time("some.timer.bucket")
	def some_func():
	    pass #do something

Please see detail in [Counting](#counting) or [Timing](#timing) section.

There are, in total, three modules for python client. The main functionality is all included in `client.py`.  
To use statsd client with UDP, you also have to setup `local_settings.py` if your server is not on localhost or is not using default port.  
To use it with AppFirst Collector, you will need the `AFTransport` from `afclient.py`. Please see below.

AppFirst Extension:
-------------------

By default, Statsd client in client.py uses UDP [Transport](#about-transport) to send messages. To use with AppFirst Collector (please [install the collector][collector] before you do), switch to the AppFirst [Transport](#about-transport) before usage **FOR ONE TIME ONLY** or simply import it from afclient.py (Statsd client use AFTransport by default in this module):
	
	from client import Statsd
	from afclient import AFTransport
	
	Statsd.set_transport(AFTransport())

	@Statsd.time("some.timer.bucket")
	def some_func():
	    pass #do something

We also accept message as extension. On the graph, the message will be displayed for each tick.
<!--We need a example pic to demostrate here.-->
The format will be position-invariant:

	bucket: field0 | field1 | field2                 | field3
	bucket: value  | unit   | sampele_rate/timestamp | message

e.g. a counter message and a gauge message look like this:

	bucket:2|c|0.1|some_message
	bucket:333|g|1344570108|some_message

When a message is there, we always keep field2 even if it's blank:

	bucket:10|ms||some_message

Please see below for detail usage.
	
Counting
--------
**increment(buckets, sample_rate=1, message=None)**
	
To increment one or more stats counters, message is available with **[AppFirst Extended Format](#appfirst-extension)** only.
	    	
Here's an example (the second example are using [sample rate](#sample-rate)):

	from client import Statsd
    Statsd.increment('some.int')
	Statsd.increment('some.int',0.5)
	
'c' is the unit for counter. This will send messages:

	some.int:1|c
	some.int:1|c|@0.5

**decrement(buckets, sample_rate=1, message=None)**

To decrement one or more stats counters.

	from client import Statsd
	Statsd.decrement('some.int', 'by process 1')
	
And this will send message:

	some.int:-1|c||by process 1

**update_stats(buckets, delta=1, sampleRate=1, message=None)**

To update one or more stats counters by arbitrary amounts:

	from client import Statsd
	Statsd.update_stats('some.int',10)
	
That message is:

	some.int:10|c

Sample Rate
-----------

Chances are that your application sends lots of counting through UDP/AFTransport, which might significantly hurt your performance. Sampling can resolve that.

By pass in a `sample_rate`, the client will only sent `(sample_rate * 100)%` of the **counter** messages by **random** and discard the rest "unlucky" ones. The message will carry this rate, and the server will restore the count by multiply `1/sample_rate` upon reception (of course, we lose the precision).

By default the sample_rate is always 1, which means every message will be sent.

Note: this is a **counter** only feature, `sample_rate` for **timer** and **gauge** will be ignored.

Convenient Count Wrapper
------------------------

We've added a convenient wrapper method in client.Statsd.

	@Statsd.count("counter1")
	def some_func():
	    pass #do something

This will count how many times this wrapped function is invoked, and sent to the statsd server.

You can count with more than one bucket, with sample_rate:

	@Statsd.count(["counter1", "counter2"], sample_rate=0.5)

Timing
------
**timing(bucket, elapse, message=None)**

Log timing information is measuring the elapsed time for a certain action.

	from client import Statsd
    Statsd.timing('some.time', 500)

UOM for timing is always 'ms' (milli-second). A message like this is sent:

	some.time:500|ms

Optionally, you can also define message (with **[AppFirst Extended Format](#appfirst-extension)** only).
    
Convenient Time Wrapper
-----------------------

We've added a convenient wrapper method in client.Statsd.

	@Statsd.time("some.timer.bucket")
	def some_func():
	    pass #do something

This will calculate the time consumed by the wrapped function, and send it to the statsd server.


Gauge
-----
**gauge(bucket, reading, message=None)**

Log gauge information is the status of the moment. The client will send the message with a current timestamp.

	from client import Statsd
	Statsd.gauge('some.gauge', 500)
	
The unit for gauge is 'g'. The message appears as:

	some.gauge:500|g

Again, you can also define message (with **[AppFirst Extended Format](#appfirst-extension)** only)
	

About Transport
---------------
In order to switch between different ways of sending statsd message, we have created Transport. Currently we have `UDPTransport` by default in client.py.  `AFTransport` by default in afclient if you want to communicate with [AppFirst Collector][collector] is also available.

To switch transport, create an instance of that transport and pass in Statsd module using `set_transport`. This should be done only once (as you don't have to pass in a new transport every time you send message). Once you set a different transport, the former one will **close()**. Whenever the process is exited, the transport will also be closed.

	from client import Statsd
	from afclient import AFTransport

    transport = AFTransport()
	Statsd.set_transport(transport)
	
	@Statsd.count("bucket.name")
	def some_func()
	    #do something

This is equivalent to:

    from afclient import Statsd

    @Statsd.count("bucket.name")
    def some_func()
        #do something


To implement your own transport, there are two methods you need to implement:

**emit(self, data)**

To emit the data through this transport.

**close(self)**

To close the connection and dispose of the resources (aka clean things up before you quit).

Namespace
----------
TBD


[collector]: https://wwws.appfirst.com
