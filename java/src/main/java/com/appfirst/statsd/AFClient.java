package com.appfirst.statsd;

import org.apache.log4j.Logger;

import com.appfirst.statsd.strategy.StrategyFactory;
import com.appfirst.statsd.transport.AFTransport;
import com.appfirst.statsd.transport.Transport;
import com.appfirst.statsd.transport.UdpTransport;
import com.sun.jna.LastErrorException;


/**
 * @author Yangming
 * Convenient Proxy for StatsdClient using AFTransport and GeyserStrategy()
 */
public class AFClient extends DefaultStatsdClient{
	static Logger log = Logger.getLogger(AFClient.class);

	private final Transport transport = new MixTransport();
	
	private final class MixTransport extends AFTransport{
		private final AFTransport transport = new AFTransport();

		private UdpTransport udpTransport;

		private boolean doUDPSend(final String stat){
			if (udpTransport == null){
				try {
					udpTransport = new UdpTransport();
				} catch (Exception e) {
					log.error(String.format("Could not send stat %s with UDP either, sending message failed.", stat));
					return false;
				}
			}
			return udpTransport.doSend(stat);
		}
		
		public boolean doSend(final String stat){
			try {
				return super.doSend(stat);
			} catch (LastErrorException e) {
				log.error(String.format("Could not send stat, Error Code: %s", e.getErrorCode()));
				return false;
			} catch (UnsatisfiedLinkError ufe){
				log.error(String.format("%s, sending UDP msg to localhost.", ufe.getMessage(), stat));
				transport.close();
				return this.doUDPSend(stat);
			} catch (Exception e) {
				log.error(String.format("Could not send stat %s with AFClient, sending UDP msg to localhost.", stat));
				transport.close();
				return this.doUDPSend(stat);
			}
		}
	}

	/**
	 * Default Constructor. Initialize AFClient.
	 */
	public AFClient() {
		this.setStrategy(new StrategyFactory().getGeyserStrategy());
	}

	@Override
	protected Transport getTransport() {
		return transport;
	}
}
