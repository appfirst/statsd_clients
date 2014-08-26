package com.appfirst.statsd.transport;

public interface Transport {
	/**
	 * To write a customized client, all you need is to implement this method which sends the stats
	 *  message to StatsD Server thru your own media.
	 *
	 * @param stat - the formatted message ready to send to the StatsD Server.
	 * @return True if success, False otherwise.
	 */
	boolean doSend(String stat);
	
	/**
	 * This method is used to determine how data should be formatted when sent up to the server.
	 * If being sent to AppFirst (via POSIX MQ or Windows Mailslot), it should return true.
	 * @return True if an AppFirst transport type, False otherwise.
	 */
	boolean isAppFirst();

	/**
	 * To close a transport, most of the transport need to release resources properly
	 */
	void close();
}
