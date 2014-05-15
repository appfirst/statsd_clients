package com.appfirst.statsd.bucket;


public class TimerBucket implements Bucket{
	private String name;
	private int sumstat = 0;
	private int count = 0;

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
		stat = String.format("%s:%d|ms",  name, avg);
		return stat;
	}

	@Override
	public void infuse(int value){
		this.sumstat += value;
		this.count++;
	}

}
