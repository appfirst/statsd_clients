using System;
using System.Linq;
using System.Net.Sockets;
using System.Text;

namespace Statsd
{
    public class StatsdPipe : IDisposable
    {
        private readonly UdpClient udpClient;
        private readonly Random random = new Random();

        public StatsdPipe(string host, int port)
        {
            udpClient = new UdpClient(host, port);
        }
        

        //Gauge Functions
        public bool Gauge(string key, int value)
        {
            return GaugeWithMessage(null, key, value);
        }

        public bool Gauge(string key, int value, double sampleRate)
        {
            return GaugeWithMessage(null, key, value, sampleRate);
        }


        //GaugeWithMessage Functions
        public bool GaugeWithMessage(string message, string key, int value)
        {
            return Gauge(key, value, 1.0);
        }

        public bool GaugeWithMessage(string message, string key, int value, double sampleRate)
        {
            return Send(message, sampleRate, String.Format("{0}:{1:d}|g", key, value));
        }


        //Timing Functions
        public bool Timing(string key, int value)
        {
            return TimingWithMessage(null, key, value);
        }

        public bool Timing(string key, int value, double sampleRate)
        {
            return TimingWithMessage(null, key, value, sampleRate);
        }

        //TimingWithMessage Functions
        public bool TimingWithMessage(string message, string key, int value)
        {
            return TimingWithMessage(message, key, value, 1.0);
        }

        public bool TimingWithMessage(string message, string key, int value, double sampleRate)
        {
            return Send(message, sampleRate, String.Format("{0}:{1:d}|ms", key, value));
        }

        // Decrement Functions
        public bool Decrement(string key)
        {
            return Increment(key, -1, 1.0);
        }

        public bool Decrement(string key, int magnitude)
        {
            return Decrement(key, magnitude, 1.0);
        }

        public bool Decrement(string key, int magnitude, double sampleRate)
        {
            magnitude = magnitude < 0 ? magnitude : -magnitude;
            return Increment(key, magnitude, sampleRate);
        }

        public bool Decrement(params string[] keys)
        {
            return Increment(-1, 1.0, keys);
        }

        public bool Decrement(int magnitude, params string[] keys)
        {
            magnitude = magnitude < 0 ? magnitude : -magnitude;
            return Increment(magnitude, 1.0, keys);
        }

        public bool Decrement(int magnitude, double sampleRate, params string[] keys)
        {
            magnitude = magnitude < 0 ? magnitude : -magnitude;
            return Increment(magnitude, sampleRate, keys);
        }

        // Decrement Functions
        public bool DecrementWithMessage(string message, string key)
        {
            return IncrementWithMessage(message, key, -1, 1.0);
        }

        public bool DecrementWithMessage(string message, string key, int magnitude)
        {
            return DecrementWithMessage(message, key, magnitude, 1.0);
        }

        public bool DecrementWithMessage(string message, string key, int magnitude, double sampleRate)
        {
            magnitude = magnitude < 0 ? magnitude : -magnitude;
            return IncrementWithMessage(message, key, magnitude, sampleRate);
        }

        public bool DecrementWithMessage(string message, params string[] keys)
        {
            return IncrementWithMessage(message, -1, 1.0, keys);
        }

        public bool DecrementWithMessage(string message, int magnitude, params string[] keys)
        {
            magnitude = magnitude < 0 ? magnitude : -magnitude;
            return IncrementWithMessage(message, magnitude, 1.0, keys);
        }

        public bool DecrementWithMessage(string message, int magnitude, double sampleRate, params string[] keys)
        {
            magnitude = magnitude < 0 ? magnitude : -magnitude;
            return IncrementWithMessage(message, magnitude, sampleRate, keys);
        }


        // Increment Functions
        public bool Increment(string key)
        {
            return IncrementWithMessage(null, key);
        }

        public bool Increment(string key, int magnitude)
        {
            return IncrementWithMessage(null, key, magnitude);
        }

