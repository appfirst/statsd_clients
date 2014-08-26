![Appfirst](https://wwws.appfirst.com/site_media/images/af_logo_blue.svg)

Java StatsD client for the [AppFirst](http://www.appfirst.com) collector
====================================

This Java StatsD client includes several AppFirst extensions:

- Metrics are aggregated over 20-second periods before being transmitted to an endpoint in order to minimize
  overhead and size of data in uploads.
- Data is sent to the local collector via Mailslot or POSIX message queue instead of over UDP.

By default, both of those features are enabled, but the client can be configured
to use UDP (like the Etsy-standard) instead of the message queue.

Installation
------------

This client can be compiled with [Maven](https://maven.apache.org/) by running `mvn package`. Compiled
jars will be put in the `target/` directory.


Configuration
-------------

Create a new AFService instance:

```java
import com.appfirst.statsd.StatsdService;
import com.appfirst.statsd.AFService;

StatsdService stats = new AFService();
```

There is no additional configuration necessary to use the StatsD client with default
configuration. By default, the client will use the message queue transport and aggregation
will be enabled.

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
StatsdService stats = new AFService();
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
StatsdService stats = new AFService();
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
StatsdService stats = new AFService();
// Report that an action took 237 milliseconds
stats.timing("ecommerce.checkout", 237);
```
