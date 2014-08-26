package com.appfirst.statsd.transport;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.LastErrorException;
import com.sun.jna.Library;
import com.sun.jna.Native;

/**
 * Statsd Client that sends stats to AppFirst Collector. Use Posix message queue first, if fails, use UDP instead.
 * 
 * @author Yangming Huang
 *
 */
public class MqTransport implements Transport {
	static Logger log = LoggerFactory.getLogger(MqTransport.class.getSimpleName());

	private static String AFCAPIName = "/afcollectorapi";
	private static String LibName = "rt";
	private static int FLAG = 04001;
	private static int AFCMaxMsgSize = 2048;
	private static int AFCSeverityStatsd = 3;

	private int mqd = -1;
	private MQ mqlib = null;
	
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
	 * @see com.appfirst.statsd.Transport#doSend(java.lang.String)
	 */
	@Override
	public boolean doSend(final String stat) {
		// trim msg if over allowed size
		String msg = (stat.length() > AFCMaxMsgSize) ? stat.substring(0, AFCMaxMsgSize) : stat;

		int mqd = openQueue();
		int rc = this.mqlib.mq_send(mqd, msg, msg.length(), AFCSeverityStatsd);
		log.info(String.format("Sent: %s", stat));
		return AFCReturnCode.AFCSuccess == AFCReturnCode.valueOf(rc);
	}
	
	@Override
	public boolean isAppFirst() {
		return true;
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
}
