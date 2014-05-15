package com.appfirst.statsd.bucket;

public class CounterBucket implements Bucket{
	private String name;
	private int value = 0;

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
		String stat = String.format("%s:%d|c",  name, this.value);
		return stat;
	}

	@Override
	public void infuse(int value){
		this.value += value;
	}
	
//	public void merge
}
