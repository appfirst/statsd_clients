package com.appfirst.statsd.strategy;

import com.appfirst.statsd.Transport;
import com.appfirst.statsd.bucket.Bucket;


public interface Strategy {
	public void setTransport(Transport transport);
	public abstract <T extends Bucket> boolean emit(
			Class<T> clazz, String bucketname, int value, String message);
}
