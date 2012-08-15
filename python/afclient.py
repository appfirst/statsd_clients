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

from exceptions import Exception
from client import UDPTransport, Statsd
STATSD_SEVERITY = 3

class AFTransport(UDPTransport):
    def __init__(self, severity=STATSD_SEVERITY, useUDP=False, verbosity=False):
        self.mqueue_name = "/afcollectorapi"
        self.flags = 04001
        self.msgLen = 2048
        self.mqueue = None
        self.severity = severity
        self.verbosity = verbosity
        if not useUDP:
            self.shlib = self._loadlib()
        else:
            self.shlib = None

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
            if sys.version_info < (2, 4):
                raise MQNotAvailableError("Must use python 2.4 or greater to support ctypes")
            else:
                raise MQNotAvailableError("Can't loading native library that supports Posix MQ")
        try:
            self.mqueue = self.shlib.mq_open(self.mqueue_name, self.flags)
            if (self.mqueue < 0):
                raise MQNotAvailableError("AFCollector not installed")
        except Exception, e:
            raise MQNotAvailableError("Unknown error occur when open Posix MQ")

    def emit(self, data):
        try:
            if not self.mqueue:
                self._createQueue()
            if self.mqueue:
                self._emit(data)
        except MQNotAvailableError, e:
            if self.verbosity:
                print e.msg
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
                raise MQNotAvailableError("Failed to mq_send")

    def close(self):
        if self.mqueue:
            try:
                _ = self.shlib.mq_close(self.mqueue)
            except Exception, e:
                pass
            self.mqueue = None

    def __del__(self):
        self.close()

class MQNotAvailableError(Exception):
    def __init__(self, msg=None):
        self.msg = msg or "failed to check status. check arguments and try again."

    def __str__(self):
        return str(self.msg)

Statsd.set_transport(AFTransport())

if __name__ == "__main__":
    Statsd.set_transport(AFTransport(verbosity=True))
    Statsd.increment("mqtest")