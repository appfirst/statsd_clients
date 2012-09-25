using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Net.Sockets;
using System.Threading;
using System.Diagnostics;
using Statsd;


namespace TestPerformance
{
    class TransportMock : ITransport
    {
        private string lastSent;

        public string LastSend
        {
            get
            {
                return lastSent;
            }
        }

        public bool Send(string message)
        {
            lastSent = message;
            Console.WriteLine(message);
            return true;
        }
    }

    class ThreadPoolStatsd
    {
        private ManualResetEvent doneEvent;

        private string bucketPrefix;

        public ThreadPoolStatsd(ManualResetEvent doneEvent, string bucketPrefix)
        {
            this.doneEvent = doneEvent;
            this.bucketPrefix = bucketPrefix;
        }

        public void SendMessages(Object threadContext)
        {
            StatsdPipe statsd = new StatsdPipe();
            statsd.Transport = new TransportMock();
            statsd.Strategy = new GeyserStrategy(500);

            int threadIndex = (int)threadContext;
            for (int i = 0; i < 100; i++)
            {
                //Thread.Sleep(r.Next(5));
                DateTime start = DateTime.Now;
                statsd.Increment(bucketPrefix + "threadpool.thread" + threadIndex);
                int elapsedTimeMillis = Convert.ToInt32((DateTime.Now - start).TotalMilliseconds);
                statsd.Timing(bucketPrefix + "threadpool.incr_time", elapsedTimeMillis);
            }
            doneEvent.Set();
        }
    }

    class Program
    {
        const string bucketPrefix = "csharp.test.";

        static Random r = new Random();

        static void TestUnderPressure()
        {
            Console.WriteLine("TestUnderPressure");
            StatsdPipe statsd = new StatsdPipe();
            statsd.Transport = new TransportMock();
            statsd.Strategy = new GeyserStrategy(500);

            statsd.Increment(bucketPrefix + "pressure.multiple1");
            try
            {
                statsd.Gauge(bucketPrefix + "pressure.multiple1",1 );
            }
            catch (BucketTypeMismatchException btme)
            {
                Console.WriteLine(btme.Message);
            }

		    for (int i=0; i<1000000; i++){
			    DateTime start = DateTime.Now;
                statsd.Increment(bucketPrefix + "pressure.multiple1");
                int elapsedTimeMillis = Convert.ToInt32((DateTime.Now - start).TotalMilliseconds);
                statsd.Timing(bucketPrefix + "pressure.incr_time", elapsedTimeMillis);
			    if (i%3==0){
                    statsd.Increment(bucketPrefix + "pressure.multiple3");
			    }
		    }
	    }

        static void TestMultiThreading()
        {
            Console.WriteLine("TestMultiThreading");
            ManualResetEvent[] doneEvents = new ManualResetEvent[10];
            for (int i = 0; i < doneEvents.Length; i++)
            {
                doneEvents[i] = new ManualResetEvent(false);
                ThreadPoolStatsd tps = new ThreadPoolStatsd(doneEvents[i], bucketPrefix);
                ThreadPool.QueueUserWorkItem(tps.SendMessages, i);
            }

            WaitHandle.WaitAll(doneEvents);
        }

        static void TestMailSlotStreaming()
        {
            Console.WriteLine("TestMailSlotStreaming");
            StatsdPipe statsd = new StatsdPipe();
            statsd.Strategy = new GeyserStrategy(2000);

            for (int i=0; i<10; i++)
            {
                //Thread.Sleep(r.Next(5));
                Thread.Sleep(1000);
                statsd.Timing(bucketPrefix + "newmailslot.timer", 16);
                statsd.Gauge(bucketPrefix + "newmailslot.gauge", 8);
                statsd.UpdateCount(4, bucketPrefix + "newmailslot.counter");
                statsd.UpdateCount(2, bucketPrefix + "newmailslot.counter");
                statsd.Increment(bucketPrefix + "newmailslot.counter");
                statsd.Decrement(bucketPrefix + "newmailslot.counter");
            }
        }

        static void ShowExample(IStatsdClient statsd)
        {
            statsd.Gauge(bucketPrefix + "gauge", 500);
            statsd.Gauge("test|Gauge(string message, string key, int value)", bucketPrefix + "gauge", 500);
            statsd.Timing(bucketPrefix + "timer", 500);
            statsd.Timing("test|Timer(string message, string key, int value)", bucketPrefix + "timer", 500);
            statsd.Increment(bucketPrefix + "counter");
            statsd.Decrement(bucketPrefix + "counter");
            statsd.UpdateCount(2, bucketPrefix + "counter");
            statsd.UpdateCount(3, 0, bucketPrefix + "counter", bucketPrefix + "counter2");
            statsd.UpdateCount(4, 2, bucketPrefix + "counter", bucketPrefix + "counter2");
            statsd.UpdateCount("test|UpdateCount(string message, string key, int value)", 5, bucketPrefix + "counter", bucketPrefix + "counter2");
            statsd.UpdateCount("UpdateCount(string message, string key, int value)|test", 6, 1, bucketPrefix + "counter");
        }

        static void TestBasic()
        {
            Console.WriteLine("TestBasic");
            StatsdPipe statsd = new StatsdPipe();
            statsd.Strategy = new GeyserStrategy(5000);
            while (true)
            {
                Thread.Sleep(1000);

                ShowExample(statsd);
            }
        }

        static void TestTimeStamp()
        {
            Debug.WriteLine(TimestampHelper.Now);
            Debug.WriteLine(TimestampHelper.ConvertToDateTime(TimestampHelper.Now));
        }

        static void Main(string[] args)
        {
            TestUnderPressure();
            TestMultiThreading();
            TestMailSlotStreaming();
            ShowExample(new StatsdPipe());
            TestBasic();
        }
    }
}
