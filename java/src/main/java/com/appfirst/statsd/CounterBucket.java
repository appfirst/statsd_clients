package com.appfirst.statsd;

import java.util.Random;

public class CounterBucket implements Bucket{
	private String name;
	private int value = 0;
	private String message = null;
	private static Random RNG = new Random();

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
		if (message != null && !message.equals("")){
			stat += String.format("||%s", message);
		} 
		return stat;
	}

	public CounterBucket infuse(int value, double sampleRate, String message){
		if (sampleRate < 1.0 && RNG.nextDouble() > sampleRate) 
			return this;
		this.value += value/sampleRate;
		if (message != null && !message.equals("")){
			if (this.message != null){
				this.message += "|" + message;
			} else {
				this.message = message;
			}
		}
		return this;
	}
	
//	public void merge
}
