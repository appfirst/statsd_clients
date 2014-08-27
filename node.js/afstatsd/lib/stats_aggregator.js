/* Tracks state of current metrics and sends them up to the Collector ever 20 secs */

var PosixMQ = require('pmq');
var mq = new PosixMQ();
mq.open({name: '/afcollectorapi', flags: 2049});  // O_WRONLY | O_NONBLOCK: 2049

module.exports.Aggregator = function() {
    this.buffer = {};
    this.max_len = 2048;

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
        var master_out = "";

        // Loop over buckets and build output string
        for (var bucket_name in this.buffer) {
            var bucket = this.buffer[bucket_name];
            var bucket_out = bucket.getOutputString();
            if (master_out === "") {
                master_out = bucket_out;
            }
            else if ((master_out + '::' + bucket_out).length < this.max_len) {
                // Concatenate multiple messages so we are less likely to bump
                // into the 200 message limit
                master_out = master_out + '::' + bucket_out;
            } else {
                // We've hit the max. Publish and start over
                mq.push(master_out, 3);
                master_out = bucket_out;
            }
        }

        // Publish the final round of data currently in `master_out`
        if (master_out !== '') {
            mq.push(master_out, 3);
        }

        // Reset the buffer
        this.buffer = {};
    };
};
