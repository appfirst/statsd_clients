using System;
namespace Statsd
{
    public interface IBucket
    {
        string Name { get; set; }

        void Infuse(int value, string message);
    }

    public interface IStatsdClient
    {
        bool Gauge(string bucketname, int value);
        bool Gauge(string message, string bucketname, int value);
        bool Timing(string bucketname, int elapse);
        bool Timing(string message, string bucketname, int elapse);
        bool Decrement(params string[] bucketnames);
        bool Increment(params string[] bucketnames);
        bool UpdateCount(int magnitude, double sampleRate, params string[] bucketnames);
        bool UpdateCount(int magnitude, params string[] bucketnames);
        bool UpdateCount(string message, int magnitude, double sampleRate, params string[] bucketnames);
        bool UpdateCount(string message, int magnitude, params string[] bucketnames);
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
        bool Emit<T>(SendDelegate send, string bucketname, int value, string message)
            where T : IBucket;
    }
}
