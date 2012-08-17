package com.appfirst.statsd;

import java.util.HashMap;
import java.util.Map;

import com.sun.jna.Library;
import com.sun.jna.Native;

public class AFTransport implements Transport {
	private static String AFCAPIName = "/afcollectorapi";
	private static String LibName = "rt";
	private static int O_WRONLY = 01;
	private static int AFCMaxMsgSize = 2048;
	private static int AFCSeverityStatsd = 3;

	public AFTransport() {
	}

	private UDPTransport _udptransport = null;

	public Transport getUDPTransport(){
		if (_udptransport == null){
			try {
				_udptransport = new UDPTransport();
			} catch (Exception e) {
			}
		}
		return _udptransport;
	}

	@Override
	public boolean doSend(String stat) {
		// trim msg if over allowed size
		if (stat.length() > AFCMaxMsgSize) {
			stat = stat.substring(0, AFCMaxMsgSize);
		}

		MQ mq = (MQ) Native.loadLibrary(LibName, MQ.class);
		int mqd = mq.mq_open(AFCAPIName, O_WRONLY);
		int rv = mq.mq_send(mqd, stat, stat.length(), AFCSeverityStatsd);
		mq.mq_close(mqd);

		if (AFCReturnCode.valueOf(rv) == AFCReturnCode.AFCSuccess) {
			return true;
		} else {
			Transport udp = this.getUDPTransport();
			if (udp != null){
				return udp.doSend(stat);
			} else {
				return false;
			}
		}
	}

	public interface MQ extends Library {
		public int mq_open(String filename, int mode);
		public int mq_close(int mqd);
		public int mq_send(int mqd, String msg, int len, int prio);
	}

	public enum AFCReturnCode{
		AFCSuccess(0),
		AFCNoMemory(1),
		AFCBadParam(2),
		AFCOpenError(3),
		AFCPostError(4),
		AFCWouldBlock(5),
		AFCCloseError(6);
		
		private int code;
		
		public int code(){
			return code;
		}
		
		private AFCReturnCode(int code){
			this.code = code;
		}
		
		private static Map<Integer, AFCReturnCode> map = null;

		public static AFCReturnCode valueOf(int code)
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
	
	public static void main(String[] args){
		Transport transport = new AFTransport();
		StatsdClient client = new StatsdClient(transport);
		client.gauge("gauge", 123);
		client.increment("counter");
		client.decrement("counter");
		client.updateStats(2, null, .5, "counter", "counter2");
		client.updateStats(-1, null, 1, "counter");
		client.timing("timing", 500);
		client.timing("timing", 488, "hello");
	}
}
