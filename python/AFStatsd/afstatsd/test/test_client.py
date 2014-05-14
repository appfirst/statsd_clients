import unittest
from mock import MagicMock

from client import Statsd, UDPTransport
from afclient import AFTransport


class StatsdClientTest(unittest.TestCase):
    def test_build_message(self):
        Statsd.set_transport(UDPTransport())

        # no nothing
        self.assertEqual(Statsd._build_message({"test":"1|c"}),{'test': '1|c'})

        # with timestamp
        self.assertEqual(Statsd._build_message({"testg":"1|g"}, 1, timestamp=1339793258),
                         {"testg":"1|g|1339793258"})

    def test_AFTransport(self):
        shlib = MagicMock()
        transport = AFTransport()

        transport.shlib = shlib
        Statsd.set_transport(transport)

        shlib.mq_open.return_value = 1
        shlib.mq_send.return_value = 0

        Statsd.increment("mqtest")
        shlib.mq_open.assert_called_once_with("/afcollectorapi", 0o4001)
        self.assertEqual(shlib.mq_open.call_args[0][0], "/afcollectorapi")
        self.assertEqual(shlib.mq_open.call_args[0][1], 0o4001)
        post = "mqtest:1|c"
        shlib.mq_send.assert_called_once_with(1, post, len(post), 3)
        self.assertEqual(shlib.mq_send.call_args[0][0], 1)
        self.assertEqual(shlib.mq_send.call_args[0][1], post)
        self.assertEqual(shlib.mq_send.call_args[0][2], len(post))
        self.assertEqual(shlib.mq_send.call_args[0][3], 3)

    def test_AFTransport_with_UDP(self):
        shlib = MagicMock()
        # TODO: not finished
        transport = AFTransport(useUDP=True)
        Statsd.set_transport(transport)

        #shlib.mq_open.return_value = 1
        #shlib.mq_send.return_value = 0

        Statsd.increment("mqtest")
        self.assertEqual(shlib.mq_open.call_count, 0)

        #post = "mqtest:1|c"
        self.assertEqual(shlib.mq_send.call_count, 0)
