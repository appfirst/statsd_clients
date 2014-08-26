package com.appfirst.statsd.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.regex.Pattern;

import org.junit.Test;

import com.appfirst.statsd.transport.MockService;

public class TestStatsdClient {

	private MockService statsd;

	@org.junit.Before
	public final void setUp(){
		 statsd = new MockService();
	}

	@Test
	public final void testGaugeStringInt() {
		statsd.gauge("gauge", 1);
		String pattern = "gauge:1\\|g\\|\\d+";
		assertTrue(String.format("pattern: %s but was %s", pattern, statsd.lastMessage()),
				   Pattern.matches(pattern, statsd.lastMessage()));
	}

	@Test
	public final void testTimingStringInt() {
		statsd.timing("timer", 1);
		assertEquals("timer:1|ms", statsd.lastMessage());
	}

	@Test
	public final void testDecrement() {
		statsd.decrement("counter");
		assertEquals("counter:-1|c", statsd.lastMessage());
	}

	@Test
	public final void testIncrement() {
		statsd.increment("counter");
		assertEquals("counter:1|c", statsd.lastMessage());
	}

	@Test
	public final void testUpdateStatsIntStringArray() {
		statsd.updateStats(1, "counter1", "counter2");
		Iterator<String> iter = statsd.iterator();
		String[] expected = {
				"counter1:1|c",
				"counter2:1|c"
			};
			assertIterator(expected, iter, true);
	}

	@Test
	public final void testUpdateStatsIntDoubleStringArray() {
		statsd.updateStats(1, 1, "counter1", "counter2");
		Iterator<String> iter = statsd.iterator();
		String[] expected = {
			"counter1:1|c",
			"counter2:1|c"
		};
		assertIterator(expected, iter, true);
	}

	
	public static void assertIterator(String[] expected, Iterator<String> iter, boolean exact) {
		int i=0;
		while (iter.hasNext()){
			assertTrue("less stats has been sent", i < expected.length);
			String actual = iter.next();
			String pattern = expected[i++];
			if (exact) {
				assertEquals(pattern, actual);
			}else{
				assertTrue(String.format("pattern: %s but was %s", pattern, actual), 
					Pattern.matches(pattern, actual));
			}
		}
		assertFalse("more stats sent than expected", iter.hasNext());
	}
}