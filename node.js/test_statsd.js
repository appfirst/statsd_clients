var PosixMQ = require('pmq');
var readbuf,
    mq = new PosixMQ();

mq.on('messages', function() {
  var n;
  while ((n = this.shift(readbuf)) !== false) {
    console.log('%s - Messages (%s bytes): %s', new Date(), n, readbuf.toString('utf8', 0, n));
    console.log('%s - Messages left: %s', new Date(), this.curmsgs);
    console.log('---------------------------------------');
  }
});

process.on('exit', function () {
  console.log("closing mq");
  mq.close();
});

mq.open({ name: '/afcollectorapi'});
readbuf = new Buffer(mq.msgsize);
