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
        bool Gauge(string key, int value);
        bool Gauge(string message, string key, int value);
        bool Timing(string key, int value);
        bool Timing(string message, string key, int value);
        bool Decrement(params string[] keys);
        bool Increment(params string[] keys);
        bool UpdateCount(int magnitude, double sampleRate, params string[] keys);
        bool UpdateCount(int magnitude, params string[] keys);
        bool UpdateCount(string message, int magnitude, double sampleRate, params string[] keys);
        bool UpdateCount(string message, int magnitude, params string[] keys);
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
