package com.appfirst.statsd.bucket;

import java.util.Date;

public class GaugeBucket implements Bucket {
	private String name;
	private int sumstat = 0;
	private int count = 0;
	private long timestamp;

	@Override
	public void setName(String name){
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getOutput() {
		return this.getAfString();
	}
	
	@Override
	public String getOutput(Boolean isAppFirst) {
		if (isAppFirst) {
			return this.getAfString();
		} else {
			return this.getUdpString();
		}
	}
	
	@Override
	public String toString() {
		return this.getOutput();
	}

	private String getUdpString() {
		String stat = null;
		int avg = this.sumstat/this.count;
		stat = String.format("%s:%d|g|%s", name, avg, timestamp);
		return stat;
	}
	
	private String getAfString() {
		return this.getUdpString();
	}

	@Override
	public void infuse(int value){
		this.sumstat += value;
		this.count++;
		this.timestamp = new Date().getTime();
	}
}
