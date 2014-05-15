package com.appfirst.statsd.strategy;

import com.appfirst.statsd.bucket.Bucket;
import com.appfirst.statsd.transport.Transport;


public interface Strategy {
	void setTransport(Transport transport);
	<T extends Bucket> boolean send(Class<T> clazz, String bucketname, int value, String message);
}
