package com.appfirst.statsd.strategy;

import com.appfirst.statsd.bucket.Bucket;
import com.appfirst.statsd.transport.Transport;



public class InstantStrategy implements Strategy {
	private Transport transport;

	public void setTransport(Transport transport){
		this.transport = transport;
	}

	public <T extends Bucket> boolean send(Class<T> clazz, String bucketname, int value) {
		try {
			T bucket = clazz.newInstance();
			bucket.setName(bucketname);
			bucket.infuse(value);
			transport.doSend(bucket.toString());
		} catch (InstantiationException e) {
			e.printStackTrace();
			return false;
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
}