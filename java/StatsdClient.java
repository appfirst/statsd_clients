/**
 * StatsdClient.java
 *
 * (C) 2011 Meetup, Inc.
 * Author: Andrew Gwozdziewycz <andrew@meetup.com>, @apgwoz
 *
 * 
 *
 * Example usage:
 *
 *    StatsdClient client = new StatsdClient("statsd.example.com", 8125);
 *    // increment by 1
 *    client.increment("foo.bar.baz");
 *    // increment by 10
 *    client.increment("foo.bar.baz", 10);
 *    // sample rate
 *    client.increment("foo.bar.baz", 10, .1);
 *    // increment multiple keys by 1
 *    client.increment("foo.bar.baz", "foo.bar.boo", "foo.baz.bar");
 *    // increment multiple keys by 10 -- yeah, it's "backwards"
 *    client.increment(10, "foo.bar.baz", "foo.bar.boo", "foo.baz.bar");
 *    // multiple keys with a sample rate
 *    client.increment(10, .1, "foo.bar.baz", "foo.bar.boo", "foo.baz.bar");
 *
 * Note: For best results, and greater availability, you'll probably want to 
 * create a wrapper class which creates a static client and proxies to it.
 *
 * You know... the "Java way."
 */

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Random;

public class StatsdClient {
	private static Random RNG = new Random();
	
	private Transport _transport;
	
	public StatsdClient() throws UnknownHostException, IOException{
		_transport = new UDPTransport();
	}

	public StatsdClient(Transport transport){
		_transport = transport;
	}

	public void setTransport(Transport transport){
		_transport = transport;
	}
	
	public boolean gauge(String key, int value) {
		String stat = buildMessage(key, value, "g", new Date().getTime(), null);
		return send(stat, 1);
	}
	
	public boolean gauge(String key, int value, String message) {
		String stat = buildMessage(key, value, "g", new Date().getTime(), message);
		return send(stat, 1);
	}


	public boolean timing(String key, int value) {
		return timing(key, value, 1.0);
	}

	public boolean timing(String key, int value, double sampleRate) {
		String stat = buildMessage(key, value, "ms", sampleRate, null);
		return send(stat, sampleRate);
	}

	public boolean timing(String key, int value, double sampleRate, String message) {
		String stat = buildMessage(key, value, "ms", sampleRate, message);
		return send(stat, sampleRate);
	}

	public boolean decrement(String key) {
		return increment(key, -1, 1.0);
	}

	public boolean decrement(String key, int magnitude) {
		return decrement(key, magnitude, 1.0);
	}

	public boolean decrement(String key, int magnitude, double sampleRate) {
		magnitude = magnitude < 0 ? magnitude : -magnitude;
		return increment(key, magnitude, sampleRate);
	}

	public boolean decrement(String... keys) {
		return increment(-1, 1.0, keys);
	}

	public boolean decrement(int magnitude, String... keys) {
		magnitude = magnitude < 0 ? magnitude : -magnitude;
		return increment(magnitude, 1.0, keys);
	}

	public boolean decrement(int magnitude, double sampleRate, String... keys) {
		magnitude = magnitude < 0 ? magnitude : -magnitude;
		return increment(magnitude, sampleRate, keys);
	}

	public boolean increment(String key) {
		return increment(key, 1, 1.0);
	}

	public boolean increment(String key, int magnitude) {
		return increment(key, magnitude, 1.0);
	}

	public boolean increment(String key, int magnitude, double sampleRate) {
		return update_stats(null, magnitude, sampleRate, key);
	}

	public boolean increment(int magnitude, double sampleRate, String... keys) {
		return update_stats(null, magnitude, sampleRate, keys);
	}

	public boolean update_stats(String message, int magnitude, double sampleRate, String... buckets){
		boolean result = true;
		for (int i = 0; i < buckets.length; i++) {
			String stat = buildMessage(buckets[i], magnitude, "c", sampleRate, message);
			result = result && send(stat, sampleRate);
		}
		return result;
	}

	private String buildMessage(String bucket, int magnitude, String type, double sampleRate, String message){
		String field2 = "";
		if (sampleRate < 1) {
			field2 = String.format("@%f", sampleRate);
		}
		return buildMessage(bucket, magnitude, type, field2, message);
	}

	private String buildMessage(String bucket, int magnitude, String type, long timestamp, String message){
		String field2 = String.valueOf(timestamp);
		return buildMessage(bucket, magnitude, type, field2, message);
	}
	
	private String buildMessage(String bucket, int magnitude, String type, String field2, String message){
		// bucket: field0 | field1 | field2                 | field3
		// bucket: value  | type   | sampele_rate/timestamp | message
		String stat = String.format("%s:%d|%s",  bucket, magnitude, type);
		// when message is there, we always keep field2 even if it's blank:
		// bucket:2|c||some_message
		if (message != null && !message.equals("")){
			stat += String.format("|%s|%s", field2, message);
		} else if (!field2.equals("")){
			stat += String.format("|%s", field2);
		}

		return stat;
	}

	private boolean send(String stat, double sampleRate) {
		if (sampleRate < 1.0 && RNG.nextDouble() > sampleRate) 
			return false;
		else {
			return this._transport.doSend(stat);
		}
	}
	
	public static void main(String[] args){
		try {
			Transport transport = new AFTransport();
			StatsdClient client = new StatsdClient(transport);
			client.gauge("test", 123);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}