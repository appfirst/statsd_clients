using System.Collections.Generic;

namespace Statsd
{
    public interface IBucket
    {
        string Name { get; set; }
        int Stat { get; set; }

        void Infuse(int value);
    }

    public interface IStatsdClient
    {
        bool Gauge(string bucketname, int value);
        bool Timing(string bucketname, int elapse);
        bool Decrement(params string[] bucketnames);
        bool Increment(params string[] bucketnames);
        bool UpdateCount(int magnitude, double sampleRate, params string[] bucketnames);
        bool UpdateCount(int magnitude, params string[] bucketnames);
    }

    public interface IStrategicStatsd : IStatsdClient
    { 
    }

    public interface ITransport
    {
        bool Send(string message);
    }
    public delegate bool SendDelegate(string message);

    public interface IStrategy
    {
        bool Emit<T>(SendDelegate send, string bucketname, int value)
            where T : IBucket;
    }
}
