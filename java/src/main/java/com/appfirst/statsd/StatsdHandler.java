package com.appfirst.statsd;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.appfirst.statsd.annotation.Counting;
import com.appfirst.statsd.annotation.Timing;

public class StatsdHandler implements InvocationHandler {

	private static StatsdClient client = new AFClient();

	public static void setStatsdClient(StatsdClient client){
		StatsdHandler.client = client;
	}

	public static Object proxy(Object object){
		return Proxy.newProxyInstance(
				object.getClass().getClassLoader(), 
				object.getClass().getInterfaces(),
				new StatsdHandler(object));
	}
	
	public StatsdHandler(Object object){
		this.object = object;
	}

	private Object object;

	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		Object result = null;
		Method objectMethod = object.getClass().getMethod(method.getName(), method.getParameterTypes());
		if (objectMethod.isAnnotationPresent(Timing.class)){
			Timing timing = objectMethod.getAnnotation(Timing.class);
			long startTime = System.currentTimeMillis();
			result = method.invoke(object, args);
			long endTime = System.currentTimeMillis();
			client.timing(timing.value(), (int) (startTime-endTime));
		} else {
			method.invoke(object, args);
		}
		if (objectMethod.isAnnotationPresent(Counting.class)){
			Counting counting = objectMethod.getAnnotation(Counting.class);
			client.increment(counting.value());
		}
		return result;
	}
}
