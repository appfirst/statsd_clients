package com.appfirst.statsd.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.appfirst.statsd.AFService;
import com.appfirst.statsd.StatsdService;
import com.appfirst.statsd.strategy.StrategyFactory;
import com.appfirst.statsd.transport.MailSlotTransport;
import com.appfirst.statsd.transport.MqTransport;
import com.appfirst.statsd.transport.Transport;
import com.sun.jna.Platform;

public class TestTransports {

	@Test
	public void testDoSend() {
		StatsdService sc = new AFService().setStrategy(StrategyFactory.getInstantStrategy());
		Transport t = sc.getTransport();
		String stat = String.format("af.stats_test.java.transport.%s:1|c", t.getClass().getSimpleName());
		assertNotNull(t);
		boolean result = t.doSend(stat);
		assertTrue(result);
	}

	@Test
	public void testMqTransport() {
		if (!Platform.isLinux()){
			return;
		}
		Transport t = new MqTransport();
		String stat = String.format("af.stats_test.java.transport.%s:1|c", t.getClass().getSimpleName());
		boolean result = t.doSend(stat);
		assertTrue(result);
	}

	@Test
	public void testMailSlotTransport() {
		if (!Platform.isWindows()){
			return;
		}
		Transport t = new MailSlotTransport();
		String stat = String.format("af.stats_test.java.transport.%s:1|c", t.getClass().getSimpleName());
		boolean result = t.doSend(stat);
		assertTrue(result);
	}
}
