import unittest
from mock import Mock, MagicMock
from client import Statsd, UDPTransport
from afclient import AFTransport

class StatsdClientTest(unittest.TestCase):
    def test_build_message(self):
        Statsd.set_transport(UDPTransport())

        # no nothing
        self.assertEqual(Statsd._build_message({"test":"1|c"}),{'test': '1|c'})

        # with message
        self.assertEqual(Statsd._build_message({"test":"1|c"}, 1, "hello"),
                         {"test":"1|c||hello"})
        # with timestamp
        self.assertEqual(Statsd._build_message({"testg":"1|g"}, 1, timestamp=1339793258),
                         {"testg":"1|g|1339793258"})
        # with timestamp and message
        self.assertEqual(Statsd._build_message({"testg":"1|g"}, 1, "hello",1339793258),
                         {"testg":"1|g|1339793258|hello"})

    def test_AFTransport(self):
        shlib = MagicMock()
        transport = AFTransport()
        transport.shlib = shlib
        Statsd.set_transport(transport)

        shlib.mq_open.return_value = 1
        shlib.mq_send.return_value = 0

        Statsd.increment("mqtest")
        self.assertEqual(shlib.mq_open.call_args[0][0], "/afcollectorapi")
        self.assertEqual(shlib.mq_open.call_args[0][1], 04001)
        post = "mqtest:1|c"
        self.assertEqual(shlib.mq_send.call_args[0][0], 1)
        self.assertEqual(shlib.mq_send.call_args[0][1], post)
        self.assertEqual(shlib.mq_send.call_args[0][2], len(post))
        self.assertEqual(shlib.mq_send.call_args[0][3], 3)