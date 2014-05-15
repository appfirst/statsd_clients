![Appfirst](http://www.appfirst.com/static/images/appfirst-logo.svg)

A Python StatsD client for use with the [AppFirst](http://www.appfirst.com) collector
====================================
This Python StatsD client includes several AppFirst extensions:

- Metrics are aggregated over 20-second periods before being transmitted to an endpoint in order to minimize overhead and size of data in uploads.
- Data is sent to the local collector via a AFTransport (POSIX message queue) instead of over UDP.

By default, both of those features are enabled, but the aggregation can be disabled and the client can be configured
to use UDP (like the Etsy-standard) instead of the message queue.

Installation:
------------
This AppFirst Python client supports Python 2.6+ including Python 3.

Using pip:

    $ pip install afstatsd

**OR** manually download and place the following files in the same directory as your project.

    client.py
    afclient.py
    local_settings.py

Configuration:
-------------

Pull in the AppFirst StatsD Client (probably in your main module):

```python
from afstatsd import Statsd
```

There is no additional configuration necessary to use the StatsD client with default
configuration. By default, the client will use the message queue transport and aggregation
will be enabled.

## 

If error logging is desired, a logger can be passed when creating a new AFTransport object.
The logger can be any object that has the standard `.debug()`, `.info()`, `.warning()`, `.error()`
methods.

```python
from afstatsd import Statsd, AFTransport
Statsd.set_transport(AFTransport(logger=logger_object))
```

## 

The aggregation feature will collect statistics over a period of time, and aggregate
them to reduce the amount of data that needs to be transferred. For example, if an
individual counter is incremented 1000 times during the aggregation interval, only one
message will be sent with a count of 1000, rather than 1000 messages with count of 1.
The tradeoff is that latency is introduced in reporting the metrics.

Aggregation is turned on by default, and we highly recommend it, but it can be disabled
by setting the interval to 0, or by setting the aggregating attribute to false:

```python
Statsd.set_aggregation(False)
```

## 

The message queue is configured by the AppFirst collector. At the current
time, it is set accept about 200 messages per second. If overrun,
messages will be dropped. This is another reason to use the aggregation
capability.

This StatsD client can also send data the "Etsy standard" upload method using a UDP socket.
This can be useful if transitioning from some other StatsD implementation to the AppFirst
implementation:

```python
from afstatsd.client import UDPTransport
Statsd.set_transport(UDPTransport())
```

The IP address and UDP port number are configured in `local_settings.py`. Once these
configuration attributes are set, the client is thread safe for general usage.
Please be aware that the client will create a python thread to run the aggregation
function. As you know, you should not call os.fork() if python threads are running,
so if you use multiprocessing, import the statsd library *after* you fork.

Usage:
-----
The simplest `Statsd` method is `increment`. It simply keeps a running tally of
how many times each counter name gets incremented during each time period. To
keep track of a value like number of threads and update it periodically, report
that using `gauge`, since a gauge won't be reset after each reporting interval.
To report how long something took to execute, use the `timing` method.

The StatsD variable name can be any string. It is most common to use
dot-notation namespaces like `shopping.checkout.payment_time` or `website.pageviews`.

**Counters:**

```python
# increment the counter 'af.example.counter'
Statsd.increment('af.example.counter')
# Decrement the counter
Statsd.decrement('af.example.counter')
```

**Gauges:**

```python
# set the value of the gauge to 100
Statsd.gauge('af.example.gauge', 100)
# Update the value
Statsd.gauge('af.example.gauge', 50)
```

**Timers:** *(Time should be reported in milliseconds)*

```python
# report that an action took 237 milliseconds
Statsd.timing('ecommerce.checkout', 237)
```
