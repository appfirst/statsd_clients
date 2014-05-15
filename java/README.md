![Appfirst](http://www.appfirst.com/static/images/appfirst-logo.svg)

Java StatsD client for the [AppFirst](http://www.appfirst.com) collector
====================================

This Java StatsD client includes several AppFirst extensions:

- Metrics are aggregated over 20-second periods before being transmitted to an endpoint in order to minimize overhead and size of data in uploads.
- Data is sent to the local collector via Mailslot or POSIX message queue instead of over UDP.

By default, both of those features are enabled, but the client can be configured
to use UDP (like the Etsy-standard) instead of the message queue.

Installation
------------

This client can be compiled with maven by running `mvn package`. Compiled files will be put in the `target/` directory.


Configuration
-------------

Create a new AFClient instance:

```java
import com.appfirst.statsd.StatsdClient;
import com.appfirst.statsd.AFClient;

StatsdClient stats = new AFClient();
```

There is no additional configuration necessary to use the StatsD client with default
configuration. By default, the client will use the message queue transport and aggregation
will be enabled.

## 

The message queue is configured by the AppFirst collector. At the current
time, it is set accept about 200 messages per second. If overrun,
messages will be dropped. The Sample Rate feature can be used to mitigate this issue (outlined further below).

This StatsD client can also send data the "Etsy standard" upload method using a UDP socket.
This can be useful if transitioning from some other StatsD implementation to the AppFirst
implementation:

```java
import com.appfirst.statsd.StatsdClient;
import com.appfirst.statsd.UDPClient;

public class SomeClass {

	public static void main() {
		StatsdClient stats = new UDPClient();
		stats.increment("bucket");
	}

}
```

Usage
-----
The simplest `Statsd` method is `increment`. It simply keeps a running tally of
how many times each counter name gets incremented during each time period. To
keep track of a value like number of threads and update it periodically, report
that using `gauge`, since a gauge won't be reset after each reporting interval.
To report how long something took to execute, use the `timing` method.

The StatsD variable name can be any string. It is most common to use
dot-notation namespaces like `shopping.checkout.payment_time` or `website.pageviews`.

**Counters:**

```java
boolean increment(String... buckets);
boolean decrement(String... buckets);
boolean updateStats(int value, String... buckets);
boolean updateStats(int value, double sampleRate, String... buckets);
```

Example:

```java
StatsdClient stats = new AFClient();
// increment several counters
stats.increment("af.example.counter1", "af.example.counter2");
// Decrement a counter
stats.decrement("af.example.counter2")
// Update several counters by arbitrary amounts
stats.updateStats(5, "af.example.counter1", "af.example.counter3", "af.example.counter4");
```

**Gauges:**

```java
boolean gauge(String bucket, int value);
```

Example:

```java
StatsdClient stats = new AFClient();
// Set the gauge value
stats.gauge("af.example.gauge", 500);
// Update the gauge value
stats.gauge("af.example.gauge", 624);
```

**Timers:** *(Time should be reported in milliseconds)*

```java
boolean timing(String bucket, int value);
```

Example:

```java
StatsdClient stats = new AFClient();
# report that an action took 237 milliseconds
stats.timing("ecommerce.checkout", 237);
```

Sample Rate
-----------

If your application records a significant number of statistics, the `AFClient` transport may fill up its
message queue and the `UDPClient` may introduce too much network traffic. Sampling can resolve that.

By passing a `sampleRate` to `updateStats`, the client will only send `(sampleRate * 100)%` of the **counter** messages at **random** and discard the rest. The message will carry this rate, and the server will restore the count by multiply `1/sampleRate` upon reception.

By default the sample_rate is alway 1, which means every message will be sent.

Note this is a **counter** only feature, `sampleRate` for **timer** and **gauge** will be ignored.
