/* Tracks state of current metrics and sends them up to the Collector ever 20 secs */

var PosixMQ = require('pmq');
var mq = new PosixMQ();
mq.open({name: '/afcollectorapi'});

var agg = {};

agg.Aggregator = function() {
    this.buffer = {};

    this.handle = function(bucket) {
        /* Aggregate values till a flush is called */
        if (Object.keys(this.buffer).indexOf(bucket.name) >= 0) {
            this.buffer[bucket.name].aggregate(bucket.value);
        } else {
            this.buffer[bucket.name] = bucket;
        }
    };

    this.flush = function(bucket) {
        /* Send data to collector and reset buffer */
        for (var bucket_name in this.buffer) {
            var bucket = this.buffer[bucket_name];
            mq.push(bucket.getOutputString());
        }
        this.buffer = {};
    };
};

module.exports = agg;
