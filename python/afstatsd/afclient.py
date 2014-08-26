#!/usr/local/env python
# -*- encoding: utf-8 -*-
from __future__ import  absolute_import, print_function

"""
The AppFirst Statsd Transport
"""

__all__=['AFTransport', 'Statsd', 'UDPTransport']

import sys
import os
import errno

try:
    import ctypes
except ImportError:
    ctypes = None
try:
    import win32file
    import win32con
except ImportError:
    win32file = None
    win32con = None

from .client import UDPTransport, Statsd


PYTHON3 = sys.version_info[0] == 3
WINDOWS = sys.platform.lower().startswith("win")
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
        try:
            if WINDOWS and win32file is not None:
                self.mqueue = win32file.CreateFile(r'\\.\mailslot\{0}'.format(self.mqueue_name), win32file.GENERIC_WRITE,
                                                   win32file.FILE_SHARE_READ, None, win32con.OPEN_EXISTING, 0, None)
            elif WINDOWS:
                raise MQError("Statsd Error: required Python win32 extension is not available")
            elif not self.shlib:
                raise MQError("Statsd Error: native support for AFTransport is not available")
            else:
                self.mqueue = self.shlib.mq_open(self.mqueue_name, self.flags)
        except MQError:
            raise
        except Exception as e:
            raise MQError("Statsd Error: unknown error occur when open mqueue "
                          "({0.__class__.__name__}: {0})".format(e))
        else:
            if LOGGER:
                LOGGER.info("Statsd mqueue {0} opened successfully".format(self.mqueue))
            if (self.mqueue < 0):
                raise MQError("Statsd Error: Failed to open queue")

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
                UDPTransport().emit(data)
        except Exception as e:
            self._handleError(data, str(e))

    def _emit(self, data):
        """
        Actually send the data to the collector via the POSIX/Mailslot mq.
        Try bundling multiple messages into one if they won't exceed the max size
        """
        to_post_list = []
        for name, value in data.items():
            send_data = "{0}:{1}".format(name, value.format_string(udp=False))
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
            if WINDOWS and PYTHON3:
                # Create bytearray with pid & send to mailslot
                data_string = "{0}:{1}:{2}".format(os.getpid(), 3, post.decode('ascii'))
                data_bytes = bytearray(data_string.encode('utf-16'))
                rc, _ = win32file.WriteFile(self.mqueue, data_bytes, None)
                if rc < 0 and LOGGER:
                    LOGGER.error("Statsd Error: failed to write to Mailslot")
            elif WINDOWS:
                # Create bytearray from unicode with pid & send to mailslot
                data_string = unicode("{0}:{1}:{2}").format(os.getpid(), 3, unicode(post))
                data_bytes = bytearray(data_string.encode('utf-16'))
                rc, _ = win32file.WriteFile(self.mqueue, data_bytes, None)
                if rc < 0 and LOGGER:
                    LOGGER.error("Statsd Error: failed to write to Mailslot")
            else:
                # Send data to POSIX mq
                rc = self.shlib.mq_send(self.mqueue, post, len(post), STATSD_SEVERITY)
                if rc < 0:
                    errornumber = ctypes.get_errno()
                    if errno.errorcode[errornumber] != "EAGAIN":
                        errmsg = os.strerror(errornumber)
                        if LOGGER:
                            LOGGER.error("Statsd Error: failed to mq_send: {0}".format(errmsg))
                    elif LOGGER:
                        LOGGER.error("StatsD queue full; Failed to send message: {0}".format(post))

    def close(self):
        if self.mqueue:
            if LOGGER:
                LOGGER.warning("MQ {0} is being closed".format(self.mqueue_name))
            try:
                if WINDOWS:
                    self.mqueue.Close()
                else:
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
        self.msg = msg if msg is not None else 'Statsd Error'

    def __str__(self):
        return "{0}; return errcode: {1}".format(self.msg, errno.errorcode(self.rc))


Statsd.set_transport(AFTransport())
Statsd.set_aggregation(True)

if __name__ == '__main__':
    # Test code
    Statsd.set_transport(AFTransport(verbosity=True))
    count = 1

    @Statsd.count('python.test.count')
    @Statsd.time('python.test.time')
    def test_stats():
        Statsd.timing('python.test.timer', 500)
        Statsd.gauge('python.test.gauge', 500)
        Statsd.increment('python.test.counter')
        Statsd.decrement('python.test.counter')
        Statsd.update_stats('python.test.counter', 5, sample_rate=1)
        Statsd.update_stats('python.test.counter', -5, sample_rate=0)

    while count < 100000:
        test_stats()
        count += 1
