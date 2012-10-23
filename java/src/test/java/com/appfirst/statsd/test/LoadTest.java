package com.appfirst.statsd.test;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import com.appfirst.statsd.DefaultStatsdClient;
import com.appfirst.statsd.StatsdClient;
import com.appfirst.statsd.annotation.Timing;
import com.appfirst.statsd.strategy.StrategyFactory;
import com.appfirst.statsd.transport.Transport;

class GeysertStrategyRunner implements Runnable{
	static Logger log = Logger.getLogger(GeysertStrategyRunner.class);
	private StatsdClient client = null;
	private int times = 1;
	private long sendInterval = 1;
	
	GeysertStrategyRunner(int times, long sendInterval, int flushInterval){
		this.client = new DefaultStatsdClient(){
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

	static void underPressure() throws InterruptedException
	{
		DefaultStatsdClient client = new DefaultStatsdClient();
		client.setStrategy(new StrategyFactory().getGeyserStrategy(2));
		Random r = new Random();
		for (int i = 0; i < 1000; i++) {
			Thread.sleep(r.nextInt(5));
			long start = System.currentTimeMillis();
			client.increment("multiple1");
			int elapsedTimeMillis = (int) (System.currentTimeMillis() - start);
			client.timing("incr_time", elapsedTimeMillis);
			if (i % 3 == 0) {
				client.increment("multiple3");
			}
		}
	}
	
	public static void main(String[] args) {
		BasicConfigurator.configure();
		
		try {
			underPressure();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
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