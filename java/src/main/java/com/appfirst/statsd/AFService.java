package com.appfirst.statsd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.appfirst.statsd.strategy.Strategy;
import com.appfirst.statsd.strategy.StrategyFactory;
import com.appfirst.statsd.transport.MailSlotTransport;
import com.appfirst.statsd.transport.MqTransport;
import com.appfirst.statsd.transport.Transport;
import com.appfirst.statsd.transport.UdpTransport;
import com.sun.jna.LastErrorException;
import com.sun.jna.Platform;


/**
 * @author Yangming
 * Convenient Proxy for StatsdClient using AFTransport and GeyserStrategy()
 */
public class AFService extends DefaultService implements StatsdService {
	final static Logger log = LoggerFactory.getLogger(AFService.class.getSimpleName());

	private final class MixTransport implements Transport {
		private Transport transport = null;

		public MixTransport() {
			if (Platform.isLinux()) {
				transport = new MqTransport();
			} else if (Platform.isWindows()) {
				transport = new MailSlotTransport();
			}
		}

		private UdpTransport udpTransport;

		private boolean doUDPSend(final String stat){
			if (udpTransport == null) {
				try {
					udpTransport = new UdpTransport();
				} catch (Exception e) {
					log.error(String.format("Could not send stat %s with UDP either, sending message failed.", stat));
					return false;
				}
			}
			return udpTransport.doSend(stat);
		}

		@Override
		public boolean doSend(final String stat) {
			if (transport == null){
				log.error(String.format("No Transport Available, sending UDP msg to localhost.", stat));
				return this.doUDPSend(stat);
			}
			try {
				return transport.doSend(stat);
			} catch (LastErrorException e) {
				log.error(String.format("Could not send stat, Error Code: %s", e.getErrorCode()));
				return false;
			} catch (UnsatisfiedLinkError ufe) {
				log.error(String.format("%s, sending UDP msg to localhost.", ufe.getMessage(), stat));
				transport.close();
				return this.doUDPSend(stat);
			} catch (Exception e) {
				e.printStackTrace();
				log.error(String.format("Could not send stat %s with AFClient, sending UDP msg to localhost.", stat));
				transport.close();
				return this.doUDPSend(stat);
			}
		}
		
		@Override
		public boolean isAppFirst() {
			if (transport == null) {
				return false;  // Will default to UDP
			} else {
				return transport.isAppFirst();
			}
		}

		@Override
		public void close() {
			transport.close();
		}
	}

	/**
	 * Default Constructor. Initialize AFClient.
	 */
	public AFService() {
		transport = new MixTransport();
	}
	
	@Override
    public Strategy getStrategy() {
		if (strategy == null) {
			this.setStrategy(StrategyFactory.getGeyserStrategy());
		}
		return this.strategy;
	}

	public static void main(String[] args) {
		StatsdService c = new AFService();
		while (true) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			c.increment("test.java.count");
		}
	}
}
