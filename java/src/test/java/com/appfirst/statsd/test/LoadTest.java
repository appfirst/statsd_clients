package com.appfirst.statsd.test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import com.appfirst.statsd.AFClient;
import com.appfirst.statsd.AbstractStatsdClient;
import com.appfirst.statsd.StatsdClient;
import com.appfirst.statsd.Transport;
import com.appfirst.statsd.annotation.Timing;
import com.appfirst.statsd.strategy.StrategyFactory;

class GeysertStrategyRunner implements Runnable{
	static Logger log = Logger.getLogger(GeysertStrategyRunner.class);
	private StatsdClient client = null;
	private int times = 1;
	private long sendInterval = 1;
	
	GeysertStrategyRunner(int times, long sendInterval, int flushInterval){
		this.client = new AbstractStatsdClient(){
			@Override
			protected Transport getTransport() {
				return new Transport(){
						@Override
						public boolean doSend(String stat) {
							log.info(String.format("sending %s",stat));
							return false;
						};
				};
			}
		}.setStrategy(new StrategyFactory().getGeyserStrategy(flushInterval));
		this.times = times;
		this.sendInterval = sendInterval;
	}

	@Timing("java.test.GeysertStrategyRunner.run")
	public void run(){
		for (int i=0; i<times; i++){
			if (sendInterval > 0){
				try {
					Thread.sleep(sendInterval);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			client.increment("java.test.counter");
		}
		client.increment("java.test.gauge");
		client.increment("java.test.timer");
	}
}

public class LoadTest {

	public static void main(String[] args) {
		BasicConfigurator.configure();
		ExecutorService executor = Executors.newFixedThreadPool(10);

		for (int i=0; i<10000; i++){
			executor.execute(new GeysertStrategyRunner(10000, 0, 2));
		}

		try {
			executor.awaitTermination(1, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}




// A correct implementation of a producer and consumer.
class Q {
	int n;
	boolean valueSet = false;

	synchronized int get() {
		if (!valueSet)
			try {
				wait();
			} catch (InterruptedException e) {
				System.out.println("InterruptedException caught");
			}
		System.out.println("Got: " + n);
		valueSet = false;
		notify();
		return n;
	}

	synchronized void put(int n) {
		if (valueSet)
			try {
				wait();
			} catch (InterruptedException e) {
				System.out.println("InterruptedException caught");
			}
		this.n = n;
		valueSet = true;
		System.out.println("Put: " + n);
		notify();
	}
}

class Producer implements Runnable {
	Q q;

	Producer(Q q) {
		this.q = q;
		new Thread(this, "Producer").start();
	}

	public void run() {
		int i = 0;
		while (true) {
			q.put(i++);
		}
	}
}

class Consumer implements Runnable {
	Q q;

	Consumer(Q q) {
		this.q = q;
		new Thread(this, "Consumer").start();
	}

	public void run() {
		while (true) {
			q.get();
		}
	}
}

class PCFixed {
	public static void main(String args[]) {
		Q q = new Q();
		new Producer(q);
		new Consumer(q);
		System.out.println("Press Control-C to stop.");
	}
}