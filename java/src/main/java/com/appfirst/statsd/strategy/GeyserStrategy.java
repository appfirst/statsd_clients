package com.appfirst.statsd.strategy;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.appfirst.statsd.bucket.Bucket;
import com.appfirst.statsd.transport.Transport;

public final class GeyserStrategy implements Strategy{
	static Logger log = LoggerFactory.getLogger(GeyserStrategy.class.getSimpleName());

	public static int DEFAULT_INTERVAL = 5;
	public static int SHUTDOWN_TIME = 5;

	private int interval = DEFAULT_INTERVAL;
	private TimeUnit unit = TimeUnit.SECONDS;
	private Transport transport;

	private ScheduledFuture<?> progress;
	private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

	private BucketBuffer buffer = new BucketBuffer();

	private boolean shutdownHookSet = false;

	private void setShutdownHook(){
		if (!shutdownHookSet){
			log.info("set the shutdown hook");
			Runtime.getRuntime().addShutdownHook(new Thread(){
				@Override
				public void run() {
					terminate();
				}
			});
			shutdownHookSet = true;
		}
	}
	
	public void terminate() {
		log.info("force terminating execution");
		if (!executor.isShutdown()) {
			executor.shutdown();
			try {
				if (!executor.awaitTermination(SHUTDOWN_TIME, unit)) {
					log.warn("Executor did not terminate in the specified time.");
					List<Runnable> droppedTasks = executor.shutdownNow();
					log.warn("Executor was abruptly shut down. " + 
						  	 droppedTasks.size() + " tasks will not be executed.");
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		flush();
		transport.close();
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

	public <T extends Bucket> boolean send(Class<T> clazz, String bucketname, int value) {
		try {
			buffer.deposit(clazz, bucketname, value);
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
				&& (progress == null || progress.isDone() || progress.isCancelled())) {
				log.info(String.format("scheduling execution after %s %s", interval, unit));
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
			log.debug(dumpcellar.values().toString());
			for (Bucket bucket : dumpcellar.values()){
				transport.doSend(bucket.getOutput(transport.isAppFirst()));
			}
		}
	}

	private GeyserStrategy(){
		this.setShutdownHook();
	}

	private static volatile GeyserStrategy singleton = null;

	public static GeyserStrategy getSingleton(){
		if (singleton == null) {
			synchronized(GeyserStrategy.class){
				if (singleton == null){
					singleton = new GeyserStrategy();
				}
			}
		}
		return singleton;
	}
}
