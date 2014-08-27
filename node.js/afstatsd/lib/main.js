/*
 * APPFIRST STATSD CLIENT
 * for Node.js
 *
 * 2014-08-26
 * author: Mike Okner
 * email: michael@appfirst.com
 *
 * Currently only supports POSIX message queues. The module `node-statsd` should
 * be used UDP transport is desired.
 *
 * usage:
 * Statsd.increment('counter.name')
 * Statsd.timer('timer.name', miliseconds)
 * Statsd.gauge('name.of.gauge', 37)
 *
 */

var process = require('process');
var buckets = require('./buckets');
var Aggregator = require('./stats_aggregator').Aggregator;

var aggregator = new Aggregator();
var Statsd = {};  // API object


Statsd.updateStats = function(name, value) {
    /* Adds `value` to the current value of a metric w/ name `name` */
    aggregator.handle(new buckets.CounterBucket(name, value));
};

Statsd.increment = function(name) {
    /* Increments a counter with name `name` by value. If value is not specified,
     * `name` is incremented by 1.
     */
    return updateStats(name, 1);
};

Statsd.decrement = function(name) {
    /* Decrements a counter with name `name` by value. If value is not specified,
     * `name` is decremented by 1.
     */
    return updateStats(name, -1);
};

Statsd.gauge = function(name, value) {
    /* Sets a gauge with name `name` to `value`. */
    aggregator.handle(new buckets.GaugeBucket(name, value));
};

Statsd.timing = function(name, value) {
    /* Sets a timer with name `name` to the time in miliseconds contained in `value` */
    aggregator.handle(new buckets.TimerBucket(name, value));
};

function cleanup_handler() {
    /* Do any cleanup and exit */
    aggregator.flush();

    process.exit();
}

setInterval(aggregator.flush, 20000);  // Every 20 seconds, flush data to collector

/* Register cleanup exit events */
process.on('SIGINT', cleanup_handler);
process.on('SIGTERM', cleanup_handler);
process.on('exit', cleanup_handler);

module.exports = Statsd;
