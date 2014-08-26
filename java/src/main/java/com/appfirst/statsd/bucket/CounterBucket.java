package com.appfirst.statsd.bucket;

public class CounterBucket implements Bucket {
	private String name;
	private int value = 0;

	@Override
	public void setName(String name) {
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

	@Override
	public void infuse(int value) {
		this.value += value;
	}
	
	private String getUdpString() {
		String stat = String.format("%s:%d|c", name, this.value);
		return stat;
	}
	
	private String getAfString() {
		return this.getUdpString();
	}
}
