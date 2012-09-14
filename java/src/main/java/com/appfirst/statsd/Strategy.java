package com.appfirst.statsd;

public interface Strategy {
	public abstract void setTask(Runnable task);
	
	public abstract void process();
}
