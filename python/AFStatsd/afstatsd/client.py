# -*- encoding: utf-8 -*-

"""
Created on Jun 14, 2012

@author: Steve Ivy <steveivy@gmail.com>
@co-author: yangming <yangming@appfirst.com>
http://www.appfirst.com

Updated for Python 3 May 14, 2014 by michael@appfirst.com

Python client for AppFirst Statsd+
this file expects local_settings.py to be in the same dir, with statsd host and
port information:

statsd_host = 'localhost'
statsd_port = 8125

Sends statistics to a stats daemon over UDP
Sends statistics to the AppFirst collector over POSIX MQ or Windows Mailslot
"""

import sys
import time
import random
import atexit
import threading
from socket import socket, AF_INET, SOCK_DGRAM


#---------------------------------------------------------------------------
#   Default UDP Transport
#---------------------------------------------------------------------------
class UDPTransport(object):
    def __init__(self, host='localhost', port=8125):
        self.host = host
        self.port = port

    def emit(self, data):
        """
        Send the metrics over UDP
        """
        addr=(self.host, self.port)

        udp_sock = socket(AF_INET, SOCK_DGRAM)
        try:
            for name, value in data.items():
                send_data = "{0}:{1}".format(name, value.format_string(udp=True))
                udp_sock.sendto(send_data, addr)
        except Exception as e:
            sys.stderr.write("Error emitting stats over UDP: {0.__class__.__name__}: {0}\n".format(e))

    def close(self):
        pass


#---------------------------------------------------------------------------
#   Statsd Aggregator to buffer stats of the same bucket and dump them together
#---------------------------------------------------------------------------
class StatsdAggregator(object):
    def __init__(self, interval, transport):
        self.running = False
        self.interval = interval
        self.transport = transport
        self.buf = {}
        self.lock = threading.Lock()
        self._service_thread = None

        self.left_buffers = {}   # 2 buffer groups, each stored in a dict
        self.right_buffers = {}  # one of each for each thread
        self.rbufs = self.left_buffers    # buffer group currently being read from
        self.wbufs = self.right_buffers   # buffer group currently being written to

    def service_loop(self):
        while self.running:
            time.sleep(self.interval/2.0)
            self.swap_buffers()
            time.sleep(self.interval/2.0)
            self.dump()

    def start(self):
        """
        Start aggregation
        """
        if self.running:
            return
        else:
            self.running = True
            if self._service_thread == None:
                self._service_thread = threading.Thread(target=self.service_loop)
                self._service_thread.daemon = True
            self._service_thread.start()

    def stop(self):
        """
        Stop aggregation
        """
        if self.running:
            self.running = False
            self.dump()
            self.swap_buffers()
            self.dump()

    def is_empty(self):
        """
        Check if data in self.buf
        """
        if self.buf:
            return False
        else:
            return True

    def add(self, bucket):
        # is setdefault atomic (thread safe)?  It's faster!
        write_buffer = self.wbufs.setdefault(threading.currentThread(), {})
        """
        if threading.currentThread() in self.wbufs:
            write_buffer = self.wbufs[threading.currentThread()]
        else:
            #print "creating new write buffer for new thread"
            write_buffer = {}
            self.lock.acquire()
            self.wbufs[threading.currentThread()] = write_buffer
            self.lock.release()
        """
        if bucket.name in write_buffer:
            # aggregate if bucket is already in bucket
            write_buffer[bucket.name].aggregate(bucket.stat)
        else:
            # otherwise add
            write_buffer[bucket.name] = bucket
        return

    def dump(self):
        """
        aggregate data across all read buffers and then emit
        """
        send_buffer = {}
        for th in self.rbufs:
            read_buffer = self.rbufs[th]
            for name, bucket in read_buffer.items():
                if name in send_buffer:
                    send_buffer[name].aggregate(bucket.stat)
                else:
                    send_buffer[name]=bucket
            read_buffer.clear()
        self.transport.emit(send_buffer)

    def swap_buffers(self):
        if self.rbufs == self.left_buffers:
            self.rbufs = self.right_buffers
            self.wbufs = self.left_buffers
        else:
            self.rbufs = self.left_buffers
            self.wbufs = self.right_buffers


class Bucket(object):
    def format_string(self, udp=False):
        if udp:
            return self._to_udp_string()
        else:
            return self._to_af_string()

    def _to_udp_string(self):
        raise NotImplementedError

    def _to_af_string(self):
        return self._to_udp_string()


class CounterBucket(Bucket):
    def __init__(self, name, stat, rate=1):
        self.name = name
        self.stat = stat
        self.rate = rate

    def _to_udp_string(self):
        return "{0}|c".format(self.stat)

    def aggregate(self, stat):
        """
        CounterBuckets are aggregated by adding new values to
        the current value.
        """
        # Note: This is non-standard. We should not divide this out,
        #  but instead send the semple rate upstream (with @rate)
        self.stat += int(stat/self.rate)


