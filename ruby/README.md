![Appfirst](https://wwws.appfirst.com/site_media/images/af_logo_blue.svg)

A statsd Ruby client for use with the [AppFirst](http://www.appfirst.com) collector.  
===============================================================================
[Statsd is a scheme developed by Etsy](https://github.com/etsy/statsd) that provides 
a lightweight method of gathering statistics from your applications.  The Appfirst 
version is based on the [statsd Ruby client by reinh](https://github.com/reinh/statsd), 
but does not include the batch capability of the original.  Instead, it 
includes an aggregation capability to reduce the amount of data that must be transported.

In the default case, data is sent to the AppFirst collector that is running on the same server
as your application.  The data is sent over a POSIX message queue.  If the collector is not found,
the client will fall back to the UDP transport method.

Installation:
-------------
The AppFirst Statsd client is packaged as a ruby gem.  Installation should be as simple as:

    $ gem install afstatsd

This should automatically install another gem, `posix_mq`.  If the `posix_mq` fails during installation, 
it is probably because your ruby installation does not include the development package (eg `ruby1.9-devel`).

Configuration:
--------------
Pull in the AppFirst Statsd Client (probably in your main module):

```ruby
require 'afstatsd'
```

At startup, instantiate a Statsd client object. To use the default configuration which aggregates metrics for 20 seconds and then sends data to the AppFirst collector, you should instantiate the client object like so:

```ruby
$statsd = Statsd.new
``` 
    
To add a UDP fall-back, include the host and port of the fall-back server:

```ruby                
$statsd = Statsd.new 'localhost', 8125
```

Or, to only use UDP, set the aggregation period to 0 and specify the `'udp'` parameter like so:

```ruby
$statsd = Statsd.new 'localhost', 8125, 0, 'udp'
```

The client object in this example is a global for convenience, but this is not a requirement.
The third parameter is the timing interval for metric aggregation, and defaults to 20 sec.

The aggregation feature will collect statistics over a period of time, and aggregate 
them to reduce the amount of data that needs to be transferred.  For example, if an
individual counter is incremented 1000 times during the aggregation interval, only one 
message will be sent with a count of 1000, rather than 1000 messages with count of 1.
The tradeoff is that latency is introduced in reporting the metrics.

Aggregation is turned on by default, and we highly recommend it, but it can be disabled 
by setting the interval to 0, or by setting the aggregating attribute to false:

```ruby
$statsd.aggregating = false
```

The message queue is configured by the AppFirst collector.  At the current 
time, it is set accept about 200 messages per second.  If overrun, 
messages will be dropped.  This is another reason to use the aggregation 
capability.  Dropped messages are counted and can be accessed with the 
`$statsd.dropped` attribute.

The namespace attribute, if specified, will be prepended to all metrics, for example:  

```ruby
$statsd.namespace = 'system.component'
```

or perhaps:

```ruby
$statsd.namespace = 'system.#{Socket.gethostname}.component'
```

This Statsd client can also send data the 'Etsy standard' way using a UDP socket.  This 
can be useful if transitioning from some other StatsD implementation to the AppFirst 
implementation:

```ruby
$statsd.set_transport(:udp_transport)
```

If the message queue is not found, the client will revert to using a UDP 
socket as a backup transport method.  The only reason this should happen is if 
the AppFirst collector is not installed.
    
Once these configuration attributes are set, the client is thread-safe for general usage.

Usage:
------
Now comes the fun part.  Just sprinkle these one-liners throughout your 
application code to report metrics to your AppFirst application or dashboard:

**Counters**
```ruby
def increment(name, sample_rate=1)
def decrement(stat, sample_rate=1)
def count(stat, count, sample_rate=1)
```

**Timers**
```ruby
def timing(stat, ms, sample_rate=1)
def time(stat, sample_rate=1)
```

**Gauges**
```ruby
def gauge(stat, value)
```

Examples:
---------
```ruby
$statsd.increment 'foo'  						     # increment event counter 'foo'.
$statsd.decrement 'manchu'						     # decrement event counter 'manchu'.
$statsd.count 'mustache', 10 					     # report that event 'mustache' ocurred 10 times.
$statsd.gauge 'buffers_left', buffer_pool.count      # report a value as a guage
$statsd.time 'cart_process_time' {cart_processing}   # will report the execution time of a block
```

The easiest Statsd method to use is *increment*.  You don't have to track anything yourself in your application.  Just fire off that one-liner 
for every event, you want to monitor, and the upstream apparatus will take care of everything for you.
    
If you want to keep track of the current state of a value, you should report that with a *gauge*. 
    
To report how long something took to execute, use the *time* metric.

**Logging:**

To attach a logger to the Statsd client:
```ruby
require 'logger'
#...
Statsd.logger ||= Logger.new 'afstatsd.log'
Statsd.logger.level = Logger::DEBUG
```

   Copyright (c) 2013 AppFirst

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0 "http://www.apache.org/licenses/LICENSE-2.0")

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
