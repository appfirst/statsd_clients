package com.appfirst.statsd;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class GeyserStrategy implements Strategy, Runnable {
	private int interval = 20;
	private TimeUnit unit = TimeUnit.SECONDS;
	private Runnable task;
	private ScheduledFuture<?> f;
	private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

	public GeyserStrategy(int interval){
		this.setInterval(interval);

		Runtime.getRuntime().addShutdownHook(new Thread(this));
	}

	public void run(){
		System.out.println("final execution");
//		executor.execute(task);
		task.run();
		if (!executor.isShutdown()){
			executor.shutdown();
	//		if (!executor.awaitTermination(SHUTDOWN_TIME)) { //optional *
	//			Logger.log("Executor did not terminate in the specified time."); //optional *
	//			List<Runnable> droppedTasks = executor.shutdownNow(); //optional **
	//			Logger.log("Executor was abruptly shut down. " + droppedTasks.size() + " tasks will not be executed."); //optional **
	//		}
		}
	}

	public void setInterval(int interval){
		this.interval = interval;
	}

	public void setTimeUnit(TimeUnit unit){
		this.unit = unit;
	}
	
	public void setTask(Runnable task){
		this.task = task;
	}

	public void process(){
		if (f == null || f.isDone()){
			System.out.println("scheduling execution");
			f = executor.schedule(task, interval, unit);
		}
	}
}
