package com.appfirst.statsd;

public class TimerBucket implements Bucket{
	private String name;
	private int sumstat = 0;
	private int count = 0;
	private String message = null;

	@Override
	public void setName(String name){
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String toString(){
		String stat = null;
		int avg = this.sumstat/this.count;
		if (message != null && !message.equals("")){
			stat = String.format("%s:%d|ms||%s", name, avg, message);
		} else {
			stat = String.format("%s:%d|ms",  name, avg);
		}
		return stat;
	}

	public TimerBucket infuse(int value, String message){
		this.sumstat += value;
		this.count++;
		if (message != null && !message.equals("")){
			if (this.message != null){
				this.message += "|" + message;
			} else {
				this.message = message;
			}
		}
		return this;
	}
}
