package com.appfirst.statsd;

public interface Bucket {
	public abstract String getName();
	public void setName(String name);
	public void infuse(int value, String message);
}
