package com.appfirst.statsd;

public class CounterBucket implements Bucket{
	private String name;
	private int value = 0;
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
		String stat = String.format("%s:%d|c",  name, this.value);
		if (message != null && !message.equals("")){
			stat += String.format("||%s", message);
		} 
		return stat;
	}

	@Override
	public void infuse(int value, String message){
		this.value += value;
		if (message != null && !message.equals("")){
			if (this.message != null){
				this.message += "|" + message;
			} else {
				this.message = message;
			}
		}
	}
	
//	public void merge
}
