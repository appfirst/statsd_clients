package com.appfirst.statsd.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.appfirst.statsd.StatsdService;
import com.appfirst.statsd.strategy.Strategy;
import com.appfirst.statsd.strategy.StrategyFactory;
import com.appfirst.statsd.transport.MockService;

public class TestStrategies {
	
	private MockService service = null;
			
	@Before
	public final void setUp(){
		this.service = new MockService();
	}

	@Test
	public final void testUnderPressure() {
		StatsdService client = service.setStrategy(StrategyFactory.getGeyserStrategy(1));
		for (int i=0; i<1000; i++){
			long start = System.currentTimeMillis();
			client.increment("multiple1");
			int elapsedTimeMillis = (int)(System.currentTimeMillis()-start);
			client.timing("incr_time", elapsedTimeMillis);
			if (i%3==0){
				client.increment("multiple3");
			}
		}
		StrategyFactory.getGeyserStrategy().terminate();
	}

	class GeysertStrategyRunner implements Runnable{
		private int times = 1;
		private long sendInterval = 1;

		GeysertStrategyRunner(int times, long sendInterval, int flushInterval){
			Strategy s = StrategyFactory.getGeyserStrategy(flushInterval);
			service.setStrategy(s);
			this.times = times;
			this.sendInterval = sendInterval;
		}

		public void run(){
			for (int i=0; i<times; i++){
				if (sendInterval > 0){
					try {
						Thread.sleep(sendInterval);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				service.increment("multiple1");
			}
		}
	}

	@Test
	public final void testMultithreading() throws UnknownHostException, IOException, InterruptedException {
		ExecutorService executor = Executors.newFixedThreadPool(10);
		for (int i=0; i<10; i++){
			executor.execute(new GeysertStrategyRunner(10, 0, 1));
		}
		executor.awaitTermination(1, TimeUnit.SECONDS);
		StrategyFactory.getGeyserStrategy().terminate();
		assertEquals("multiple1:100|c", this.service.lastMessage());
	}
}
