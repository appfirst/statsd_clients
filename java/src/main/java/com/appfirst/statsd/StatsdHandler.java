package com.appfirst.statsd;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import com.appfirst.statsd.annotation.Counting;
import com.appfirst.statsd.annotation.Timing;

public class StatsdHandler implements InvocationHandler {
	
	private static StatsdClient client = new AFClient();

	public void setStatsdClient(StatsdClient client){
		StatsdHandler.client = client;
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		Object result = null;
		if (method.isAnnotationPresent(Timing.class)){
			Timing timing = method.getAnnotation(Timing.class);
			long startTime = System.currentTimeMillis();
			result = method.invoke(proxy, args);
			long endTime = System.currentTimeMillis();
			client.timing(timing.value(), (int) (startTime-endTime));
		} else {
			method.invoke(proxy, args);
		}
		if (method.isAnnotationPresent(Counting.class)){
			Counting counting = method.getAnnotation(Counting.class);
			client.increment(counting.value());
		}
		return result;
	}
}
