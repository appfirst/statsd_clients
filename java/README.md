![Appfirst](http://www.appfirst.com/img/appfirst-logo.png)
Statsd Java Clients by [AppFirst](http://www.appfirst.com)
====================================

For Statsd, here's a definition I like from [Native StatsD integration with Gauges and Percentiles](http://blog.librato.com/2012/05/new-statsd-integration-with-gauges-and.html) by Mike Heffner from Librato.

> StatsD is a simple network daemon that continuously receives metrics pushed over UDP and periodically sends aggregate metrics to upstream services like Graphite and Librato Metrics. Because it uses UDP, clients (for example, web applications) can ship metrics to it very fast with little to no overhead. This means that a user can capture multiple metrics for every request to a web application, even at a rate of thousands of requests per second. Request-level metrics are aggregated over a flush interval (default 10 seconds) and pushed to an upstream metrics service.

There are currently 3 types of stats: [Counting](#couting), [Timing](#timing) and [Gauge](#gauge). Etsy has defined the [concepts](https://github.com/etsy/statsd#concepts). 

For API specification please see the documentation under `src/main/javadoc/` or [doc on github](
http://appfirst.github.com/java_statsd_client/src/main/javadoc/index.html?overview-summary.html)

Include `target/jar/statsd.jar`

## Use Statsd Client
-------------------

To use Statsd client, simply:

	StatsdClient client = new AFClient();
	client.increment("some_bucket");
	
By doing this, you sent a message that will look like this:

	some_bucket:1|c
	
That's it! Use that message anywhere in your code to get statistical data over time. The server will do everything for you. Currently there are several implementations available, such as the original [etsy/statsd](https://github.com/etsy/statsd) implementation and [AppFirst Collector][collector]'s seamless integration. Here is [a list of the different implementations](http://joemiller.me/2011/09/21/list-of-statsd-server-implementations/).


###AppFirst Extension:

There are currently two implementations of Statsd client, [UDPClient and AFClient](#about-transport) to send messages. To send through UDP, use UDPClient. To send through with AppFirst Collector (please [install the collector][collector] before you do this), use AFClient. Here is a complete example:

	import com.appfirst.statsd.StatsdClient;	
	import com.appfirst.statsd.AFClient;
	
	public class SomeClass{

		public static void main(){

			StatsdClient client = new AFClient();
			client.increment("bucket");
		}
	}

We also accept messages as extensions. On the graph, the message will be displayed for each tick.
<!--We need a example pic to demostrate here.-->
The format will be position-invariant:

	bucket: field0 | field1 | field2                 | field3
	bucket: value  | unit   | sampele_rate/timestamp | message

e.g. a counter message and a gauge message:

	bucket:2|c|0.1|some_message
	bucket:333|g|1344570108|some_message

If a message is there, we will keep field2 (even if it's blank):

	bucket:10|ms||some_message

Please see below for detail usage.
	
## Counting
-------------------
**increment(buckets, sample_rate=1, message=None)**
	
To increment one or more stats counters, message is available with **[AppFirst Extended Format](#appfirst-extension)** only.
	    	
Here's an example (the second example are using [sample rate](#sample-rate)):

	StatsdClient client = new AFClient();
	client.increment("some.int", "some.int2");
	
'c' is the unit for counter. This will send messages:

	some.int:1|c
	some.int2:1|c

**decrement(buckets, sample_rate=1, message=None)**

To decrement one or more stats counters.

	StatsdClient client = new AFClient();
	client.decrement("some.int");
	
And this will send message:

	some.int:-1|c

**update_stats(buckets, delta=1, sampleRate=1, message=None)**

To updates one or more stats counters by arbitrary amounts:

	StatsdClient client = new AFClient();
	client.updateStats(5, "some.int");
	client.updateStats(2, 0.5, "some.int");
	client.updateStats(3, "act1", 1.0, "some.int")
	
And the messages for these are:

	some.int:5|c
	some.int:2|c|@0.5
	some.int:3|@1.0|act1

###Sample Rate

Chances are that your application sends lots of counting through UDP/AFTransport which might significantly hurt your performance. Sampling can resolve that.

By pass in a `sampleRate`, the client will only send `(sampleRate * 100)%` of the **counter** messages by **random** and discard the rest "unlucky" ones. The message will carry this rate, and the server will restore the count by multiply `1/sampleRate` upon reception (of course, we lose the precision).

By default the sample_rate is alway 1, which means every message will be sent.

Note this is a **counter** only feature, `sampleRate` for **timer** and **gauge** will be ignored.


## Timing
-------------------
**timing(bucket, elapse, message=None)**

Log timing information is the measuring the elapsed time for a certain action.
Optionally, you can also define message (with **[AppFirst Extended Format](#appfirst-extension)** only).

	StatsdClient client = new AFClient();
	client.timing("some.time", 500);
	client.timing("some.time", 300, "message");

UOM for timing is always 'ms' (milli-second). A message like this is sent:

	some.time:500|ms
	some.time:300|ms||message

## Gauge
-------------------
**gauge(bucket, reading, message=None)**

Log gauge information is the status of the moment. The client will send the message with a current timestamp.

	StatsdClient client = new AFClient();
	client.gauge("some.gauge", 500);

The unit for gauge is 'g', here is the message:

	some.gauge:500|g

Again, you can also define message (with **[AppFirst Extended Format](#appfirst-extension)** only)
	

## UDPClient and AFClient
-------------------
In order to send StatsD message in different ways, you can have different implementation to `StatsdClient` interface. Currently we have `UDPClient` and `AFClient`. If you want to communicate with [AppFirst Collector][collector], `AFClient` will send statsd message via a posix message queue to the collector. If the message transfer fails, it will sent to the confiured statsd daemon, via the normal statsd message transport method, which is UDP. Please send email to <support@appfirst.com> if you have any questions about this.

To write your own, just extended the skeleton class AbstractStatsdClient and implement the method `doSend(String)`

## Namespace
-------------------
TBD


[collector]: https://wwws.appfirst.com
