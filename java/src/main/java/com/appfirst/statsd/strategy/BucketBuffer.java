package com.appfirst.statsd.strategy;

import java.util.Hashtable;
import java.util.Map;

import com.appfirst.statsd.bucket.Bucket;

public class BucketBuffer {
	private Map<String, Bucket> cellar = new Hashtable<String, Bucket>();

	synchronized boolean isEmpty(){
		return this.cellar.isEmpty();
	}

	synchronized <T extends Bucket> void deposit(
			Class<T> clazz, String bucketname, int value, String message)
					throws BucketTypeMismatchException,
						   InstantiationException,
						   IllegalAccessException{
		T bucket = null;
		if (cellar.containsKey(bucketname)){
			Bucket raw = cellar.get(bucketname);
			if (clazz.isInstance(raw)){
				bucket = (T) clazz.cast(raw);
			} else {
				String exMessage = String.format(
						"Bucket {0} was {1} but is sent as {2}",
                        raw.getName(), raw.getClass(), clazz);
				throw new BucketTypeMismatchException(exMessage);
			}
		} else {
			bucket = clazz.newInstance();
			bucket.setName(bucketname);
			cellar.put(bucketname, bucket);
		}
		bucket.infuse(value, message);
	}

	synchronized Map<String, Bucket> withdraw(){
		Map<String, Bucket> dumpcellar = cellar;
		cellar = new Hashtable<String, Bucket>();
		return dumpcellar;
	}
}