package com.appfirst.statsd.strategy;

import java.util.Hashtable;
import java.util.Map;

import com.appfirst.statsd.bucket.Bucket;


class BucketBuffer {
	private Map<String, Bucket> cellar = new Hashtable<String, Bucket>();

	synchronized boolean isEmpty(){
		return this.cellar.isEmpty();
	}

	synchronized <T extends Bucket> void brew(Class<T> clazz, String bucketname,
			int value,
			String message){
		T bucket = null;
		if (cellar.containsKey(bucketname)){
			Bucket rawbucket = cellar.get(bucketname);
			if (clazz.isInstance(rawbucket)){
				bucket = (T) clazz.cast(rawbucket);
			} else {
				throw new RuntimeException("wrong type of bucket");
			}
		} else {
			try {
				bucket = clazz.newInstance();
				bucket.setName(bucketname);
				cellar.put(bucketname, bucket);
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		bucket.infuse(value, message);
	}

	synchronized Map<String, Bucket> dump(){
		Map<String, Bucket> dumpcellar = cellar;
		cellar = new Hashtable<String, Bucket>();
		return dumpcellar;
	}
}