class TimerBucket(Bucket):
    def __init__(self, name, stat):
        self.name = name
        self.stat = [stat]
        self.count = 1

    def _to_af_string(self):
        """
        Sending up the full list of values by default so AppFirst can calculate
        the max/min during the interval as well.
        """
        return "{0}|ms".format(','.join([str(n) for n in self.stat]))

    def _to_udp_str(self):
        """
        Only send up the average if emitting over UDP so we don't break existing
        StatsD implementations.
        """
        avg = sum(self.stat) / self.count
        return "{0}|ms".format(avg)

    def aggregate(self, stat):
        """
        TimerBuckets are aggregated by adding new time values to the existing
        time values and incrementing a counter used to get an average time.
        """
        self.stat.extend(stat)
        self.count += 1


class GaugeBucket(Bucket):
    def __init__(self, name, stat):
        self.name = name
        self.stat = stat
        self.timestamp=int(time.time())

    def _to_udp_string(self):
        return "{0}|g|{1}".format(self.stat, self.timestamp)

    def aggregate(self, stat):
        """
        GuageBuckets are updated by setting the current gauge value to the new
        value. No actual aggregation is done.
        """
        self.stat = stat
        self.timestamp = int(time.time())


#---------------------------------------------------------------------------
#   Statsd Client
#---------------------------------------------------------------------------
class Statsd(object):
    _transport = UDPTransport()
    _aggregator = StatsdAggregator(20, _transport)

    @staticmethod
    def set_transport(transport):
        Statsd._transport.close()
        Statsd._transport = transport
        Statsd._aggregator.transport = transport

    @staticmethod
    def set_aggregation(should_aggregate):
        if should_aggregate and not Statsd._aggregator.running:
            Statsd._aggregator.start()
        if not should_aggregate and Statsd._aggregator.running:
            Statsd._aggregator.stop()

    @staticmethod
    def gauge(name, reading):
        """
        Log gauge information
        >>> from client import Statsd
        >>> Statsd.gauge('some.gauge', 500)
        """
        GaugeBucket(name, reading)
        Statsd.send(GaugeBucket(name, reading))

    @staticmethod
    def timing(name, elapse):
        """
        Log timing information
        >>> from client import Statsd
        >>> Statsd.timing('some.time', 500)
        """
        Statsd.send(TimerBucket(name, int(round(elapse))))

    @staticmethod
    def increment(names, sample_rate=1):
        """
        Increments one or more stats counters
        >>> Statsd.increment('some.int')
        >>> Statsd.increment('some.int', 0.5)
        """
        Statsd.update_stats(names, 1, sample_rate)

    @staticmethod
    def decrement(names, sample_rate=1):
        """
        Decrements one or more stats counters
        >>> Statsd.decrement('some.int')
        """
        Statsd.update_stats(names, -1, sample_rate)

    @staticmethod
    def update_stats(names, delta=1, sample_rate=1):
        """
        Updates one or more stats counters by arbitrary amounts
        >>> Statsd.update_stats('some.int', 10)

        Sample rate is a decimal value representing the proportion of stats
        to keep. For example, if sample_rate is 0.5, then 50% of stats will
        be discarded. Default value is 1 and does not discard any stats.
        """
        if sample_rate < 1 and random.random() > sample_rate:
            return
        if not isinstance(names, list):
            names = [names]
        for name in names:
            Statsd.send(CounterBucket(name, int(round(delta)), sample_rate))

    @staticmethod
    def send(bucket):
        if Statsd._aggregator.running:
            Statsd._aggregator.add(bucket)
        else:
            bucket = {bucket.name: bucket}
            Statsd._transport.emit(bucket)

    @staticmethod
    def flush(buf):
        Statsd._transport.emit(buf.dump())

    @staticmethod
    def shutdown():
        Statsd._aggregator.stop()
        Statsd._transport.close()

    @staticmethod
    def time(name, enabled=True):
        """
        Function Decorator to report function execution time.

        >>> @Statsd.time("some.timer.bucket")
        >>> def some_func():
        >>>     pass  # do something
        """
        def wrap_timer(method):
            if not enabled:
                return method
            def send_statsd(*args, **kwargs):
                start = time.time()
                result = method(*args, **kwargs)
                duration = (time.time() - start) * 1000
                Statsd.timing(name, duration)
                return result
            return send_statsd
        return wrap_timer

    @staticmethod
    def count(name, sample_rate=1, enabled=True):
        """
        Function Decorator to count how many times a function is invoked.

        >>> @Statsd.count("some.counter.bucket")
        >>> def some_func():
        >>>     pass #do something
        """
        def wrap_counter(method):
            if not enabled:
                return method
            def send_statsd(*args, **kwargs):
                result = method(*args, **kwargs)
                Statsd.increment(name, sample_rate)
                return result
            return send_statsd
        return wrap_counter


# shutdown automatically on application exit...
atexit.register(Statsd.shutdown)
