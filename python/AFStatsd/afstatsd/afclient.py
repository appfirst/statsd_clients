#!/usr/local/env python
# -*- encoding: utf-8 -*-

"""
The AppFirst Statsd Transport
"""

__all__=['AFTransport', 'Statsd', 'UDPTransport']

try:
    import ctypes
except Exception as e:
    ctypes = None

import sys
import os
import errno

from .client import UDPTransport, Statsd

PYTHON3 = sys.version_info[0] == 3
STATSD_SEVERITY = 3
LOGGER = None


def set_logger(logger):
    global LOGGER
    LOGGER = logger


class AFTransport(UDPTransport):
    def __init__(self, use_udp=True, verbosity=False, logger=None):
        set_logger(logger)
        self.mqueue_name = "/afcollectorapi"
        if PYTHON3:
            # Convert from Python 3's default unicode
            self.mqueue_name = self.mqueue_name.encode('ascii')
        self.flags = 0o4001
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
            except:
                return None

    def _handleError(self, data, emsg=""):
        if LOGGER:
            LOGGER.error("Statsd Error: {0} when sending {1}".format(emsg, data))
        if self.mqueue:
            self.close()
            self.mqueue = None

    def _createQueue(self):
        if not self.shlib:
            raise MQError("Statsd Error: native support for AFTransport is not available")
        try:
            self.mqueue = self.shlib.mq_open(self.mqueue_name, self.flags)
            if LOGGER:
                LOGGER.info("Statsd mqueue {0} opened successfully".format(self.mqueue))
            if (self.mqueue < 0):
                raise MQError("Statsd Error: AFCollector not installed")
        except Exception as e:
            raise MQError("Statsd Error: unknown error occur when open mqueue "
                          "({0.__class__.__name__}: {0})".format(e))

    def emit(self, data):
        if self.verbosity and LOGGER:
            LOGGER.info("Sending {0}".format(data))
        try:
            if not self.mqueue:
                self._createQueue()
            if self.mqueue:
                self._emit(data)
        except MQSendError as e:
            if LOGGER:
                LOGGER.error("{0.__class__.__name__}: {0}".format(e))
        except MQError as e:
            if LOGGER:
                LOGGER.error("{0.__class__.__name__}: {0}".format(e))
            if self.use_udp:
                if self.verbosity and LOGGER:
                    LOGGER.info("Trying to use UDP Transport.")
                UDPTransport.emit(self, data)
        except Exception as e:
            self._handleError(data, str(e))

    def _emit(self, data):
        """
        Actually send the data to the collector via the POSIX mq.
        Try bundling multiple messages into one if they won't exceed the max size
        """
        to_post_list = []
        for name, value in data.items():
            send_data = "{0}:{1}".format(name, value)
            if PYTHON3:
                # Unicode not currently supported
                send_data = send_data.encode('ascii')
            mlen = min(len(send_data), self.msgLen)
            post = send_data[:mlen]
            if self.verbosity and LOGGER:
                LOGGER.info("Sending data: {0}".format(repr(post)))

            if len(to_post_list) == 0:
                to_post_list.append(post)
            else:
                previous = to_post_list[-1]
                if PYTHON3:
                    # More unicode fun
                    combined = "{0}::{1}".format(previous.decode('ascii'),
                                                 post.decode('ascii')).encode('ascii')
                else:
                    combined = "{0}::{1}".format(previous, post)
                if len(combined) > self.msgLen:
                    # Combined message would be too long
                    to_post_list.append(post)
                else:
                    # Combine messages to use less space in POSIX mq
                    to_post_list[-1] = combined
        
        for post in to_post_list:
            rc = self.shlib.mq_send(self.mqueue, post, len(post), STATSD_SEVERITY)
            if (rc < 0):
                errornumber = ctypes.get_errno()
                if errno.errorcode[errornumber] != "EAGAIN":
                    errmsg = os.strerror(errornumber)
                    if LOGGER:
                        LOGGER.error("Statsd Error: failed to mq_send {0}".format(errmsg))
                elif LOGGER:
                    LOGGER.error("StatsD queue full; Failed to send message: {0}".format(post))

    def close(self):
        if self.mqueue:
            if LOGGER:
                LOGGER.warning("mq {0} is being closed".format(self.mqueue_name))
            try:
                _ = self.shlib.mq_close(self.mqueue)
            except:
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
    # Test code
    Statsd.set_transport(AFTransport(verbosity=True))
    count = 1

    @Statsd.count("python.test.count")
    @Statsd.time("python.test.time")
    def test_stats():
        Statsd.timing("python.test.timer",500)
        Statsd.gauge("python.test.gauge",500)
        Statsd.increment("python.test.counter")
        Statsd.decrement("python.test.counter")
        Statsd.update_stats("python.test.counter", 5, sample_rate=1)
        Statsd.update_stats("python.test.counter", -5, sample_rate=0)

    while count < 100000:
        test_stats()
        count += 1
