package com.appfirst.statsd.test;

import static org.junit.Assert.*;

import java.util.Date;

import org.junit.Test;

import com.appfirst.statsd.bucket.CounterBucket;
import com.appfirst.statsd.bucket.GaugeBucket;
import com.appfirst.statsd.bucket.TimerBucket;

public class TestBuckets {

	@Test
	public final void testCounter() {
		CounterBucket bucket = new CounterBucket();
		bucket.setName("counter");
		bucket.infuse(1);
		bucket.infuse(2);
		bucket.infuse(4);
		String actual = bucket.toString();
		String expected = "counter:7|c";
		assertEquals("Aggregated stat", expected, actual);
	}

	@Test
	public final void testTimer() {
		TimerBucket bucket = new TimerBucket();
		bucket.setName("timer");
		bucket.infuse(1);
		bucket.infuse(3);
		bucket.infuse(5);
		String actual = bucket.toString();
		String expected = "timer:3|ms";
		assertEquals("Aggregated stat", expected, actual);
	}

	@Test
	public final void testGauge() {
		String actual = "";
		long beforelast = 0L;
		try {
			GaugeBucket bucket = new GaugeBucket();
			bucket.setName("gauge");
			bucket.infuse(1);
			bucket.infuse(3);

			beforelast = new Date().getTime();
			Thread.sleep(1);

			bucket.infuse(5);
			actual = bucket.toString();

			Thread.sleep(1);
		} catch (InterruptedException e) {
		}

		String expectedhead = "gauge:3";
		String expectedtail = "|g";
		assertTrue("Aggregated stat", actual.startsWith(expectedhead));
		assertTrue("Aggregated stat", actual.endsWith(expectedtail));
		long timestamp = Long.valueOf(actual.substring(
				actual.indexOf(expectedhead) + expectedhead.length(), 
				actual.indexOf(expectedtail)));
		long now = new Date().getTime();
		assertTrue("Some time before now", timestamp < now);
		assertTrue("Some time after start", timestamp > beforelast);
	}
}