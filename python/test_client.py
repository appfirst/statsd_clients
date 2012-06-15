import unittest

class StatsdClientTest(unittest.TestCase):
    def test_build_message(self):
        from client import Statsd

        # no nothing
        self.assertEqual(Statsd._build_message({"test":"1|c"}),{'test': '1|c'})

        # with message
        self.assertEqual(Statsd._build_message({"test":"1|c"}, 1, "hello"),
                         {"test":"1|c||hello"})
        # with timestamp
        self.assertEqual(Statsd._build_message({"test":"1|c"}, 1, timestamp=1339793258),
                         {"test":"1|c|1339793258"})
        # with timestamp and message
        self.assertEqual(Statsd._build_message({"test":"1|c"}, 1, "hello",1339793258),
                         {"test":"1|c|1339793258|hello"})