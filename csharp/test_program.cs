using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Net.Sockets;


namespace Statsd
{
    class Program
    {
        static void Main(string[] args)
        {
            StatsdPipe statsd = new StatsdPipe("127.0.0.1", 8126);

            string bucketPrefix = "csharp.test.";


            // These are examples of every function the API offers. 
            statsd.Gauge(bucketPrefix + "Gauge", 500);
            statsd.Gauge(bucketPrefix + "Gauge", 500, 2);
            statsd.GaugeWithMessage("GaugeWithMessage(string message, string key, int value)",bucketPrefix + "GaugeWithMessage", 500);
            statsd.GaugeWithMessage("GaugeWithMessage(string message, string key, int value, double samplerate)", bucketPrefix + "GaugeWithMessage", 500, 2);

            statsd.Timing(bucketPrefix + "Timing", 1);
            statsd.Timing(bucketPrefix + "Timing", 1, 2);
            statsd.TimingWithMessage("Timing(string key, int value)", bucketPrefix + "Timing", 1);
            statsd.TimingWithMessage("Timing(string key, int value)", bucketPrefix + "Timing", 2);

            statsd.Decrement(bucketPrefix + "Decrement");
            statsd.Decrement(bucketPrefix + "Decrement", 1);
            statsd.Decrement(bucketPrefix + "Decrement", 1, 2);
            statsd.Decrement(bucketPrefix + "Dec1", bucketPrefix + "Dec2", bucketPrefix + "Dec3");
            statsd.Decrement(1, bucketPrefix + "Dec1", bucketPrefix + "Dec2", bucketPrefix + "Dec3");
            statsd.Decrement(1, 2, bucketPrefix + "Dec1", bucketPrefix + "Dec2", bucketPrefix + "Dec3");

            statsd.DecrementWithMessage("DecrementWithMessage(string message, string key)", bucketPrefix + "DecrementWithMessage");
            statsd.DecrementWithMessage("DecrementWithMessage(string message, string key, int magnitude)", bucketPrefix + "DecrementWithMessage", 1);
            statsd.DecrementWithMessage("DecrementWithMessage(string message, string key, int magnitude, double sampleRate)", bucketPrefix + "DecrementWithMessage", 1, 2);
            statsd.DecrementWithMessage("DecrementWithMessage(string message, params string[] keys)", bucketPrefix + "Dec1", bucketPrefix + "Dec2", bucketPrefix + "Dec3");
            statsd.DecrementWithMessage("DecrementWithMessage(string message, int magnitude, params string[] keys)", 1, bucketPrefix + "Dec1", bucketPrefix + "Dec2", bucketPrefix + "Dec3");
            statsd.DecrementWithMessage("DecrementWithMessage(string message, int magnitude, double sampleRate, params string[] keys)", 1, 2, bucketPrefix + "Dec1", bucketPrefix + "Dec2", bucketPrefix + "Dec3");

            statsd.Increment(bucketPrefix + "Increment");
            statsd.Increment(bucketPrefix + "Increment", 1);
            statsd.Increment(bucketPrefix + "Increment", 1, 2);
            statsd.Increment(bucketPrefix + "Inc1", bucketPrefix + "Inc2", bucketPrefix + "Inc3");
            statsd.Increment(1, bucketPrefix + "Inc1", bucketPrefix + "Inc2", bucketPrefix + "Inc3");
            statsd.Increment(1, 2, bucketPrefix + "Inc1", bucketPrefix + "Inc2", bucketPrefix + "Inc3");

            statsd.IncrementWithMessage("IncrementWithMessage(string message, string key)", bucketPrefix + "IncrementWithMessage");
            statsd.IncrementWithMessage("IncrementWithMessage(string message, string key, int magnitude)", bucketPrefix + "IncrementWithMessage", 1);
            statsd.IncrementWithMessage("IncrementWithMessage(string message, string key, int magnitude, double sampleRate)", bucketPrefix + "IncrementWithMessage", 1, 2);
            statsd.IncrementWithMessage("IncrementWithMessage(string message, params string[] keys)", bucketPrefix + "Inc1", bucketPrefix + "Inc2", bucketPrefix + "Inc3");
            statsd.IncrementWithMessage("IncrementWithMessage(string message, int magnitude, params string[] keys)", 1, bucketPrefix + "Inc1", bucketPrefix + "Inc2", bucketPrefix + "Inc3");
            statsd.IncrementWithMessage("IncrementWithMessage(string message, int magnitude, double sampleRate, params string[] keys)", 1, 2, bucketPrefix + "Inc1", bucketPrefix + "Inc2", bucketPrefix + "Inc3");







            // These are some failing cases. 

            string bucketnameWith65Chars = bucketPrefix+"65CharacterBucketName";

            while (bucketnameWith65Chars.Length < 65)
                bucketnameWith65Chars += "A";

            statsd.IncrementWithMessage("This bucket should not be accepted, because it is too long!", bucketnameWith65Chars);




            string bucketnameWithSpace = bucketPrefix + "Spaced Name";

            statsd.IncrementWithMessage("This bucket has a space it its name.", bucketnameWithSpace);


            string bucketnameWithPipe = bucketPrefix + "Piped|Name";

            statsd.IncrementWithMessage("This bucket has a pipe in its name.", bucketnameWithPipe);


            string bucketnameWithColon = bucketPrefix + "Colon:Name";

            statsd.IncrementWithMessage("This bucket has a colon in its name.", bucketnameWithColon);


            string bucketnameWithUnicodeSymbols = bucketPrefix + "Unicode√∭∑⍥汉字/漢字Name";

            statsd.IncrementWithMessage("This bucket has unicode characters in its name.", bucketnameWithUnicodeSymbols);

            statsd.IncrementWithMessage("This bucket ends in a '.' ", bucketPrefix);

            statsd.IncrementWithMessage("This bucket should be perfectly acceptable, but comes after some unacceptable names.", bucketPrefix + "acceptableName");


        }
    }
}
