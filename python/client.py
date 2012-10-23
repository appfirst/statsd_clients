'''
Created on Jun 14, 2012

@author: Steve Ivy <steveivy@gmail.com>
@co-author: yangming <yangming@appfirst.com>
http://www.appfirst.com

python client for appfirst statsd+
this file expects local_settings.py to be in the same dir, with statsd host and port information:

statsd_host = 'localhost'
statsd_port = 8125

Sends statistics to the stats daemon over UDP
Sends statistics to the appfirst collector over UDP
'''
import sys
import time
import random
import threading
from socket import socket, AF_INET, SOCK_DGRAM

#---------------------------------------------------------------------------
#   Default UDP Transport
#---------------------------------------------------------------------------
class UDPTransport(object):
    def emit(self, data):
        """
        Squirt the metrics over UDP
        """
        try:
            import local_settings as settings
            host = settings.statsd_host
            port = settings.statsd_port
            addr=(host, port)
        except Exception:
            exit(1)

        udp_sock = socket(AF_INET, SOCK_DGRAM)
        try:
            for stat in data.keys():
                value = data[stat]
                send_data = "%s:%s" % (stat, value)
                udp_sock.sendto(send_data, addr)
        except:
            from pprint import pprint
            print "Unexpected error:", pprint(sys.exc_info())
            pass # we don't care

    def close(self):
        pass

#---------------------------------------------------------------------------
#   Statsd Buffer to aggregate stats of the same bucket and dump them together
#---------------------------------------------------------------------------
class StatsdBuffer(object):
    def __init__(self):
        self.buf = {}
        self.lock = threading.Lock()

    def is_empty(self):
        if self.buf:
            return False
        else:
            return True

    def add(self, bucket, clazz, stat, message=None, sample_rate=1):
        self.lock.acquire()
        try:
            if (clazz == CounterBucket):
                stat = self.buf.setdefault(bucket, clazz(bucket)) \
                           .aggregate(stat, message, sample_rate)
            elif (clazz == TimerBucket):
                stat = self.buf.setdefault(bucket, clazz(bucket)) \
                           .aggregate(stat, message)
            elif (clazz == GaugeBucket):
                stat = self.buf.setdefault(bucket, clazz(bucket)) \
                           .aggregate(stat, message)
        finally:
            self.lock.release()
        return stat

    def dump(self):
        self.lock.acquire()
        try:
            laststat = self.buf
            self.buf = {}
        finally:
            self.lock.release()
        data = dict([(k,str(v)) for k,v in laststat.iteritems()])
        return data

class CounterBucket(object):
    def __init__(self, bucket):
        self.bucket = bucket
        self.stat = 0

    def __str__(self):
        if self.message:
            return "%s|c||%s" % (self.stat, self.message)
        else:
            return "%s|c" % self.stat

    def aggregate(self, stat, message=None, sample_rate=1):
        if sample_rate < 1 and random.random() > sample_rate:
            return self
        self.stat += int(stat/sample_rate)
        self.message = message
        # for chaining
        return self

class TimerBucket(object):
    def __init__(self, bucket):
        self.bucket = bucket
        self.summstat = 0
        self.count = 0

    def __str__(self):
        avg = self.summstat/self.count;
        if self.message:
            return "%s|ms||%s" % (avg, self.message)
        else:
            return "%s|ms" % avg

    def aggregate(self, stat, message=None):
        self.summstat += stat
        self.count += 1
        self.message = message
        # for chaining
        return self

class GaugeBucket(object):
    def __init__(self, bucket):
        self.bucket = bucket

    def __str__(self):
        if self.message:
            return "%s|g|%s|%s" % (self.stat, self.timestamp, self.message)
        else:
            return "%s|g|%s" % (self.stat, self.timestamp)

    def aggregate(self, stat, message=None):
        self.stat = stat
        self.message = message
        self.timestamp=int(time.time())
        # for chaining
        return self

