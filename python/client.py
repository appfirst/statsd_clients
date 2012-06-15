'''
Created on Jun 14, 2012

@author: Steve Ivy <steveivy@gmail.com>
@modifier: yangming <yangming@appfirst.com>
http://www.appfirst.com

python client for appfirst statsd+
this file expects local_settings.py to be in the same dir, with statsd host and port information:

statsd_host = 'localhost'
statsd_port = 8125

Sends statistics to the stats daemon over UDP
Sends statistics to the appfirst collector over UDP
'''
class Statsd(object):

    @staticmethod
    def gauge(bucket, reading, sample_rate=1, message=None):
        """
        Log timing information
        >>> from python_example import Statsd
        >>> Statsd.timing('some.time', 500)
        """
        stats = {}
        stats[bucket] = "%d|g" % reading
        Statsd.send(stats, sample_rate, message)

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
    def send(data, sample_rate=1, message=None):
        """
        Squirt the metrics over UDP
        """
        try:
            import local_settings as settings
            host = settings.statsd_host
            port = settings.statsd_port
            addr=(host, port)
        except Error:
            exit(1)

        sampled_data = {}

        if(sample_rate < 1):
            import random
            if random.random() <= sample_rate:
                for stat in data.keys():
                    value = sampled_data[stat]
                    sampled_data[stat] = "%s|@%s" %(value, sample_rate)
        else:
            sampled_data=data

        from socket import socket, AF_INET, SOCK_DGRAM
        udp_sock = socket(AF_INET, SOCK_DGRAM)
        try:
            for stat in sampled_data.keys():
                value = sampled_data[stat]
                send_data = "%s:%s" % (stat, value)
                udp_sock.sendto(send_data, addr)
        except:
            import sys
            from pprint import pprint
            print "Unexpected error:", pprint(sys.exc_info())
            pass # we don't care