using System;
using System.Collections.Generic;
using System.Threading;


namespace Statsd
{
    public class StatsdPipe : IStatsdClient
    {
        private static readonly Random RAND = new Random();

        private ITransport transport = new AFTransport();

        public ITransport Transport
        {
            set
            {
                this.transport = value;
            }
        }

        private SendDelegate Send
        {
            get { return new SendDelegate(this.transport.Send); }
        }

        private IStrategy strategy = new InstantStrategy();

        public IStrategy Strategy
        {
            set
            {
                if (value != null)
                {
                    this.strategy = value;
                }
            }
        }
        
        #region Gauge Functions

        //Gauge Functions
        public bool Gauge(string key, int value)
        {
            return strategy.Emit<GaugeBucket>(this.Send, key, value);
        }

        #endregion

        #region Timing Functions

        public bool Timing(string key, int value)
        {
            return strategy.Emit<TimerBucket>(this.Send, key, value);
        }

        #endregion

        #region Counter Functions

        public bool Decrement(params string[] keys)
        {
            return UpdateCount(-1, keys);
        }

        public bool Increment(params string[] keys)
        {
            return UpdateCount(1, keys);
        }

        public bool UpdateCount(int magnitude, params string[] keys)
        {
            return UpdateCount(magnitude, 1.0, keys);
        }

        public bool UpdateCount(int magnitude, double sampleRate, params string[] keys)
        {
            if (sampleRate < 1)
            {
                if (RAND.NextDouble() > sampleRate)
                {
                    return true;
                }
                else
                {
                    magnitude = Convert.ToInt32(magnitude/sampleRate);
                }
            }
            bool retvalue = true;
            foreach (string key in keys)
            {
                bool success = this.strategy.Emit<CounterBucket>(this.Send, key, magnitude);
                retvalue &= success;
            }
            return retvalue;
        }
        
        #endregion
    }


    // To avoid obtaiing a lock on every add, we use two sets of buffers.  One only 
    // used for reads, the other only used for writes.  Periodically, we swap them.
    // If multi-threaded, each thread will have it's own pair.  
    // Details in BucketBuffer, in Buckets.cs
    public class BufferedStrategy : IStrategy
    {
        private static BucketBuffer buffer = new BucketBuffer();
        public static int Interval = 20000;
        private static SendDelegate doSend;
        private static Thread bg_thread = new Thread(background);

        static BufferedStrategy()
        {
            AppDomain domain = AppDomain.CurrentDomain;
            domain.ProcessExit += new EventHandler(FlushFlushDone);
        }

        public BufferedStrategy()
        {
            this.start();
        }

        public BufferedStrategy(int interval)
        {
            Interval = interval;
            this.start();
        }

        private static void background()
        {
            while (true)
            {
                Thread.Sleep(Interval / 2);
                buffer.SwapBuffers();
                Thread.Sleep(Interval / 2);
                Flush();
            }
        }

        private void start()
        {
            bg_thread.IsBackground = true;
            bg_thread.Start();
        }

        private static void stop()
        {
            bg_thread.Abort();
        }

        public static void Flush()
        {
            Dictionary<String, IBucket> dumpcellar = buffer.Dump();
            foreach (IBucket bucket in dumpcellar.Values)
            {
                doSend(bucket.ToString());
            }
        }

        private static void FlushFlushDone(object sender, EventArgs e)
        {
            Flush();
            buffer.SwapBuffers();
            Flush();
            //stop();
        }

        public bool Emit<T>(SendDelegate doSend, string bucketname, int value) 
            where T : IBucket
        {
            BufferedStrategy.doSend = doSend;
            buffer.Accumulate<T>(bucketname, value);
            

            return true;
        }
    }

    public class InstantStrategy : IStrategy
    {
        public bool Emit<T>(SendDelegate doSend, string bucketname, int value) 
            where T : IBucket
        {
            T bucket = (T) Activator.CreateInstance(typeof(T));
            bucket.Name = bucketname;
            bucket.Infuse(value);            
            return doSend(bucket.ToString());
        }
    }
}