#---------------------------------------------------------------------------
#   Stategy such as where the data is stored and how frequent the stats are sent
#---------------------------------------------------------------------------
class GeyserStategy():
    def __init__(self, interval=20):
        self.interval = interval
        self.triggered = False

    def setup(self, func, *args, **kwargs):
        if self.triggered:
            return
        self.triggered = True
        def wrap_func():
            try:
                result = func(*args, **kwargs)
            finally:
                self.triggered = False
            return result
        self.t = threading.Timer(self.interval, wrap_func)
        self.t.daemon = True
        self.t.start()

class InstantStategy():
    def setup(self, func, *args, **kwargs):
        return func(*args, **kwargs)

#---------------------------------------------------------------------------
#   Statsd Client
#---------------------------------------------------------------------------
class Statsd(object):

    _buffer = StatsdBuffer()
    _transport = UDPTransport()
    _strategy = InstantStategy()

    @staticmethod
    def set_transport(transport):
        Statsd._transport.close()
        Statsd._transport = transport

    @staticmethod
    def set_strategy(strategy):
        Statsd._strategy = strategy

    @staticmethod
    def gauge(bucket, reading, message=None):
        """
        Log gauge information
        >>> from client import Statsd
        >>> Statsd.gauge('some.gauge', 500)
        """
        Statsd._buffer.add(bucket, GaugeBucket, reading, message)
        Statsd.send()

    @staticmethod
    def timing(bucket, elapse, message=None):
        """
        Log timing information
        >>> from client import Statsd
        >>> Statsd.timing('some.time', 500)
        """
        Statsd._buffer.add(bucket, TimerBucket, elapse, message)
        Statsd.send()

    @staticmethod
    def increment(buckets, sample_rate=1, message=None):
        """
        Increments one or more stats counters
        >>> Statsd.increment('some.int')
        >>> Statsd.increment('some.int',0.5)
        """
        Statsd.update_stats(buckets, 1, sample_rate, message)

    @staticmethod
    def decrement(buckets, sample_rate=1, message=None):
        """
        Decrements one or more stats counters
        >>> Statsd.decrement('some.int')
        """
        Statsd.update_stats(buckets, -1, sample_rate, message)

    @staticmethod
    def update_stats(buckets, delta=1, sample_rate=1, message=None):
        """
        Updates one or more stats counters by arbitrary amounts
        >>> Statsd.update_stats('some.int',10)
        """
        if (type(buckets) is not list):
            buckets = [buckets]
        for bucket in buckets:
            Statsd._buffer.add(bucket, CounterBucket, delta, message, sample_rate)
        Statsd.send()

    @staticmethod
    def send():
        Statsd._strategy.setup(Statsd.flush, Statsd._buffer)

    @staticmethod
    def flush(buf):
        Statsd._transport.emit(buf.dump())

    @staticmethod
    def shutdown():
        if not Statsd._buffer.is_empty():
            Statsd.flush(Statsd._buffer)
        Statsd._transport.close()

    @staticmethod
    def time(bucket, enabled=True):
        """
        Convenient wrapper.
        This will count how many this wrapped function is invoked.

        >>>@Statsd.time("some.timer.bucket")
        >>>def some_func():
        >>>    pass #do something
        """
        def wrap_timer(method):
            if not enabled:
                return method
            def send_statsd(*args, **kwargs):
                start = time.time()
                result = method(*args, **kwargs)
                duration = (time.time() - start) * 1000
                Statsd.timing(bucket, duration)
                return result
            return send_statsd
        return wrap_timer

    @staticmethod
    def count(buckets, sample_rate=1, enabled=True):
        """
        Convenient wrapper.
        This will count how many this wrapped function is invoked.

        @Statsd.count("some.counter.bucket")
        def some_func():
            pass #do something
        """
        def wrap_counter(method):
            if not enabled:
                return method
            def send_statsd(*args, **kwargs):
                result = method(*args, **kwargs)
                Statsd.increment(buckets, sample_rate)
                return result
            return send_statsd
        return wrap_counter


'''
shutdown automatically on application exit...
'''
try:
    import atexit
    atexit.register(Statsd.shutdown)
except ImportError: # for Python versions < 2.0
    def exithook(status, old_exit=sys.exit):
        try:
            Statsd.shutdown()
        finally:
            old_exit(status)

    sys.exit = exithook
