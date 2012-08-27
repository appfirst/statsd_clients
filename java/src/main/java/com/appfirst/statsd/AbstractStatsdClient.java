package com.appfirst.statsd;
import java.util.Date;
import java.util.Random;

/**
 * The Skeleton class of Java Statsd Client with AppFirst Extension.
 * <br/>
 * Note: For best results, and greater availability, you'll probably want to 
 * create a wrapper class which creates a static client and proxies to it.
 * <br/>
 * You know... the "Java way."
 * <br/>
 * Based on Statsd Client of (C) 2011 Meetup, Inc.
 * Author: Andrew Gwozdziewycz <andrew@meetup.com>, @apgwoz
 * 
 * @author Yangming Huang @leonmax
 */
public abstract class AbstractStatsdClient implements StatsdClient {
	private static Random RNG = new Random();

	/* (non-Javadoc)
	 * @see com.appfirst.statsd.IStatsdClient#gauge(java.lang.String, int)
	 */
	public boolean gauge(String bucket, int value) {
		return gauge(bucket, value, null);
	}

	/* (non-Javadoc)
	 * @see com.appfirst.statsd.IStatsdClient#gauge(java.lang.String, int, java.lang.String)
	 */
	public boolean gauge(String bucket, int value, String message) {
		String stat = buildMessage(bucket, value, "g", new Date().getTime(), message);
		return send(stat, 1);
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
	public boolean timing(String bucket, int value, String message) {
		String stat = buildMessage(bucket, value, "ms", 1, message);
		return send(stat, 1);
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
			String stat = buildMessage(buckets[i], value, "c", sampleRate, message);
			result = result && send(stat, sampleRate);
		}
		return result;
	}

	private String buildMessage(String bucket, int value, String type, double sampleRate, String message){
		String field2 = "";
		if (sampleRate < 1) {
			field2 = String.format("@%f", sampleRate);
		}
		return buildMessage(bucket, value, type, field2, message);
	}

	private String buildMessage(String bucket, int value, String type, long timestamp, String message){
		String field2 = String.valueOf(timestamp);
		return buildMessage(bucket, value, type, field2, message);
	}

	private String buildMessage(String bucket, int value, String type, String field2, String message){
		// bucket: field0 | field1 | field2                 | field3
		// bucket: value  | type   | sampele_rate/timestamp | message
		String stat = String.format("%s:%d|%s",  bucket, value, type);
		// when message is there, we always keep field2 even if it's blank:
		// bucket:2|c||some_message
		if (message != null && !message.equals("")){
			stat += String.format("|%s|%s", field2, message);
		} else if (!field2.equals("")){
			stat += String.format("|%s", field2);
		}

		return stat;
	}

	private boolean send(String stat, double sampleRate) {
		if (sampleRate < 1.0 && RNG.nextDouble() > sampleRate) 
			return false;
		else {
			return this.doSend(stat);
		}
	}
	
	/**
	 * To write a customized client, all you need is to implement this method which sends the stats message to StatsD Server thru your own media.
	 * 
	 * @param stat - the formatted message ready to send to the StatsD Server.
	 * @return True if success, False otherwise.
	 */
	protected abstract boolean doSend(final String stat);
}