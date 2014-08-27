/* Bucket type definitions */

var buckets = {};

buckets.CounterBucket = function(name, value) {
    this.name = name;
    this.value = value;

    this.aggregate = function(value) {
        this.value += value;
    };

    this.getOutputString = function() {
        return this.name + ":" + this.value + "|c";
    };
};

buckets.GaugeBucket = function(name, value) {
    this.name = name;
    this.value = value;
    this.timestamp = null;

    this.aggregate = function(value) {
        this.value = value;
        this.timestamp = (new Date().getTime() / 1000).toFixed(0);
    };

    this.getOutputString = function() {
        return this.name + ":" + this.value + "|g|" + this.timestamp;
    };
};

buckets.TimerBucket = function(name, value) {
    this.name = name;
    this.values = value;

    this.aggregate = function(value) {
        this.values.push(value);
    };

    this.getOutputString = function() {
        return this.name + ":" + ','.join(this.values) + "|ms";
    };
};

module.exports = buckets;