        public bool Increment(string key, int magnitude, double sampleRate)
        {
            return IncrementWithMessage(null, key, magnitude, sampleRate);
        }

        public bool Increment(params string[] keys)
        {
            return IncrementWithMessage(null, keys);
        }

        public bool Increment(int magnitude, params string[] keys)
        {
            return IncrementWithMessage(null, magnitude, keys);
        }

        public bool Increment(int magnitude, double sampleRate, params string[] keys)
        {
            return IncrementWithMessage(null, magnitude, sampleRate, keys);
        }

        // IncrementWithMessage Functions
        public bool IncrementWithMessage(string message, string key)
        {
            return IncrementWithMessage(message, key, 1, 1.0);
        }

        public bool IncrementWithMessage(string message, string key, int magnitude)
        {
            return IncrementWithMessage(message, key, magnitude, 1.0);
        }

        public bool IncrementWithMessage(string message, string key, int magnitude, double sampleRate)
        {
            string stat = String.Format("{0}:{1}|c", key, magnitude);
            return Send(message, stat, sampleRate);
        }

        public bool IncrementWithMessage(string message, params string[] keys)
        {
            return IncrementWithMessage(message, 1, 1.0, keys);
        }

        public bool IncrementWithMessage(string message, int magnitude, params string[] keys)
        {
            //Made a change here to original logic!! 
            //Original: magnitude = magnitude < 0 ? magnitude : -magnitude;
            //This made the magintude always negative when this function was called

            magnitude = magnitude < 0 ? -magnitude : magnitude;
            return IncrementWithMessage(message, magnitude, 1.0, keys);
        }

        public bool IncrementWithMessage(string message, int magnitude, double sampleRate, params string[] keys)
        {
            return Send(message, sampleRate, keys.Select(key => String.Format("{0}:{1}|c", key, magnitude)).ToArray());
        }



        protected string CreateMessage(string stat,double sampleRate, string message, decimal timestamp)
        {
            //bucket: field0 | field1 | field2                  | field3
            //bucket: value  | unit   | sampele_rate/timestamp  | message

            string messageString = stat;
            string field2 = "";


            if (sampleRate < 1.0)
            {
                field2 = String.Format("@{0:f}", sampleRate);
            }
            else if (timestamp != 0)
            {
                field2 = String.Format("{0}", timestamp);
            }


            if (message != null)
            {
                messageString += String.Format("|{0}|{1}", field2, message);
            }
            else if (field2 != "")
            {
                messageString += String.Format("|{0}", field2);
            }

            return messageString;
        }
        
        protected bool Send(string message, string stat, double sampleRate)
        {
            return Send(message, sampleRate, stat);
        }

        protected bool Send(string message, double sampleRate, params string[] stats)
        {
            return Send(message, 0, sampleRate, stats);
        }

        protected bool Send(string message, decimal timestamp, double sampleRate, params string[] stats)
        {
            var retval = false; // didn't send anything


            if (sampleRate < 1.0)
            {
                foreach (var stat in stats)
                {
                    if (random.NextDouble() <= sampleRate)
                    {
                        var statFormatted = CreateMessage(stat,sampleRate,message,timestamp);
                        if (DoSend(statFormatted))
                        {
                            retval = true;
                        }
                    }
                }
            }
            else
            {
                foreach (var stat in stats)
                {
                    var statFormatted = CreateMessage(stat, sampleRate, message, timestamp);
                    if (DoSend(statFormatted))
                    {
                        retval = true;
                    }
                }
            }

            return retval;
        }
        protected bool DoSend(string stat)
        {
            var data = Encoding.Default.GetBytes(stat + "\n");

            udpClient.Send(data, data.Length);
            return true;
        }

        #region IDisposable Members

        public void Dispose()
        {
            try
            {
                if (udpClient != null)
                {
                    udpClient.Close();
                }
            }
            catch
            {
            }
        }

        #endregion
    }
}
