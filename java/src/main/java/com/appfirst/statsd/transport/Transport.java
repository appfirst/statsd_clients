package com.appfirst.statsd.transport;

public interface Transport {/**
	 * To write a customized client, all you need is to implement this method which sends the stats
	 *  message to StatsD Server thru your own media.
	 *
	 * @param stat - the formatted message ready to send to the StatsD Server.
	 * @return True if success, False otherwise.
	 */
	boolean doSend(String stat);
}
