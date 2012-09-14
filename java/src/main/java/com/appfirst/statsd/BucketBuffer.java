package com.appfirst.statsd;

import java.util.Hashtable;
import java.util.Map;

public class BucketBuffer {
	private Map<String, Bucket> cellar = new Hashtable<String, Bucket>();

	boolean isEmpty(){
		return this.cellar.isEmpty();
	}

	<T extends Bucket> T getBucket(String bucketname, Class<T> clazz){
		T bucket = null;
		if (cellar.containsKey(bucketname)){
			Bucket rawbucket = cellar.get(bucketname);
			if (clazz.isInstance(rawbucket)){
				bucket = (T) clazz.cast(rawbucket);
			} else {
				return null;
			}
		} else {
			try {
				bucket = clazz.newInstance();
				bucket.setName(bucketname);
			} catch (InstantiationException e) {
			} catch (IllegalAccessException e) {
			}
			cellar.put(bucketname, bucket);
		}
		return bucket;
	}

	synchronized Map<String, Bucket> dump(){
		Map<String, Bucket> dumpcellar = cellar;
		cellar = new Hashtable<String, Bucket>();
		return dumpcellar;
	}
}
