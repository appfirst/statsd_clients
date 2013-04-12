#!/usr/local/bin/python
# -*- coding: utf-8 -*-
"""
The AppFirst Statsd Transport
"""
__all__=['AFTransport', 'Statsd', 'UDPTransport']

try:
    import ctypes
except Exception, e:
    ctypes = None

import errno, os, sys
from exceptions import Exception
from client import UDPTransport, Statsd, GeyserStategy
STATSD_SEVERITY = 3

LOGGER = None

def set_logger(logger):
    global LOGGER
    LOGGER = logger

class AFTransport(UDPTransport):
    def __init__(self, use_udp=True, verbosity=False, logger=None):
        set_logger(logger)
        self.mqueue_name = "/afcollectorapi"
        self.flags = 04001
        self.msgLen = 2048
        self.mqueue = None
        self.verbosity = verbosity
        self.shlib = self._loadlib()
        self.use_udp = use_udp

    def _loadlib(self):
        if ctypes:
            try:
                ctypes.cdll.LoadLibrary("librt.so.1")
                return ctypes.CDLL("librt.so.1", use_errno=True)
            except Exception:
                return None

    def _handleError(self, data, emsg=""):
        if LOGGER:
            LOGGER.error("Statsd Error: %s when sending %s" % (emsg, data))
        if self.mqueue:
            self.close()
            self.mqueue = None

    def _createQueue(self):
        if not self.shlib:
            if sys.version_info < (2, 5):
                raise MQError("Statsd Error: require python 2.5 or greater but using %s.%s" %
                              (sys.version_info[0], sys.version_info[1]))
            else:
                raise MQError("Statsd Error: native support for AFTransport is not available")
        try:
            self.mqueue = self.shlib.mq_open(self.mqueue_name, self.flags)
            if LOGGER:
                LOGGER.info("Statsd mqueue %s opened successfully" % self.mqueue)
            if (self.mqueue < 0):
                raise MQError("Statsd Error: AFCollector not installed")
        except Exception, e:
            raise MQError("Statsd Error: unknown error occur when open mqueue")

    def emit(self, data):
        if self.verbosity:
            print "Sending %s" % data
        try:
            if not self.mqueue:
                self._createQueue()
            if self.mqueue:
                self._emit(data)
        except MQSendError, e:
            if LOGGER:
                LOGGER.error(unicode(e))
        except MQError, e:
            print e.msg
            if LOGGER:
                LOGGER.error(e.msg)
            if self.use_udp:
                if self.verbosity:
                    print "Trying to use UDP Transport."
                UDPTransport.emit(self, data)
        except Exception, e:
            self._handleError(data, str(e))

    def _emit(self, data):
        for stat in data.keys():
            value = data[stat]
            send_data = "%s:%s" % (stat, value)
            mlen = min(len(send_data), self.msgLen)
            post = send_data[:mlen]
            if self.verbosity:
                print mlen, post
            rc = self.shlib.mq_send(self.mqueue, post, len(post), STATSD_SEVERITY)
            if (rc < 0):
                errornumber = ctypes.get_errno()
                if errno.errorcode[errornumber] != "EAGAIN":
                    errmsg = os.strerror(errornumber)
                    if LOGGER:
                        LOGGER.error(u"Statsd Error: failed to mq_send %s" % errmsg)

    def close(self):
        if self.mqueue:
            if LOGGER:
                LOGGER.warning(u"mq %s is being closed" % self.mqueue_name)
            try:
                _ = self.shlib.mq_close(self.mqueue)
            except Exception, e:
                pass
            self.mqueue = None

    def __del__(self):
        self.close()

class MQError(Exception):
    def __init__(self, msg=None):
        self.msg = msg or "Statsd Error"

    def __str__(self):
        return str(self.msg)

class MQSendError(Exception):
    def __init__(self, rc, msg=None):
        self.rc = rc
        self.msg = msg or "Statsd Error"

    def __str__(self):
        return str(self.msg) + " return errcode %s" % errno.errorcode(self.rc)

Statsd.set_transport(AFTransport())
Statsd.set_aggregation(True)

if __name__ == "__main__":
#    import time
    Statsd.set_transport(AFTransport(verbosity=True))
    max_count = 1000000
    count = 1
    @Statsd.count("python.test.count")
    @Statsd.time("python.test.time")
    def do_nothing():
        Statsd.timing("python.test.timer",500)
        Statsd.gauge("python.test.gauge",500)
        Statsd.increment("python.test.counter")
        Statsd.decrement("python.test.counter")
        Statsd.update_stats("python.test.counter", 5, sample_rate=1, message="ok")
        Statsd.update_stats("python.test.counter", -5, sample_rate=0)
    while True:
        if max_count and count >= max_count:
            break
        do_nothing()
#        print "count %s" % count
        count += 1