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
import time
import random
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
        except Error as e:
            exit(1)

        udp_sock = socket(AF_INET, SOCK_DGRAM)
        try:
            for stat in data.keys():
                value = data[stat]
                send_data = "%s:%s" % (stat, value)
                udp_sock.sendto(send_data, addr)
        except:
            import sys
            from pprint import pprint
            print "Unexpected error:", pprint(sys.exc_info())
            pass # we don't care

    def close(self):
        pass

#---------------------------------------------------------------------------
#   Default UDP Transport
#---------------------------------------------------------------------------
class Statsd(object):
    _transport = UDPTransport()

    @staticmethod
    def set_transport(transport):
        Statsd._transport = transport

    @staticmethod
    def gauge(bucket, reading, sample_rate=1, message=None):
        """
        Log timing information
        >>> from python_example import Statsd
        >>> Statsd.timing('some.time', 500)
        """
        stats = {}
        stats[bucket] = "%d|g" % reading
        Statsd.send(stats, sample_rate, message, int(time.time()))

    @staticmethod
    def timing(bucket, elapse, sample_rate=1, message=None):
        """
        Log timing information
        >>> from python_example import Statsd
        >>> Statsd.timing('some.time', 500)
        """
        stats = {}
        stats[bucket] = "%d|ms" % elapse
        Statsd.send(stats, sample_rate, message)

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
    def update_stats(buckets, delta=1, sampleRate=1, message=None):
        """
        Updates one or more stats counters by arbitrary amounts
        >>> Statsd.update_stats('some.int',10)
        """
        if (type(buckets) is not list):
            buckets = [buckets]
        stats = {}
        for bucket in buckets:
            stats[bucket] = "%s|c" % delta

        Statsd.send(stats, sampleRate, message)

    @staticmethod
    def _build_message(data, sample_rate=1, message=None, timestamp=None):
        # the format will be position-invariant:
        # bucket: f0    | f1    | f2                     | f3
        # bucket: value | unit  | sampele_rate/timestamp | message

        # field2(f2) is either sample_rate or timestamp
        field2 = ""
        if (sample_rate < 1):
            if random.random() <= sample_rate:
                field2 = str(sample_rate)
        elif timestamp:
            field2 = str(timestamp)

        # when message is there, we always keep field2 even if it's blank:
        # bucket:2|c||some_message
        if message:
            for stat in data.keys():
                data[stat] += "|%s|%s" % (field2, message)
        elif field2 != "":
            for stat in data.keys():
                data[stat] += "|%s" % field2

        return data

    @staticmethod
    def send(data, sample_rate=1, message=None, timestamp=None):
        data = Statsd._build_message(data, sample_rate, message, timestamp)
        Statsd._transport.emit(data)

    @staticmethod
    def shutdown():
        Statsd._transport.close()

#Let's try and shutdown automatically on application exit...
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
