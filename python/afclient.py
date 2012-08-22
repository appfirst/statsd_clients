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

import errno
from exceptions import Exception
from client import UDPTransport, Statsd
STATSD_SEVERITY = 3

LOGGER = None

def set_logger(logger):
    global LOGGER
    LOGGER = logger

class AFTransport(UDPTransport):
    def __init__(self, use_udp=False, verbosity=False):
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
                return ctypes.CDLL("librt.so.1")
            except Exception:
                return None

    def _handleError(self, data, emsg=""):
        if self.mqueue:
            self.close()
            self.mqueue = None
        if self.verbosity:
            print "Error: ", emsg
        import sys
        from pprint import pprint
        print "Unexpected error:", pprint(sys.exc_info())

    def _createQueue(self):
        if not self.shlib:
            import sys
            if sys.version_info < (2, 5):
                raise MQError("Statsd Error: require python 2.5 or greater but using %s.%s" %
                              (sys.version_info[0], sys.version_info[1]))
            else:
                raise MQError("Statsd Error: native support for AFTransport is not available")
        try:
            self.mqueue = self.shlib.mq_open(self.mqueue_name, self.flags)
            if (self.mqueue < 0):
                raise MQError("Statsd Error: AFCollector not installed")
        except Exception, e:
            raise MQError("Statsd Error: unknown error occur when open mqueue")

    def emit(self, data):
        try:
            if not self.mqueue:
                self._createQueue()
            if self.mqueue:
                self._emit(data)
        except MQSendError, e:
            if LOGGER:
                LOGGER.error(str(e))
        except MQError, e:
            print e.msg
            if self.use_udp:
                if self.verbosity:
                    print "Trying to use UDP Transport."
                UDPTransport.emit(self, data)
        except Exception, e:
            self._handleError(data, "mq_send")

    def _emit(self, data):
        for stat in data.keys():
            value = data[stat]
            send_data = "%s:%s" % (stat, value)
            mlen = min(len(send_data), self.msgLen)
            post = send_data[:mlen]
            if self.verbosity:
                print mlen, post
            rc = self.shlib.mq_send(self.mqueue, post, len(post), self.severity)
            if (rc < 0):
                raise MQSendError(rc, "Statsd Error: failed to mq_send")

    def close(self):
        if self.mqueue:
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

if __name__ == "__main__":
    Statsd.set_transport(AFTransport(verbosity=True))
    Statsd.increment("mqtest")
