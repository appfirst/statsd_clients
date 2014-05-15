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
 * @author (maintainer) Mike Okner (michael@appfirst.com)
 */
public class DefaultService implements StatsdService {
	protected Strategy strategy = null;
	protected Transport transport = null;

	private final static Random RNG = new Random();

	/* (non-Javadoc)
	 * @see com.appfirst.statsd.StatsdService#setStrategy(com.appfirst.statsd.strategy.Strategy)
	 */
	@Override
    public StatsdService setStrategy(Strategy strategy){
		this.strategy = strategy;
		this.strategy.setTransport(this.getTransport());
		// for chaining purpose
		return this;
	}

	/* (non-Javadoc)
	 * @see com.appfirst.statsd.StatsdService#getStrategy()
	 */
	@Override
    public Strategy getStrategy(){
		if (strategy == null){
			this.setStrategy(new InstantStrategy());
		}
		return this.strategy;
	}

	/* (non-Javadoc)
	 * @see com.appfirst.statsd.IStatsdClient#gauge(java.lang.String, int, java.lang.String)
	 */
	public boolean gauge(String bucketname, int value){
		return getStrategy().send(GaugeBucket.class, bucketname, value);
	}

	/* (non-Javadoc)
	 * @see com.appfirst.statsd.IStatsdClient#timing(java.lang.String, int, java.lang.String)
	 */
	public boolean timing(String bucketname, int value){
		return getStrategy().send(TimerBucket.class, bucketname, value);
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
		return updateStats(1, 1, buckets);
	}

	/* (non-Javadoc)
	 * @see com.appfirst.statsd.IStatsdClient#updateStats(int, java.lang.String)
	 */
	public boolean updateStats(int value, String... buckets) {
		return updateStats(value, 1, buckets);
	}

	/* (non-Javadoc)
	 * @see com.appfirst.statsd.IStatsdClient#updateStats(int, java.lang.String, double, java.lang.String)
	 * Updates multiple buckets
	 */
	public boolean updateStats(int value, double sampleRate, String... buckets) {
		boolean result = true;
		for (int i = 0; i < buckets.length; i++) {
			result = result && this.updateStats(buckets[i], value, sampleRate);
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see com.appfirst.statsd.IStatsdClient#updateStats(java.lang.String, int, double, java.lang.String)
	 * Updates a single bucket
	 */
	public boolean updateStats(String bucketname, int value, double sampleRate){
		if (sampleRate < 1.0 && RNG.nextDouble() > sampleRate) 
			return true;
		value /= sampleRate;
		return getStrategy().send(CounterBucket.class, bucketname, value);
	}
	
	@Override
    public StatsdService setTransport(Transport transport){
		this.transport = transport;
		// for chaining purpose
		return this;
	}

	/* (non-Javadoc)
	 * @see com.appfirst.statsd.StatsdService#getTransport()
	 */
	@Override
    public Transport getTransport(){
		if (transport == null){
			try {
				this.transport = new UdpTransport();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return this.transport;
	}
}