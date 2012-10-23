package com.appfirst.statsd.strategy;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.appfirst.statsd.bucket.Bucket;
import com.appfirst.statsd.transport.Transport;

public final class GeyserStrategy implements Strategy{
	static Logger log = Logger.getLogger(GeyserStrategy.class);
	public static int DEFAULT_INTERVAL = 5;
	public static int SHUTDOWN_TIME = 5;

	private int interval = DEFAULT_INTERVAL;
	private TimeUnit unit = TimeUnit.SECONDS;
	private Transport transport;

	private ScheduledFuture<?> progress;
	private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

	private BucketBuffer buffer = new BucketBuffer();
	
	private static boolean shutdownHookSet = false;

	private void setShutdownHook(){
		if (!shutdownHookSet){
			log.info("set the shutdown hook");
			Runtime.getRuntime().addShutdownHook(new Thread(){
				@Override
				public void run() {
					log.info("final execution");
					if (!executor.isShutdown()){
						executor.shutdown();
						try {
							if (!executor.awaitTermination(SHUTDOWN_TIME, unit)) {
								log.warn("Executor did not terminate in the specified time.");
								List<Runnable> droppedTasks = executor.shutdownNow();
								log.warn("Executor was abruptly shut down. " + droppedTasks.size() + " tasks will not be executed.");
							}
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					flush();
				}
			});
			shutdownHookSet = true;
		}
	}

	public void setTransport(Transport transport){
		this.transport = transport;
	}

	public void setInterval(int interval){
		this.interval = interval;
	}

	public void setTimeUnit(TimeUnit unit){
		this.unit = unit;
	}

	public <T extends Bucket> boolean send(
			Class<T> clazz,
			String bucketname,
			int value,
			String message){
		try {
			buffer.deposit(clazz, bucketname, value, message);
		} catch (BucketTypeMismatchException e) {
			e.printStackTrace();
			return false;
		} catch (InstantiationException e) {
			e.printStackTrace();
			return false;
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return false;
		}
		synchronized(executor){
			if (!(executor.isShutdown() || executor.isTerminated()) 
				&& (progress == null || progress.isDone())){

				log.info("scheduling execution");
				progress = executor.schedule(new Runnable() {
					@Override public void run() { flush(); }
				}, interval, unit);
			}
		}
		return true;
	}

	private void flush(){
		if (!buffer.isEmpty()){
			Map<String, Bucket> dumpcellar = buffer.withdraw();
			log.debug(dumpcellar.values());
			for (Bucket bucket : dumpcellar.values()){
				transport.doSend(bucket.toString());
			}
		}
	}

	GeyserStrategy(){
		log.debug("new GeyserStrategy");
		this.setShutdownHook();
	}
}
