package com.appfirst.statsd;
import java.util.Map;

/**
 * The Skeleton class of Java Statsd Client with AppFirst Extension.
 * <br/>
 * Note: For best results, and greater availability, you'll probably want to 
 * create a wrapper class which creates a static client and proxies to it.
 * <br/>
 * You know... the "Java way."
 * <br/>
 * Based on Statsd Client of (C) 2011 Meetup, Inc.
 * by Andrew Gwozdziewycz <andrew@meetup.com>, @apgwoz
 * 
 * @author Yangming Huang @leonmax
 */
public abstract class AbstractStatsdClient implements StatsdClient, Runnable {
	private Strategy strategy = new InstantStrategy();

	public void setStrategy(Strategy strategy){
		this.strategy = strategy;
		this.strategy.setTask(this);
	}
	
	private BucketBuffer buffer = new BucketBuffer();

	public void run(){
		if (!this.buffer.isEmpty()){
			Map<String, Bucket> dumpcellar = this.buffer.dump();
			for (Bucket bucket : dumpcellar.values()){
				this.doSend(bucket.toString());
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.appfirst.statsd.IStatsdClient#gauge(java.lang.String, int)
	 */
	public boolean gauge(String bucket, int value) {
		return gauge(bucket, value, null);
	}

	/* (non-Javadoc)
	 * @see com.appfirst.statsd.IStatsdClient#gauge(java.lang.String, int, java.lang.String)
	 */
	public synchronized boolean gauge(String bucketname, int value, String message){
		GaugeBucket bucket = this.buffer.getBucket(bucketname, GaugeBucket.class);
		bucket.infuse(value, message);
		strategy.process();
		return true;
	}

	/* (non-Javadoc)
	 * @see com.appfirst.statsd.IStatsdClient#timing(java.lang.String, int)
	 */
	public boolean timing(String bucket, int value) {
		return timing(bucket, value, null);
	}

	/* (non-Javadoc)
	 * @see com.appfirst.statsd.IStatsdClient#timing(java.lang.String, int, java.lang.String)
	 */
	public synchronized boolean timing(String bucketname, int value, String message){
		TimerBucket bucket = this.buffer.getBucket(bucketname, TimerBucket.class);
		bucket.infuse(value, message);
		strategy.process();
		return true;
	}

	/* (non-Javadoc)
	 * @see com.appfirst.statsd.IStatsdClient#decrement(java.lang.String)
	 */
	public boolean decrement(String... buckets) {
		return updateStats(-1, null, 1, buckets);
	}

	/* (non-Javadoc)
	 * @see com.appfirst.statsd.IStatsdClient#increment(java.lang.String)
	 */
	public boolean increment(String... buckets) {
		return updateStats(1, null, 1, buckets);
	}

	/* (non-Javadoc)
	 * @see com.appfirst.statsd.IStatsdClient#updateStats(int, java.lang.String)
	 */
	public boolean updateStats(int value, String... buckets){
		return updateStats(value, null, 1, buckets);
	}
	
	/* (non-Javadoc)
	 * @see com.appfirst.statsd.IStatsdClient#updateStats(int, double, java.lang.String)
	 */
	public boolean updateStats(int value, double sampleRate, String... buckets){
		return updateStats(value, null, sampleRate, buckets);
	}

	/* (non-Javadoc)
	 * @see com.appfirst.statsd.IStatsdClient#updateStats(int, java.lang.String, double, java.lang.String)
	 */
	public boolean updateStats(int value, String message, double sampleRate, String... buckets){
		boolean result = true;
		for (int i = 0; i < buckets.length; i++) {
			result = result && this.updateStats(buckets[i], value, sampleRate, message);
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see com.appfirst.statsd.IStatsdClient#updateStats(java.lang.String, int, double, java.lang.String)
	 */
	public synchronized boolean updateStats(String bucketname, int value, double sampleRate, String message){
		CounterBucket bucket = this.buffer.getBucket(bucketname, CounterBucket.class);
		bucket.infuse(value, sampleRate, message);
		strategy.process();
		return true;
	}

	/**
	 * To write a customized client, all you need is to implement this method which sends the stats
	 *  message to StatsD Server thru your own media.
	 * 
	 * @param stat - the formatted message ready to send to the StatsD Server.
	 * @return True if success, False otherwise.
	 */
	protected abstract boolean doSend(final String stat);
}