package com.appfirst.statsd;

public class StrategyFactory {
	private static final GeyserStrategy singleInstance = new GeyserStrategy();

	public GeyserStrategy getGeyserStrategy(){
		return singleInstance;
	}

	public GeyserStrategy getGeyserStrategy(int interval){
		GeyserStrategy instance = singleInstance;
		instance.setInterval(interval);
		return instance;
	}
	
	public InstantStrategy getInstantStrategy(){
		return new InstantStrategy();
	}
}
