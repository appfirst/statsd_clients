package com.appfirst.statsd.transport;

import java.util.Iterator;
import java.util.Stack;

import com.appfirst.statsd.DefaultService;
import com.appfirst.statsd.StatsdService;

public class MockService extends DefaultService implements StatsdService, Iterable<String>{
	private Stack<String> output = new Stack<String>();

	@Override
	public Iterator<String> iterator(){
		return output.iterator();
	}

	public String lastMessage(){
		return output.peek();
	}
	
	public void clear(){
		output.clear();
	}

	public Transport getTransport(){
		return new Transport() {

			@Override
			public boolean doSend(String stat) {
				return output.add(stat);
			}

			@Override
			public boolean isAppFirst() {
				return true;
			}
			
			@Override public void close() {}
		};
	}

}
