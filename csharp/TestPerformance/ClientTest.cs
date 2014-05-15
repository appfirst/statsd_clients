using System;
using System.Diagnostics;
using System.Text;
using System.Threading;
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

        private IStatsdClient statsd;

        public ThreadPoolStatsd(ManualResetEvent doneEvent, string bucketPrefix, IStatsdClient statsd)
        {
            this.doneEvent = doneEvent;
            this.bucketPrefix = bucketPrefix;
            this.statsd = statsd;
        }

        public void SendMessages(Object threadContext)
        {
            int threadIndex = (int)threadContext;
            for (int i = 0; i < 10000000; i++)
            {
                //Thread.Sleep(r.Next(5));
                DateTime start = DateTime.Now;
                statsd.Increment(bucketPrefix + "threadpool.thread");
//                statsd.Increment(bucketPrefix + "threadpool.thread" + threadIndex);
                int elapsedTimeMillis = Convert.ToInt32((DateTime.Now - start).TotalMilliseconds);
                statsd.Timing(bucketPrefix + "threadpool.incr_time", elapsedTimeMillis);
            }
            doneEvent.Set();
        }
    }

    class Program
    {
        const string bucketPrefix = "test.csharp.";

        static Random r = new Random();

        static void TestUnderPressure()
        {
            Console.WriteLine("TestUnderPressure");
            StatsdPipe statsd = new StatsdPipe();
            statsd.Transport = new TransportMock();
            statsd.Strategy = new BufferedStrategy(500);

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
			    if (i % 3 == 0) {
                    statsd.Increment(bucketPrefix + "pressure.multiple3");
			    }
		    }
	    }

        static void TestMultiThreading2()
        {
            Console.WriteLine("TestMultiThreading (CB)");
            DateTime start = DateTime.Now;
            ManualResetEvent[] doneEvents = new ManualResetEvent[10];
            StatsdPipe statsd = new StatsdPipe();
            //statsd.Transport = new TransportMock();
            statsd.Strategy = new BufferedStrategy(2000);
            for (int i = 0; i < doneEvents.Length; i++)
            {
                doneEvents[i] = new ManualResetEvent(false);
                ThreadPoolStatsd tps = new ThreadPoolStatsd(doneEvents[i], bucketPrefix, statsd);
                ThreadPool.QueueUserWorkItem(tps.SendMessages, i);
            }

            WaitHandle.WaitAll(doneEvents);
            int elapsedTimeMillis = Convert.ToInt32((DateTime.Now - start).TotalMilliseconds);
            Console.WriteLine("Test took: " + elapsedTimeMillis + " msec");
        }

        static void TestMultiThreading()
        {
            Console.WriteLine("TestMultiThreading");
            ManualResetEvent[] doneEvents = new ManualResetEvent[10];
            for (int i = 0; i < doneEvents.Length; i++)
            {
                StatsdPipe statsd = new StatsdPipe();
                statsd.Transport = new TransportMock();
                statsd.Strategy = new BufferedStrategy(500);
                doneEvents[i] = new ManualResetEvent(false);
                ThreadPoolStatsd tps = new ThreadPoolStatsd(doneEvents[i], bucketPrefix, statsd);
                ThreadPool.QueueUserWorkItem(tps.SendMessages, i);
            }

            WaitHandle.WaitAll(doneEvents);
        }

        static void ShowExample(IStatsdClient statsd)
        {
            statsd.Gauge(bucketPrefix + "gauge", 500);
            statsd.Timing(bucketPrefix + "timer", 500);
            statsd.Increment(bucketPrefix + "counter");
            statsd.Decrement(bucketPrefix + "counter");
            statsd.UpdateCount(2, bucketPrefix + "counter");
            statsd.UpdateCount(4, 0, bucketPrefix + "counter", bucketPrefix + "counter2");
            statsd.UpdateCount(8, 2, bucketPrefix + "counter", bucketPrefix + "counter2");
        }

        static void SendingWithGeyserStrategy()
        {
            Console.WriteLine("SendingWithGeyserStrategy");
            StatsdPipe statsd = new StatsdPipe();
            statsd.Strategy = new BufferedStrategy(5000);
            while (true)
            {
                Thread.Sleep(1);
                statsd.Increment(bucketPrefix + "mailslot");
            }
        }

        static void SendMany()
        {
            Console.WriteLine("SendMany");
            DateTime start = DateTime.Now;
            StatsdPipe statsd = new StatsdPipe();
            statsd.Strategy = new BufferedStrategy(5000);
            for (int i = 0; i < 100000000; i++ )
            {
                //Thread.Sleep(1);
                statsd.Increment(bucketPrefix + "mailslot");
            }
            int elapsedTimeMillis = Convert.ToInt32((DateTime.Now - start).TotalMilliseconds);
            Console.WriteLine("Test took: " + elapsedTimeMillis + " msec");
        }

        static void SendingWithoutGeyserStrategy()
        {
            Console.WriteLine("SendingWithoutGeyserStrategy");
            StatsdPipe statsd = new StatsdPipe();
            int i = 0;
            StringBuilder sb = new StringBuilder();
            sb.Append("a");
            for (i = 0; i < 10; i++)
            {
                sb.Append(sb.ToString());
            }
            Console.WriteLine(sb.Length);
            statsd.UpdateCount(1, 'a');
            statsd.Strategy = new BufferedStrategy(5000);
            while (true)
            {
                Thread.Sleep(1);
                statsd.Increment(bucketPrefix + "mailslot");
            }
            
        }
 
        static void TestBasic()
        {
            Console.WriteLine("SendingWithoutGeyserStrategy");
            StatsdPipe statsd = new StatsdPipe();
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
            //TestBasic();
            //SendMany();
            //TestUnderPressure();
            //TestMultiThreading();
            TestMultiThreading2();
            //ShowExample(new StatsdPipe());
            //SendingWithGeyserStrategy();
            //SendingWithoutGeyserStrategy();
            //MailSlotTest.TestMailSlot();
        }
    }
}
