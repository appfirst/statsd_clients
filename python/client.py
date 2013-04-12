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
        #print "launching service loop"
        while self.running:
            #print "top of service loop"
            time.sleep(self.interval/2.0)
            self.swap_buffers()        
            time.sleep(self.interval/2.0)
            self.dump()

    def start(self):
        #print "starting aggregation"
        if self.running:
            return
        else:
            self.running = True
            if self._service_thread == None:
                self._service_thread = threading.Thread(target=self.service_loop)
                self._service_thread.daemon = True
            self._service_thread.start()
    
    def stop(self):
        #print "stopping aggregation"
        if self.running:
            self.running = False    
            self.dump()
            self.swap_buffers()
            self.dump()
        
    def is_empty(self):
        if self.buf:
            return False
        else:
            return True

    def add(self, bucket):
        # is setdefault atomic (thread safe)?  It's faster!
        write_buffer = self.wbufs.setdefault(threading.currentThread(), {})
        '''
        if threading.currentThread() in self.wbufs:
            write_buffer = self.wbufs[threading.currentThread()]
        else:
            #print "creating new write buffer for new thread"
            write_buffer = {}
            self.lock.acquire()
            self.wbufs[threading.currentThread()] = write_buffer
            self.lock.release()    
        '''    
        if bucket.name in write_buffer:
            #print "\texisting bucket"
            write_buffer[bucket.name].aggregate(bucket.stat)
        else:
            #print "\tvirgin bucket"
            write_buffer[bucket.name] = bucket
        return 

    def dump(self):
        # aggregate data across all read buffers
        send_buffer = {}
        for th in self.rbufs:
            #print "dump:found an rbuf"
            read_buffer = self.rbufs[th]       
            for name, bucket in read_buffer.iteritems():
                #print "dump:found a bucket"
                if name in send_buffer:
                    #print "dump: existsing agg bucket"
                    send_buffer[name].aggregate(bucket.stat)
                else:
                    #print "dump: virgin agg bucket"
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
    

class CounterBucket(object):
    def __init__(self, name, stat, rate=1, message=None):
        self.name = name
        self.stat = stat
        self.rate = rate
        self.message = message

    def __str__(self):
        if self.message:
            return "%s|c||%s" % (self.stat, self.message)
        else:
            return "%s|c" % self.stat

    def aggregate(self, stat):
        # Note:  This is non-standard.  We should not divide this out, but instead send the semple rate upstream (with @rate)
        self.stat += int(stat/self.rate)
        # for chaining
        return self

class TimerBucket(object):
    def __init__(self, name, stat, message=None):
        self.name = name
        self.summstat = stat
        self.count = 1
        self.message = message

    def __str__(self):
        avg = self.summstat/self.count;
        if self.message:
            return "%s|ms||%s" % (avg, self.message)
        else:
            return "%s|ms" % avg

    def aggregate(self, stat):
        self.summstat += stat
        self.count += 1
        # for chaining
        return self

class GaugeBucket(object):
    def __init__(self, name, stat, message=None):
        self.name = name
        self.stat = stat
        self.message = ""
        self.timestamp=int(time.time())

    def __str__(self):
        if self.message:
            return "%s|g|%s|%s" % (self.stat, self.timestamp, self.message)
        else:
            return "%s|g|%s" % (self.stat, self.timestamp)

    def aggregate(self, stat):
        self.stat = stat
        self.timestamp=int(time.time())
        # for chaining
        return self

#---------------------------------------------------------------------------
#   Stategy such as where the data is stored and how frequent the stats are sent
#---------------------------------------------------------------------------
class GeyserStategy():
    def __init__(self, interval=10):
        print "deprecated"

class InstantStategy():
    def __init__(self):
        print "deprecated"

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
        #print "setting aggregation to", should_aggregate
        if should_aggregate and not Statsd._aggregator.running:
            Statsd._aggregator.start()
        if not should_aggregate and Statsd._aggregator.running:
            Statsd._aggregator.stop()
            
    @staticmethod
    def set_strategy(strategy):
        # deprecated.  Try to fake it
        if strategy.__name__ == "GeyserStategy":
            print "Warning: using deprecated method to enable aggregation"
            Statsd.set_aggregation(True)
        else:
            print "Warning: using deprecated method to disable aggregation"
            Statsd.set_aggregation(False)

    @staticmethod
    def gauge(name, reading, message=None):
        """
        Log gauge information
        >>> from client import Statsd
        >>> Statsd.gauge('some.gauge', 500)
        """
        GaugeBucket(name, reading, message)
        Statsd.send(GaugeBucket(name, reading, message))

    @staticmethod
    def timing(name, elapse, message=None):
        """
        Log timing information
        >>> from client import Statsd
        >>> Statsd.timing('some.time', 500)
        """
        Statsd.send(TimerBucket(name, int(round(elapse)), message))

    @staticmethod
    def increment(names, sample_rate=1, message=None):
        """
        Increments one or more stats counters
        >>> Statsd.increment('some.int')
        >>> Statsd.increment('some.int',0.5)
        """
        Statsd.update_stats(names, 1, sample_rate, message)

    @staticmethod
    def decrement(names, sample_rate=1, message=None):
        """
        Decrements one or more stats counters
        >>> Statsd.decrement('some.int')
        """
        Statsd.update_stats(names, -1, sample_rate, message)

    @staticmethod
    def update_stats(names, delta=1, sample_rate=1, message=None):
        """
        Updates one or more stats counters by arbitrary amounts
        >>> Statsd.update_stats('some.int',10)
        """
        if sample_rate < 1 and random.random() > sample_rate:
            return
        if (type(names) is not list):
            names = [names]
        for name in names:
            Statsd.send(CounterBucket(name, int(round(delta)), sample_rate, message))

    @staticmethod
    def send(bucket):
        if Statsd._aggregator.running:
            Statsd._aggregator.add(bucket)
        else:
            bucket = [bucket]
            Statsd._transport.emit(bucket)

    @staticmethod
    def flush(buf):
        Statsd._transport.emit(buf.dump())

    @staticmethod
    def shutdown():
        #print "statsd shutdown"
        Statsd._aggregator.stop()
        Statsd._transport.close()

    @staticmethod
    def time(name, enabled=True):
        '''
        Function Decorator to report function execution time.

        >>>@Statsd.time("some.timer.bucket")
        >>>def some_func():
        >>>    pass #do something
        '''
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
        '''
        Function Decorator to count how many times a function is invoked.

        @Statsd.count("some.counter.bucket")
        def some_func():
            pass #do something
        '''
        def wrap_counter(method):
            if not enabled:
                return method
            def send_statsd(*args, **kwargs):
                result = method(*args, **kwargs)
                Statsd.increment(name, sample_rate)
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
