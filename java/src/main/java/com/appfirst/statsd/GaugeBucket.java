package com.appfirst.statsd;

import java.util.Date;

public class GaugeBucket implements Bucket {
	private String name;
	private int sumstat = 0;
	private int count = 0;
	private long timestamp;
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
			stat = String.format("%s:%d|g|%s|%s", name, avg, timestamp, message);
		} else {
			stat = String.format("%s:%d|g|%s",  name, avg, timestamp);
		}
		return stat;
	}

	public GaugeBucket infuse(int value, String message){
		this.sumstat += value;
		this.count++;
		if (message != null && !message.equals("")){
			if (this.message != null){
				this.message += "|" + message;
			} else {
				this.message = message;
			}
		}
		this.timestamp = new Date().getTime();
		return this;
	}
}
