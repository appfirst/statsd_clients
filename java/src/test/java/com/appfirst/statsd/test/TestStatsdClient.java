package com.appfirst.statsd.test;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Random;

import org.apache.log4j.BasicConfigurator;
import org.junit.Test;

import com.appfirst.statsd.GeyserStrategy;
import com.appfirst.statsd.UDPClient;

public class TestStatsdClient {

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
		UDPClient client = new UDPClient();
		client.setStrategy(new GeyserStrategy(2));
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

}
