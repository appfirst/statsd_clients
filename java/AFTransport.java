import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import com.sun.jna.Library;
import com.sun.jna.Native;


public class AFTransport extends UDPTransport implements Transport {
	private static String AFCAPIName = "/afcollectorapi";
	private static String LibName = "rt";
	private static int OPEN_WRITEONLY = 01;
	private static int AFCMaxMsgSize = 2048;
	private static int AFCSeverityStatsd = 3;

	public AFTransport() throws UnknownHostException, IOException{
		super();
	}

	@Override
	public boolean doSend(String stat) {
		// trim msg if over allowed size
		if (stat.length() > AFCMaxMsgSize) {
			stat = stat.substring(0, AFCMaxMsgSize);
		}

		MQ mq = (MQ) Native.loadLibrary(LibName, MQ.class);
		int mqd = mq.mq_open(AFCAPIName, OPEN_WRITEONLY);
		int rv = mq.mq_send(mqd, stat, stat.length(), AFCSeverityStatsd);
		mq.mq_close(mqd);

		if (AFCReturnCode.valueOf(rv) == AFCReturnCode.AFCSuccess) {
			return true;
		} else {
			return super.doSend(stat);
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
}
