package com.appfirst.statsd;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Random;

import com.appfirst.statsd.bucket.CounterBucket;
import com.appfirst.statsd.bucket.GaugeBucket;
import com.appfirst.statsd.bucket.TimerBucket;
import com.appfirst.statsd.strategy.InstantStrategy;
import com.appfirst.statsd.strategy.Strategy;
import com.appfirst.statsd.transport.Transport;
import com.appfirst.statsd.transport.UdpTransport;


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
public class DefaultStatsdClient implements StatsdClient {
	private Strategy strategy = null;
	private Transport transport = null;

	private Class<? extends Strategy> defaultStrategyClass = InstantStrategy.class;
	private Class<? extends Transport> defaultTransportClass = UdpTransport.class;

	private final static Random RNG = new Random();

	public StatsdClient setStrategy(Strategy strategy){
		this.strategy = strategy;
		this.strategy.setTransport(this.getTransport());
		// for chaining purpose
		return this;
	}

	public Strategy getStrategy(){
		if (strategy == null){
			try {
				this.strategy = defaultStrategyClass.newInstance();
			} catch (InstantiationException e) {
				this.strategy = new InstantStrategy();
			} catch (IllegalAccessException e) {
				this.strategy = new InstantStrategy();
			}
			this.strategy.setTransport(this.getTransport());
		}
		return this.strategy;
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
	public boolean gauge(String bucketname, int value, String message){
		return strategy.send(GaugeBucket.class, bucketname, value, message);
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
	public boolean timing(String bucketname, int value, String message){
		return strategy.send(TimerBucket.class, bucketname, value, message);
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
	public boolean updateStats(String bucketname, int value, double sampleRate, String message){
		if (sampleRate < 1.0 && RNG.nextDouble() > sampleRate) 
			return true;
		value /= sampleRate;
		return strategy.send(CounterBucket.class, bucketname, value, message);
	}

	protected Transport getTransport(){
		if (transport == null){
			try {
				this.transport = defaultTransportClass.newInstance();
			} catch (InstantiationException e) {
			} catch (IllegalAccessException e) {
			}
			if (this.transport == null) {
				try {
					this.transport = new UdpTransport();
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			this.strategy.setTransport(this.transport);
		}
		return this.transport;
	}
}