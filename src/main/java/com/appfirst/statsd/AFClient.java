package com.appfirst.statsd;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.sun.jna.Library;
import com.sun.jna.Native;

/**
 * Statsd Client that sends stats to AppFirst Collector. Use Posix message queue first, if fails, use UDP instead.
 * 
 * @author Yangming Huang
 *
 */
public class AFClient extends AbstractStatsdClient implements StatsdClient {
	private static Logger log = Logger.getLogger(AFClient.class.getName());

	private static String AFCAPIName = "/afcollectorapi";
	private static String LibName = "rt";
	private static int O_WRONLY = 01;
	private static int AFCMaxMsgSize = 2048;
	private static int AFCSeverityStatsd = 3;

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

	/* (non-Javadoc)
	 * @see com.appfirst.statsd.AbstractStatsdClient#doSend(java.lang.String)
	 */
	@Override
	protected final boolean doSend(final String stat) {
		// trim msg if over allowed size
		String msg = (stat.length() > AFCMaxMsgSize) ? stat.substring(0, AFCMaxMsgSize) : stat;
		try {
			MQ mq = (MQ) Native.loadLibrary(LibName, MQ.class);
			int mqd = mq.mq_open(AFCAPIName, O_WRONLY);
			int rc = mq.mq_send(mqd, msg, msg.length(), AFCSeverityStatsd);
			AFCReturnCode rv = AFCReturnCode.valueOf(rc);
			mq.mq_close(mqd);
	
			if (rv == AFCReturnCode.AFCSuccess) {
				return true;
			}else {
				log.error(String.format("Send stat %s with AFClient returns %s", stat, rv));
				throw new Exception();
			}
		} catch (Exception e) {
			log.error(String.format("Could not send stat %s with AFClient, sending UDP msg to localhost.", stat));
			return this.doUDPSend(msg);
		}
	}

	
	/**
	 * Interface that wraps basic functions of Posix Message Queue
	 */
	interface MQ extends Library {
		int mq_open(String filename, int mode);
		int mq_close(int mqd);
		int mq_send(int mqd, String msg, int len, int prio);
	}

	/**
	 * AppFirst Client Return Code Enumeration.
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
		StatsdClient client = new AFClient();
		client.gauge("gauge", 123);
		client.increment("counter");
		client.decrement("counter");
		client.updateStats(2, null, .5, "counter", "counter2");
		client.updateStats(-1, null, 1, "counter");
		client.timing("timing", 500);
		client.timing("timing", 488, "hello");
	}
}
