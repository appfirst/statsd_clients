package com.appfirst.statsd.transport;

import java.io.IOException;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.appfirst.statsd.AFService;
import com.appfirst.statsd.transport.MqTransport.AFCReturnCode;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
//import com.sun.jna.platform.win32.WinNT.SECURITY_ATTRIBUTES;
import com.sun.jna.ptr.IntByReference;

public class MailSlotTransport implements Transport {
	static Logger log = LoggerFactory
	        .getLogger(AFService.class.getSimpleName());

	private static String SLOTNAME = "\\\\.\\mailslot\\afcollectorapi";
	
	public static final int FILE_ACCESS_WRITE = 2;

	private static int AFCMaxMsgSize = 2048;
	private static int AFCSeverityStatsd = 3;

	private Kernel32 lib = Kernel32.INSTANCE;
	private HANDLE mailSlot = null;

	private HANDLE getMailSlot() throws IOException {
		if (mailSlot == null || mailSlot == WinBase.INVALID_HANDLE_VALUE) {
			mailSlot = lib.CreateFile(SLOTNAME, FILE_ACCESS_WRITE,
			        WinNT.FILE_SHARE_READ | WinNT.FILE_SHARE_WRITE,
			        new WinBase.SECURITY_ATTRIBUTES(), WinNT.OPEN_EXISTING,
			        WinNT.FILE_ATTRIBUTE_NORMAL, null);
			log.info("MailSlot handle valid: " + (mailSlot != WinBase.INVALID_HANDLE_VALUE));
		}
		if (mailSlot != WinBase.INVALID_HANDLE_VALUE) {
			return mailSlot;
		} else {
			throw new IOException(
			        "MailSlot Cannot be initialized: Handle is Invalid");
		}
	}

	@Override
	public boolean doSend(final String stat) {
		try {
			String data_string = String.format("%s:%s:%s",
			        lib.GetCurrentProcessId(), AFCSeverityStatsd, stat);
			byte[] data_bytes = data_string.getBytes("UTF-16");
			int byteCount = data_bytes.length;
			if (byteCount > AFCMaxMsgSize) {
				log.info(String
				        .format("message size %s bytes but is limited to %s bytes, will be truncated",
				                byteCount, AFCMaxMsgSize));
				byteCount = AFCMaxMsgSize;
			}
			synchronized (this) {
				IntByReference lpNumberOfBytesRead = new IntByReference(0);
				boolean retval = lib.WriteFile(this.getMailSlot(), data_bytes, byteCount,
				        lpNumberOfBytesRead, null);
				if (!retval){
					log.debug("last error code:" + lib.GetLastError());
				}
			}
			log.info("Sent: "
			        + new String(Arrays.copyOfRange(data_bytes, 0, byteCount),
			                "UTF-16"));
			this.close();

			return AFCReturnCode.AFCSuccess == AFCReturnCode.valueOf(lib.GetLastError());
		} catch (IOException ioe) {
			this.close();
			log.error(String.format("%s Exception caught.", ioe));
			return false;
		}
		// return true;
	}
	
	@Override
	public boolean isAppFirst() {
		return true;
	}

	public void close() {
		log.debug("closing mailslot");
		synchronized (this) {
			if (mailSlot != null) {
				try {
					lib.CloseHandle(mailSlot);
				} catch (Exception e) {
				} finally {
					mailSlot = null;
				}
			}
		}
	}

	public void finalize() {
		this.close();
	}
}
