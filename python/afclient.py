#!/usr/local/bin/python
# -*- coding: utf-8 -*-
"""
The AppFirst Statsd Transport
"""
__all__=['AfTransport']

import logging, os, client
try:
    import ctypes
except Exception as e:
    ctypes = None

from client import *

STATSD_SEVERITY = 3

class AFTransport(UDPTransport):
    def __init__(self, useUDP=False):
        self.mqueue_name = "/afcollectorapi"
        self.flags = 04001
        self.msgLen = 2048
        self.mqueue = None
        self.verbosity = False
        self.shlib = self.loadlib() if not useUDP else None

    def loadlib(self):
        if ctypes:
            try:
                ctypes.cdll.LoadLibrary("librt.so.1")
                return ctypes.CDLL("librt.so.1")
            except Exception:
                return None

    def _handleError(self, data, emsg=" "):
        if self.mqueue:
            self.close()
            self.mqueue = None
        if self.verbosity:
            print "Error: ", emsg
        import sys
        from pprint import pprint
        print "Unexpected error:", pprint(sys.exc_info())
        pass # we don't care

    def _createQueue(self):
        if not self.shlib:
            return False
        try:
            self.mqueue = self.shlib.mq_open(self.mqueue_name, self.flags)
            if (self.mqueue < 0) :
                return False
        except Exception as e:
            return False
        return True

    def emit(self, data):
        if not self.mqueue and not self._createQueue():
            self.mqueue = None
        if self.mqueue:
            self._emit(data)
        else:
            if self.verbosity:
                print "AFCollector not installed, Using UDP Transport"
            UDPTransport.emit(self, data)

    def _emit(self, data):
        try:
            for stat in data.keys():
                value = data[stat]
                send_data = "%s:%s" % (stat, value)
                mlen = min(len(send_data), self.msgLen)
                post = send_data[:mlen]
                if self.verbosity:
                    print mlen, post
                rc = self.shlib.mq_send(self.mqueue, post, len(post), STATSD_SEVERITY)
                if (rc < 0):
                    self._handleError(record, "mq_send")
        except Exception as e:
            self._handleError(record, "mq_send")

    def close(self):
        if self.mqueue:
            try:
                rc = self.shlib.mq_close(self.mqueue)
            except Exception as e:
                pass
            self.mqueue = None
