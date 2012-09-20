using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Runtime.CompilerServices;

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

    abstract class AbstractBucket : IBucket
    {
        protected String name;
        protected StringBuilder message = new StringBuilder();

        public String Name
        {
            get { return this.name; }
            set { this.name = value; }
        }

        protected String GetStatsdString(int value, String unit)
        {
            String stat = null;
            if (message != null && message.Length>0)
            {
                stat = String.Format("{0}:{1:d}|{2}|{3}", name, value, unit, message.ToString());
            }
            else
            {
                stat = String.Format("{0}:{1:d}|{2}", name, value, unit);
            }
            return stat;
        }

        protected void AddMessage(String message)
        {
            if (message != null && !message.Equals(""))
            {
                this.message.Append("|" + message);
            }
        }

        public abstract void Infuse(int value, String message);
    }

    class CounterBucket : AbstractBucket
    {
	    private int value = 0;

	    public override String ToString(){
		    return this.GetStatsdString(this.value, "c");
	    }

        public override void Infuse(int value, String message)
        {
            this.value += value;
            this.AddMessage(message);
        }
    }

    class TimerBucket : AbstractBucket
    {
	    private int sumstat = 0;
	    private int count = 0;

        public override String ToString()
        {
            int avg = Convert.ToInt32(this.sumstat / this.count);
            return this.GetStatsdString(avg, "ms");
	    }

        public override void Infuse(int value, String message)
        {
		    this.sumstat += value;
            this.count++;
            this.AddMessage(message);
	    }
    }

    class GaugeBucket : AbstractBucket
    {
	    private int sumstat = 0;
	    private int count = 0;
	    private ulong timestamp;

        public override String ToString()
        {
            int avg = Convert.ToInt32(this.sumstat / this.count);
            this.message.Insert(0, timestamp);
            return this.GetStatsdString(avg, "g");
	    }

        public override void Infuse(int value, String message)
        {
		    this.sumstat += value;
            this.count++;
            this.AddMessage(message);


            this.timestamp = TimestampHelper.Now;
	    }
    }

    class BucketBuffer {
	    private Dictionary<String, IBucket> cellar = new Dictionary<String, IBucket>();

	    public bool IsEmpty(){
		    return this.cellar.Count == 0;
	    }


        [MethodImpl(MethodImplOptions.Synchronized)]
        public void Accumulate<T>(String bucketname, int value, string message)
            where T : IBucket
        {
            Type buckettype = typeof(T);
            T bucket = default(T);
            if (cellar.ContainsKey(bucketname))
            {
                IBucket rawbucket;
                cellar.TryGetValue(bucketname, out rawbucket);
                if (rawbucket is T)
                {

                    bucket = (T)rawbucket;
                }
                else
                {
                    string exMessage = String.Format(
                        "{0} for name {1} is not matching {2} which was sent before",
                        buckettype,
                        rawbucket.Name,
                        rawbucket.GetType());
                    throw new BucketTypeMismatchException(exMessage);
                }
            }
            else
            {
                bucket = (T)Activator.CreateInstance(buckettype);
                bucket.Name = bucketname;
                cellar.Add(bucketname, bucket);
            }
            bucket.Infuse(value, message);
	    }

        [MethodImpl(MethodImplOptions.Synchronized)]
        public Dictionary<String, IBucket> Dump()
        {
            Dictionary<String, IBucket> dumpcellar = cellar;
            cellar = new Dictionary<String, IBucket>();
		    return dumpcellar;
	    }
    }

    public class BucketTypeMismatchException : Exception
    {
        public BucketTypeMismatchException()
        {
        }

        public BucketTypeMismatchException(string message)
            : base(message)
        {
        }
    }
}
