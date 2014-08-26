using System;
using System.Collections.Generic;
using System.Runtime.CompilerServices;
using System.Text;
using System.Threading;

namespace Statsd
{
    public static class TimestampHelper
    {
        public static readonly DateTime UNIX_EPOCH = new DateTime(1970, 1, 1);

        public static ulong Now
        {
            get
            {
                return Timestamp(DateTime.UtcNow);
            }
        }

        public static ulong Timestamp(DateTime dt)
        {
            return (ulong)(dt - UNIX_EPOCH).TotalSeconds;
        }

        public static DateTime ConvertToDateTime(ulong timestamp)
        {
            return UNIX_EPOCH.AddSeconds(timestamp);
        }
    }

    public abstract class AbstractBucket : IBucket
    {
        private String name;
        private int stat;
        private String field2 = null;

        public String Field2
        {
            set { this.field2 = value; }
        }

        public String Name
        {
            get { return this.name; }
            set { this.name = value; }
        }

        public Int32 Stat
        {
            get { return this.stat; }
            set { this.stat = value; }
        }

        protected String GetStatsdString(int value, String unit)
        {
            StringBuilder output = new StringBuilder(String.Format("{0}:{1:d}|{2}", name, value, unit));
            return output.ToString();
        }

        public abstract void Infuse(int value);
    }

    public class CounterBucket : AbstractBucket
    {

	    public override String ToString()
        {
		    return this.GetStatsdString(this.Stat, "c");
	    }

        public override void Infuse(int value)
        {
            this.Stat += value;
        }

    }

    public class TimerBucket : AbstractBucket
    {
	    private int count = 0;

        public override String ToString()
        {
            int avg = Convert.ToInt32(this.Stat / this.count);
            return this.GetStatsdString(avg, "ms");
	    }

        public override void Infuse(int value)
        {
		    this.Stat += value;
            this.count++;
	    }

     }

    public class GaugeBucket : AbstractBucket
    {
	    private ulong timestamp;

        public override String ToString()
        {
            this.Field2 = Convert.ToString(timestamp);
            return this.GetStatsdString(Stat, "g");
	    }

        public override void Infuse(int value)
        {
		    this.Stat = value;
            this.timestamp = TimestampHelper.Now;
	    }

     }



    // To avoid a mutex lock on every add, we use a pair of buffers, one for reading,
    // the other for writing.  They get swapped midway though the send interval, and 
    // dumped at the end of the send interval.  If we are multi-threaded, a pair of 
    // buffers will be created for each thread.
    public class BucketBuffer
    {

        static Dictionary<int, Dictionary<String, IBucket>> left_buffers = new Dictionary<int, Dictionary<String, IBucket>>();
        static Dictionary<int, Dictionary<String, IBucket>> right_buffers = new Dictionary<int, Dictionary<String, IBucket>>();

        Dictionary<int, Dictionary<String, IBucket>> rbufs = left_buffers;
        Dictionary<int, Dictionary<String, IBucket>> wbufs = right_buffers;

        static Mutex mutex = new Mutex();

        public void SwapBuffers()
        {
            if (wbufs == right_buffers)
            {
                wbufs = left_buffers;
                rbufs = right_buffers;

            }
            else
            {
                wbufs = right_buffers;
                rbufs = left_buffers;
            }
        }


        //[MethodImpl(MethodImplOptions.Synchronized)]
        public void Accumulate<T>(String bucketname, int value)
            where T : IBucket
        {
            Type buckettype = typeof(T);
            T bucket = default(T);
            Dictionary<String, IBucket> write_buffer;

            wbufs.TryGetValue(Thread.CurrentThread.ManagedThreadId, out write_buffer);
            if (write_buffer == null)
            {
                mutex.WaitOne();  // accquire a lock before we mess with the list of buffers
                write_buffer = new Dictionary<String, IBucket>();
                wbufs[Thread.CurrentThread.ManagedThreadId] = write_buffer;
                mutex.ReleaseMutex();
            }
            
            IBucket rawbucket;
            write_buffer.TryGetValue(bucketname, out rawbucket);
            if (rawbucket != null)
            {
                if (rawbucket is T)
                {
                    bucket = (T)rawbucket;
                }
                else
                {
                    string exMessage = String.Format("{0} for name {1} is not matching {2} which was sent before",
                                                     buckettype, rawbucket.Name, rawbucket.GetType());
                    throw new BucketTypeMismatchException(exMessage);
                }
            }
            else
            {
                bucket = (T)Activator.CreateInstance(buckettype);
                bucket.Name = bucketname;
                write_buffer.Add(bucketname, bucket);
            }
            bucket.Infuse(value);
	    }

        //[MethodImpl(MethodImplOptions.Synchronized)]
        public Dictionary<String, IBucket> Dump()
        {
            Dictionary<String, IBucket> dump_buffer = new Dictionary<String, IBucket>();
          
            foreach(Dictionary<String, IBucket> read_buffer in rbufs.Values)
            {
                foreach (IBucket Bucket in read_buffer.Values)
                {
                    IBucket dump_bucket;
                    dump_buffer.TryGetValue(Bucket.Name, out dump_bucket);
                    if (dump_bucket == null)
                    {
                        dump_buffer.Add(Bucket.Name, Bucket);
                    }
                    else
                    {
                        dump_bucket.Infuse(Bucket.Stat);
                    }
                }
                read_buffer.Clear();
            }
            return dump_buffer;
	    }
    }

    public class BucketTypeMismatchException : Exception
    {
        public BucketTypeMismatchException(){}
        public BucketTypeMismatchException(string message)
            : base(message){}
    }
}
