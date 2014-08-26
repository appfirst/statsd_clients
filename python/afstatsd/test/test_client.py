#!/usr/bin/env python
# encoding: utf-8

"""
Test the StatsD class and make sure returned bucket strings are correct
"""

import unittest

from ..afclient import AFTransport
from ..client import UDPTransport, Statsd
from ..client import CounterBucket, TimerBucket, GaugeBucket


class StatsDClientTest(unittest.TestCase):
    """
    TODO:
        - Test Statsd class w/ aggregator
        - Test AFTransport & UDPTransport
    """
    
    def test_buckets(self):
        # Set initial values
        counter_bucket = CounterBucket('test.bucket.counter', 10)
        gauge_bucket = GaugeBucket('test.bucket.gauge', 10)
        timer_bucket = TimerBucket('test.bucket.timer', 10)

        # Simulate sending second value & aggregate
        counter_update = CounterBucket('test.bucket.counter', 5)
        gauge_update = GaugeBucket('test.bucket.gauge', 5)
        timer_update = TimerBucket('test.bucket.timer', 5)
        counter_bucket.aggregate(counter_update.stat)
        gauge_bucket.aggregate(gauge_update.stat)
        timer_bucket.aggregate(timer_update.stat)

        # Test results
        self.assertEqual(counter_bucket.format_string(), '15|c')
        self.assertEqual(gauge_bucket.format_string(), '5|g|{0}'.format(gauge_bucket.timestamp))
        self.assertEqual(timer_bucket.format_string(), '10,5|ms')


if __name__ == '__main__':
    unittest.main()
