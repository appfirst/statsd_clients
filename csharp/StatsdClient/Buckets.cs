using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Runtime.CompilerServices;

namespace Statsd
{
    abstract class AbstractBucket : IBucket
    {
        protected String name;
        protected StringBuilder message = null;

        public String Name
        {
            get { return this.name; }
            set { this.name = value; }
        }

        protected String CreateMessage(int value, String unit)
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
                if (this.message == null)
                {
                    this.message = new StringBuilder();
                }
                this.message.Append("|" + message);
            }
        }

        public abstract void Infuse(int value, String message);
    }

    class CounterBucket : AbstractBucket
    {
	    private int value = 0;

	    public override String ToString(){
		    return this.CreateMessage(this.value, "c");
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
            int avg = this.sumstat / this.count;
            return this.CreateMessage(avg, "ms");
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

        public static readonly DateTime UNIX_EPOCH = new DateTime(1970, 1, 1).ToUniversalTime();

        public override String ToString()
        {
		    String stat = null;
		    int avg = this.sumstat/this.count;
            this.message.Insert(0, timestamp);
            this.CreateMessage(avg, "g");
		    return stat;
	    }

        public override void Infuse(int value, String message)
        {
		    this.sumstat += value;
            this.count++;
            this.AddMessage(message);


            this.timestamp = this.UnixTimestampNow();
	    }

        public ulong UnixTimestampNow()
        {
            return (ulong) (DateTime.UtcNow - UNIX_EPOCH).TotalSeconds;
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
                    throw new Exception();
                }
            }
            else
            {
                bucket = (T) Activator.CreateInstance(typeof(T));
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
}
