package com.appfirst.statsd;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import com.sun.jna.LastErrorException;
import com.sun.jna.Library;
import com.sun.jna.Native;

/**
 * Statsd Client that sends stats to AppFirst Collector. Use Posix message queue first, if fails, use UDP instead.
 * 
 * @author Yangming Huang
 *
 */
public class AFClient extends AbstractStatsdClient implements StatsdClient {
	static Logger log = Logger.getLogger(AFClient.class);

	private static String AFCAPIName = "/afcollectorapi";
	private static String LibName = "rt";
	private static int FLAG = 04001;
	private static int AFCMaxMsgSize = 2048;
	private static int AFCSeverityStatsd = 3;
	
	private int mqd = -1;
	private MQ mqlib = null;

	/**
	 * Default Constructor. Initialize AFClient.
	 */
	public AFClient() {
	}

	private UDPClient _udpClient = null;

	private boolean doUDPSend(final String stat){
		if (_udpClient == null){
			try {
				_udpClient = new UDPClient();
			} catch (Exception e) {
				log.error(String.format("Could not send stat %s with UDP either, sending message failed.", stat));
				return false;
			}
		}
		return _udpClient.doSend(stat);
	}
	
	private int openQueue(){
		if (this.mqd == -1){
			if (this.mqlib == null){
				this.mqlib = (MQ) Native.loadLibrary(LibName, MQ.class);
			}
			this.mqd = this.mqlib.mq_open(AFCAPIName, FLAG);
		}
		return this.mqd;
	}

	/* (non-Javadoc)
	 * @see com.appfirst.statsd.AbstractStatsdClient#doSend(java.lang.String)
	 */
	protected final boolean doSend(final String stat) {
		// trim msg if over allowed size
		String msg = (stat.length() > AFCMaxMsgSize) ? stat.substring(0, AFCMaxMsgSize) : stat;

		// log.info(String.format("Sending stat: %s", stat));
		try {
			int mqd = openQueue();
			int rc = this.mqlib.mq_send(mqd, msg, msg.length(), AFCSeverityStatsd);
			log.info(String.format("Sent: %s", stat));
			return AFCReturnCode.AFCSuccess == AFCReturnCode.valueOf(rc);
		} catch (LastErrorException e) {
			log.error(String.format("Could not send stat, Error Code: %s", e.getErrorCode()));
			return false;
		} catch (UnsatisfiedLinkError ufe){
			log.error(String.format("%s, sending UDP msg to localhost.",ufe.getMessage(), stat));
			this.close();
			return this.doUDPSend(msg);
		} catch (Exception e) {
			log.error(String.format("Could not send stat %s with AFClient, sending UDP msg to localhost.", stat));
			this.close();
			return this.doUDPSend(msg);
		}
	}
	
	public void close(){
		if (this.mqd >= 0){
			try{
				mqlib.mq_close(mqd);
			} catch (Exception e) {
			}
			this.mqd = -1;
		}
	}
	
	public void finalize(){
		this.close();
	}

	/**
	 * Interface that wraps basic functions of Posix Message Queue
	 */
	interface MQ extends Library {
		int mq_open(String filename, int mode) throws LastErrorException;;
		int mq_close(int mqd) throws LastErrorException;;
		int mq_send(int mqd, String msg, int len, int prio) throws LastErrorException;;
	}

	/**
	 * AFClient Return Code Enumeration.
	 *
	 */
	enum AFCReturnCode{
		AFCSuccess(0),
		AFCNoMemory(1),
		AFCBadParam(2),
		AFCOpenError(3),
		AFCPostError(4),
		AFCWouldBlock(5),
		AFCCloseError(6);
		
		private int code;
		
		int code(){
			return code;
		}
		
		private AFCReturnCode(int code){
			this.code = code;
		}
		
		private static Map<Integer, AFCReturnCode> map = null;

		static AFCReturnCode valueOf(int code)
		{
			synchronized(AFCReturnCode.class) {
				if (map == null) {
					map = new HashMap<Integer, AFCReturnCode>();
					for (AFCReturnCode v : values()) {
						map.put(v.code, v);
					}
				}
			}

			AFCReturnCode result = map.get(code);
			return result==null ? AFCReturnCode.AFCSuccess : result;
		}
	}

	/**
	 * This main function only intends to send some basic messages for testing.
	 *
	 * @param args
	 */
	public static void main(String[] args){
		BasicConfigurator.configure();
		StatsdClient client = new AFClient();
		client.gauge("test.java.gauge", 1);
		client.increment("test.java.counter");
		client.decrement("test.java.counter");
		client.updateStats(2, null, .5, "test.java.counter", "test.java.counter2");
		client.updateStats(-1, null, 1, "test.java.counter");
		client.timing("test.java.timing", 500);
		client.timing("test.java.timing", 488, "hello");
	}
}
