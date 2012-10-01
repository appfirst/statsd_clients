package com.appfirst.statsd.transport;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

import org.apache.log4j.Logger;


/**
 * Standard Statsd Client that sends stats thru UDP protocol.
 * 
 * @author Yangming Huang
 *
 */
public class UdpTransport implements Transport{
	public static int DEFAULT_STATSD_PORT = 8125;
	private static Logger log = Logger.getLogger(UdpTransport.class);

	private InetSocketAddress _address;
	private DatagramChannel _channel;

	/**
	 * Initializes a new instance sends message to localhost with Default port 8125
	 * 
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public UdpTransport() throws UnknownHostException, IOException{
		this(InetAddress.getLocalHost(), DEFAULT_STATSD_PORT);
	}

	/**
	 * Initializes a new instance sends message to specified host and port
	 * 
	 * @param host - The host address of the StatsD server
	 * @param port - The port of the StatsD server
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public UdpTransport(String host, int port) throws UnknownHostException, IOException {
		this(InetAddress.getByName(host), port);
	}

	/**
	 * Initializes a new instance sends message to specified host and port
	 * 
	 * @param host - The InetAddress format of the host address of the StatsD server
	 * @param port - The port of the StatsD server
	 * @throws IOException
	 */
	public UdpTransport(InetAddress host, int port) throws IOException {
		this._address = new InetSocketAddress(host, port);
		this._channel = DatagramChannel.open();
	}

	/* (non-Javadoc)
	 * @see com.appfirst.statsd.Transport#doSend(java.lang.String)
	 */
	@Override
	public final boolean doSend(final String stat) {
		log.info(String.format("Sending stat: %s", stat));
		try {
			final byte[] data = stat.getBytes("utf-8");
			final ByteBuffer buff = ByteBuffer.wrap(data);
			final int nbSentBytes = _channel.send(buff, _address);

			if (data.length == nbSentBytes) {
				return true;
			} else {
				log.error(String.format(
						"Could not send entirely stat %s to host %s:%d. Only sent %i bytes out of %i bytes", stat,
						_address.getHostName(), _address.getPort(), nbSentBytes, data.length));
				return false;
			}

		} catch (IOException e) {
			log.error(
					String.format("Could not send stat %s to host %s:%d", stat, _address.getHostName(),
							_address.getPort()), e);
			return false;
		}
	}
}
