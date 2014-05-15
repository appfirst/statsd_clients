package com.appfirst.statsd;

/**
 * Interface for Java Statsd Client
 * 
 * @author Yangming Huang
 * @author Mike Okner (michael@appfirst.com)
 */
public interface StatsdClient {

	/**
	 * Send gauge metrics with arbitrary value.
	 * 
	 * @param bucket - The bucket name of the metrics.
	 * @param value - the reading of the gauge
	 * @return True if success, False if fail to send stats
	 */
	public abstract boolean gauge(String bucket, int value);

	/**
	 * Send timer metrics with arbitrary value.
	 * 
	 * @param bucket - The bucket name of the metrics.
	 * @param value - the duration of the timer.
	 * @return True if success, False if fail to send stats.
	 */
	public abstract boolean timing(String bucket, int value);

	/**
	 * This is a convenient method to decrement all the mentioned buckets by 1
	 * 
	 * @param buckets - The bucket name of the counter. You can send to more than one counter buckets with the same magnitude.
	 * @return True if success, False if fail to send stats.
	 */
	public abstract boolean decrement(String... buckets);

	/**
	 * This is a convenient method to increment all the mentioned buckets by 1
	 * 
	 * @param buckets - The bucket name of the counter. You can send to more than one counter buckets with the same magnitude.
	 * @return True if success, False if fail to send stats.
	 */
	public abstract boolean increment(String... buckets);

	/**
	 * Send counter metrics with arbitrary magnitude.
	 * 
	 * @param value - the updating value of the counter.
	 * @param buckets - The bucket name of the counter. You can send to more than one counter buckets with the same magnitude.
	 * @return True if success, False if fail to send stats.
	 */
	public abstract boolean updateStats(int value, String... buckets);

	/**
	 * Send counter metrics with arbitrary magnitude. This one supports sampling.
	 * 
	 * @param value - the updating value of the counter.
	 * @param sampleRate - Rate of sampling. Note this is a counter only feature.
	 * @param buckets - The bucket name of the counter. You can send to more than one counter buckets with the same magnitude.
	 * @return True if success, False if fail to send stats.
	 */
	public abstract boolean updateStats(int value, double sampleRate, String... buckets);

}