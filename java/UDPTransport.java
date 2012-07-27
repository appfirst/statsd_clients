import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

import org.apache.log4j.Logger;


public class UDPTransport implements Transport{
	private static Logger log = Logger.getLogger(StatsdClient.class.getName());

	private InetSocketAddress _address;
	private DatagramChannel _channel;

	public UDPTransport() throws UnknownHostException, IOException{
		this(InetAddress.getLocalHost(), 8125);
	}
	
	public UDPTransport(String host, int port) throws UnknownHostException, IOException {
		this(InetAddress.getByName(host), port);
	}

	public UDPTransport(InetAddress host, int port) throws IOException {
		this._address = new InetSocketAddress(host, port);
		this._channel = DatagramChannel.open();
	}
	
	@Override
	public boolean doSend(final String stat) {
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
