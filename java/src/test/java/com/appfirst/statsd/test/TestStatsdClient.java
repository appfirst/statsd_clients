package com.appfirst.statsd.test;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.junit.Test;

import com.appfirst.statsd.AFClient;
import com.appfirst.statsd.DefaultStatsdClient;
import com.appfirst.statsd.StatsdClient;
import com.appfirst.statsd.strategy.StrategyFactory;
//import com.appfirst.statsd.transport.Transport;

public class TestStatsdClient {
	static Logger log = Logger.getLogger(TestStatsdClient.class);

	@Test
	public final void testSetStrategy() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testDump() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testRun() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testGaugeStringInt() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testGaugeStringIntString() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testTimingStringInt() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testTimingStringIntString() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testDecrement() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testIncrement() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testUpdateStatsIntStringArray() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testUpdateStatsIntDoubleStringArray() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testUpdateStatsIntStringDoubleStringArray() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testUpdateStatsStringIntDoubleString() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testDoSend() {
		fail("Not yet implemented"); // TODO
	}
	
	@Test
	public final void testUnderPressure() throws UnknownHostException, IOException, InterruptedException {
		BasicConfigurator.configure();
//		StatsdClient client = new AFClient();
		DefaultStatsdClient client = new DefaultStatsdClient();
		client.setStrategy(new StrategyFactory().getGeyserStrategy(2));
		Random r = new Random();
		for (int i=0; i<1000; i++){
			Thread.sleep(r.nextInt(5));
			long start = System.currentTimeMillis();
			client.increment("multiple1");
			int elapsedTimeMillis = (int)(System.currentTimeMillis()-start);
			client.timing("incr_time", elapsedTimeMillis);
			if (i%3==0){
				client.increment("multiple3");
			}
		}
	}

	class GeysertStrategyRunner implements Runnable{
		private StatsdClient client = null;
		private int times = 1;
		private long sendInterval = 1;
		
		GeysertStrategyRunner(int times, long sendInterval, int flushInterval){
//			this.client = new DefaultStatsdClient(){
//				@Override
//				protected Transport getTransport() {
//					return new Transport(){
//							@Override
//							public boolean doSend(String stat) {
//								log.info(String.format("sending %s",stat));
//								return false;
//							};
//					};
//				}
//			}.setStrategy(new StrategyFactory().getGeyserStrategy(flushInterval));
			this.client = new AFClient();
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
				client.increment("multiple1");
			}
		}
	}

	@Test
	public final void testMultithreading() throws UnknownHostException, IOException, InterruptedException {
		BasicConfigurator.configure();
		ExecutorService executor = Executors.newFixedThreadPool(10);
		
		for (int i=0; i<10; i++){
			executor.execute(new GeysertStrategyRunner(100000000, 0, 2));
		}
		
		
		
		executor.awaitTermination(1000, TimeUnit.SECONDS);
	}
	
	@Test
	public final void testAnotherMultithreading() throws UnknownHostException, IOException, InterruptedException {
		BasicConfigurator.configure();
		ExecutorService executor = Executors.newFixedThreadPool(10);
		
		for (int i=0; i<10; i++){
			executor.execute(new GeysertStrategyRunner(100000000, 0, 2));
		}

		executor.awaitTermination(1000, TimeUnit.SECONDS);
	}
}