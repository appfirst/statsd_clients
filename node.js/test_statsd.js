var PosixMQ = require('pmq');
var readbuf, mq;

mq = new PosixMQ();
mq.on('messages', function() {
  var n;
  while ((n = this.shift(readbuf)) !== false) {
    console.log('Messages (%s bytes): %s', n, readbuf.toString('utf8', 0, n));
    console.log('Messages left: %s', this.curmsgs);
    console.log('---------------------------------------');
  }
  // this.unlink();
  //this.close();
});
mq.open({ name: '/afcollectorapi'});
readbuf = new Buffer(mq.msgsize);