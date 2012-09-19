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
            Debug.WriteLine(message);
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

        public void ThreadPoolCallback(Object threadContext)
        {
            StatsdPipe statsd = new StatsdPipe();
            statsd.Send = new TransportMock().Send;
            GeyserStrategy stra = new GeyserStrategy();
            stra.Interval = 0.5;
            statsd.Strategy = stra;
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
            StatsdPipe statsd = new StatsdPipe();
            statsd.Send = new TransportMock().Send;
            GeyserStrategy stra = new GeyserStrategy();
            stra.Interval = 0.5;
		    statsd.Strategy = stra;
		    for (int i=0; i<10000; i++){
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
            ManualResetEvent[] doneEvents = new ManualResetEvent[10];
            for (int i = 0; i < doneEvents.Length; i++)
            {
                doneEvents[i] = new ManualResetEvent(false);
                ThreadPoolStatsd tps = new ThreadPoolStatsd(doneEvents[i], bucketPrefix);
                ThreadPool.QueueUserWorkItem(tps.ThreadPoolCallback, i);
            }

            WaitHandle.WaitAll(doneEvents);
        }

        

        static void TestMailSlotStreaming()
        {
            GeyserStrategy strategy = new GeyserStrategy();
            strategy.Interval = 2000;

            StatsdPipe statsd = new StatsdPipe();
            statsd.Strategy = strategy;

            for (int i=0; i<1000; i++)
            {
                Thread.Sleep(r.Next(5));
                statsd.Increment(bucketPrefix + "mailslot");
            }
        }

        static void ShowExample()
        {
            StatsdPipe statsd = new StatsdPipe();

            statsd.Gauge(bucketPrefix + "gauge", 500);
            statsd.Gauge("Gauge(string message, string key, int value)", bucketPrefix + "gauge", 500);
            statsd.Timing(bucketPrefix + "timer", 500);
            statsd.Timing("Timer(string message, string key, int value)", bucketPrefix + "timer", 500);
            statsd.Increment(bucketPrefix + "counter");
            statsd.Decrement(bucketPrefix + "counter");
            statsd.UpdateCount(2, bucketPrefix + "counter");
            statsd.UpdateCount(3, 0, bucketPrefix + "counter", bucketPrefix + "counter2");
            statsd.UpdateCount(4, 2, bucketPrefix + "counter", bucketPrefix + "counter2");
            statsd.UpdateCount("UpdateCount(string message, string key, int value)", 5, bucketPrefix + "counter", bucketPrefix + "counter2");
            statsd.UpdateCount("UpdateCount(string message, string key, int value)", 6, 1, bucketPrefix + "counter");
        }

        static void Main(string[] args)
        {
            TestUnderPressure();
            TestMultiThreading();
            TestMailSlotStreaming();
            ShowExample();
        }
    }
}
