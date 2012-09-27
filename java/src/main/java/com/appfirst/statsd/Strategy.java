package com.appfirst.statsd;


public interface Strategy {
	public void setTransport(Transport transport);
	public abstract <T extends Bucket> boolean emit(
			Class<T> clazz, String bucketname, int value, String message);
}
