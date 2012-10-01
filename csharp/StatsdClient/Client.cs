using System;
using System.Collections.Generic;
using System.Timers;


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
            return Gauge(null, key, value);
        }

        public bool Gauge(string message, string key, int value)
        {
            return strategy.Emit<GaugeBucket>(this.Send, key, value, message);
        }

        #endregion

        #region Timing Functions

        //Timing Functions
        public bool Timing(string key, int value)
        {
            return Timing(null, key, value);
        }

        public bool Timing(string message, string key, int value)
        {
            return strategy.Emit<TimerBucket>(this.Send, key, value, message);
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
            return UpdateCount(null, magnitude, 1.0, keys);
        }

        public bool UpdateCount(int magnitude, double sampleRate, params string[] keys)
        {
            return UpdateCount(null, magnitude, sampleRate, keys);
        }

        public bool UpdateCount(string message, int magnitude, params string[] keys)
        {
            return UpdateCount(message, magnitude, 1.0, keys);
        }

        public bool UpdateCount(string message, int magnitude, double sampleRate, params string[] keys)
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
                bool success = this.strategy.Emit<CounterBucket>(this.Send, key, magnitude, message);
                retvalue &= success;
            }
            return retvalue;
        }
        
        #endregion
    }

    public class BufferedStrategy : IStrategy
    {
        private static BucketBuffer buffer = new BucketBuffer();
        private static Timer schedule = new Timer();
        public double Interval = 20000;
        private static SendDelegate doSend;

        static BufferedStrategy()
        {
            AppDomain domain = AppDomain.CurrentDomain;
            domain.ProcessExit += new EventHandler(Flush);

            schedule.AutoReset = false;
            schedule.Elapsed += new ElapsedEventHandler(Flush);
        }

        public BufferedStrategy(){}

        public BufferedStrategy(int interval)
        {
            this.Interval = interval;
        }

        private static void Flush(object sender, EventArgs e)
        {
            if (schedule.Enabled)
            {
                schedule.Stop();
            }
            if (!buffer.IsEmpty())
            {
                Dictionary<String, IBucket> dumpcellar = buffer.Dump();
                foreach (IBucket bucket in dumpcellar.Values)
                {
                    doSend(bucket.ToString());
                }
            }
        }

        public bool Emit<T>(SendDelegate doSend, string bucketname, int value, string message) 
            where T : IBucket
        {
            BufferedStrategy.doSend = doSend;
            buffer.Accumulate<T>(bucketname, value, message);
            
            if (!schedule.Enabled)
            {
                schedule.Interval = this.Interval;
                schedule.Start();
            }

            return true;
        }
    }

    public class InstantStrategy : IStrategy
    {
        public bool Emit<T>(SendDelegate doSend, string bucketname, int value, string message) 
            where T : IBucket
        {
            T bucket = (T) Activator.CreateInstance(typeof(T));
            bucket.Name = bucketname;
            bucket.Infuse(value, message);            
            return doSend(bucket.ToString());
        }
    }
}
