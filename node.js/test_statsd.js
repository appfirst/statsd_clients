var PosixMQ = require('pmq');
var readbuf, mq;

mq = new PosixMQ();
mq.on('messages', function() {
  var n;
  while ((n = this.shift(readbuf)) !== false) {
    console.log('%s - Messages (%s bytes): %s', new Date(), n, readbuf.toString('utf8', 0, n));
    console.log('%s - Messages left: %s', new Date(), this.curmsgs);
    console.log('---------------------------------------');
  }
  // this.unlink();
  //this.close();
});
mq.open({ name: '/afcollectorapi'});
readbuf = new Buffer(mq.msgsize);
