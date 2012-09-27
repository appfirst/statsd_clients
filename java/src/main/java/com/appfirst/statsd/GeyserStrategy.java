package com.appfirst.statsd;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;


public final class GeyserStrategy implements Strategy{
	static Logger log = Logger.getLogger(GeyserStrategy.class);
	public static int DEFAULT_INTERVAL = 5;
	public static int SHUTDOWN_TIME = 5;

	private int interval = DEFAULT_INTERVAL;
	private TimeUnit unit = TimeUnit.SECONDS;
	private Transport transport;

	private ScheduledFuture<?> progress;
	private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

	private void initialize(){
		Runtime.getRuntime().addShutdownHook(new Thread(){
			@Override
			public void run() {
				log.info("final execution");
				if (!executor.isShutdown()){
					executor.shutdown();
					try {
						if (!executor.awaitTermination(SHUTDOWN_TIME, TimeUnit.SECONDS)) { //optional *
							log.warn("Executor did not terminate in the specified time."); //optional *
							List<Runnable> droppedTasks = executor.shutdownNow(); //optional **
							log.warn("Executor was abruptly shut down. " + droppedTasks.size() + " tasks will not be executed."); //optional **
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				flush();
			}
		});
	}

	public void setTransport(Transport _transport){
		transport = _transport;
	}

	public void setInterval(int interval){
		this.interval = interval;
	}

	public void setTimeUnit(TimeUnit unit){
		this.unit = unit;
	}

	private BucketBuffer buffer = new BucketBuffer();

	public <T extends Bucket> boolean emit(
			Class<T> clazz,
			String bucketname,
			int value,
			String message){
		buffer.brew(clazz, bucketname, value, message);
		synchronized(executor){
			if (!(executor.isShutdown() || executor.isTerminated()) 
				&& (progress.isDone() || progress == null)){

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
			Map<String, Bucket> dumpcellar = buffer.dump();
			for (Bucket bucket : dumpcellar.values()){
				transport.doSend(bucket.toString());
			}
		}
	}

	GeyserStrategy(){
		this.initialize();
	}
}
