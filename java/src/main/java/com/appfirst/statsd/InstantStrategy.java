package com.appfirst.statsd;

public class InstantStrategy implements Strategy {
	private Runnable task;

	@Override
	public void setTask(Runnable task){
		this.task = task;
	}

	@Override
	public void process(){
		this.task.run();
	}

}
