![Appfirst](https://wwws.appfirst.com/site_media/images/af_logo_blue.svg)

Node.js StatsD client for the [AppFirst](http://www.appfirst.com) collector
====================================
This Node.js StatsD client includes several AppFirst extensions:

- Metrics are aggregated over 20-second periods before being transmitted to an endpoint in order to minimize overhead and size of data in uploads.
- Data is sent to the local collector via AFTransport (POSIX message queue) instead of over UDP.

If you are looking for an Etsy-standard UDP client, we recommend [this client](https://github.com/sivy/node-statsd).

Installation
------------
Using npm:

    $ npm install afstatsd

**OR** manually download and place the `afstatsd` directory in your `node_modules` path.


Setup
-----

Pull in the AppFirst StatsD Client (probably in your main app):

```javascript
var Statsd = require('afstatsd');
```

Usage
-----
The simplest `Statsd` method is `increment`. It simply keeps a running tally of
how many times each counter name gets incremented during each time period. To
keep track of a value like number of concurrent connections and update it periodically, report
that using `gauge`, since a gauge won't be reset after each reporting interval.
To report how long something took to execute, use the `timing` method.

The StatsD variable name can be any string. It is most common to use
dot-notation namespaces like `shopping.checkout.payment_time` or `website.pageviews`.

**Counters:**

```javascript
// increment the counter 'af.example.counter'
Statsd.increment('af.example.counter');
// Decrement the counter
Statsd.decrement('af.example.counter');
// Add an arbitrary value
Statsd.updateStats('af.example.counter', 17);
```


**Gauges:**

```javascript
// set the value of the gauge to 100
Statsd.gauge('af.example.gauge', 100);
// Update the value
Statsd.gauge('af.example.gauge', 50);
```

**Timers:** *(Time should be reported in milliseconds)*

```javascript
// report that an action took 237 milliseconds
Statsd.timing('ecommerce.checkout', 237);
```
