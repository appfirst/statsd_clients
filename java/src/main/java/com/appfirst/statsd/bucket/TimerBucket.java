package com.appfirst.statsd.bucket;

import java.util.ArrayList;


public class TimerBucket implements Bucket{
	private String name;
	private ArrayList<Integer> values = new ArrayList<Integer>();
	private int count = 0;

	@Override
	public void setName(String name){
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	private String getUdpString() {
		/* Find average of all values we've collected */
		int sum = 0;
		for (Integer i: this.values) {
			sum += i;
		}
		int avg = sum / this.count;
		String stat = String.format("%s:%d|ms", name, avg);
		return stat;
	}
	
	private String getAfString(){
		/* Return a string w/ a full list of values rather than average */
		StringBuilder value_str_build = new StringBuilder();
		for (Integer i: this.values) {
			value_str_build.append(String.valueOf(i));
			value_str_build.append(",");
		}
		// Trim trailing ,
		String value_str = value_str_build.toString();
		value_str = value_str.substring(0, value_str.length()-1);
		String stat = String.format("%s:%s|ms", name, value_str);
		return stat;
	}

	@Override
	public void infuse(int value){
		this.values.add(value);
		this.count++;
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
}
