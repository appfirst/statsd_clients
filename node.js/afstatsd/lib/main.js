/*
 * APPFIRST STATSD CLIENT
 * for Node.js
 *
 * 2014-05-14
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

var PosixMQ = require('pmq');
var mq = new PosixMQ();
var Statsd = {};  // API object

mq.open({name: '/afcollectorapi'});


function increment(name, value) {
    /* Increments a counter with name `name` by value. If value is not specified,
     * `name` is incremented by 1.
     */
}

function decrement(name, value) {
    /* Decrements a counter with name `name` by value. If value is not specified,
     * `name` is decremented by 1.
     */
}

function gauge(name, value) {
    /* Sets a gauge with name `name` to `value`. */
}

function timer(name, value) {
    /* Sets a timer with name `name` to the time in miliseconds contained in `value` */
}
