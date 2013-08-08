![Appfirst](http://www.appfirst.com/img/appfirst-logo.png)

A Statsd Python Client for use with the [AppFirst](http://www.appfirst.com) collector
====================================
[Statsd is a scheme developed by Etsy](https://github.com/etsy/statsd) that provides
a lightweight method of gathering statistics from your applications. The Appfirst
version includes an aggregation capability to reduce the amount of data that must be transported,
and data is sent to the AppFirst collector that is running on the same server
as your application, using a posix message queue. By default, both of those features are enabled,
but the aggregation can be disabled, and the client can be configured to use UDP (like the Etsy-standard)
instead of the message queue.

Installation:
------------
Using pip:

    $ pip install afstatsd

**OR** manually download and place the following files in the same directory as your project.

    client.py
    afclient.py
    local_settings.py

Configuration:
-------------

Pull in the AppFirst Statsd Client (probably in your main module):

```python
from afstatd import Statsd
```

This will instantiate a default Statsd client with default configuration. By default,
the client will use the message queue transport and aggregation will be enabled.

The aggregation feature will collect statistics over a period of time, and aggregate
them to reduce the amount of data that needs to be transferred. For example, if an
individual counter is incremented 1000 times during the aggregation interval, only one
message will be sent with a count of 1000, rather than 1000 messages with count of 1.
The tradeoff is that latency is introduced in reporting the metrics.

Aggregation is turned on by deafult, and we highly recommend it, but it can be disabled
by setting the interval to 0, or by setting the aggregating attribute to false:

```python
Statsd.set_aggregation(False)
```

The message queue is configured by the AppFirst collector. At the current
time, it is set accept about 200 messages per second. If overrun,
messages will be dropped. This is another reason to use the aggregation
capability.

This statsd client can also send data the 'Etsy standard' way using a UDP socket. This
can be useful if transistioning from some other statsd implementation to the AppFirst
implementation:

```python
from afstatsd.client import UDPTransport
Statsd.set_transport(UDPTransport())
```

The IP address and udp port number are configured in local_settings.py. Once these
configuration attributes are set, the client is thread safe for general usage. Please be aware that the client will create a python thread to run the aggregation function. As you know, you should not call os.fork() if python threads are running, so if you use multiprocessing, import the statsd library *after* you fork.

Usage:
-----
The simplest StatsD metric is `increment`. It simply keeps a running tally of how many times each counter name gets incremented during each time period. To keep track of a value like number of threads and update it periodically, report that using a `gauge`, since a gauge won't be reset after each reporting interval. To report how long something took to execute, use the `timing` metric.

The StatsD variable name can be any string. It is most common to use dot-notation namespaces like `shopping.checkout.payment_time` or `website.pageviews`.

**Counters:**

```python
# increment the counter 'af.example.counter'
Statsd.increment('af.example.counter')
# Annotate the increment with an associated message
Statsd.increment('af.example.login', message=username)
# Decrement the counter
Statsd.decrement('af.example.counter', message='Message text')
```

**Gauges:**

```python
# set the value of the gauge to 100
Statsd.gauge('af.example.gauge', 100)
# Update the value and include a message
Statsd.gauge('af.example.gauge', 50, message='Process Exited')
```

**Timers:** *(Time should be reported in milliseconds)*

```python
# Report that an action took 500 milliseconds
Statsd.timing('example_timer', 500)
# report that a checkout took 237 milliseconds and annotate the username
Statsd.timing('ecommerce.checkout', 237, message=username)
```
