package com.appfirst.statsd.strategy;

public final class StrategyFactory {

	private StrategyFactory(){
		
	}

	public static GeyserStrategy getGeyserStrategy(){
		return GeyserStrategy.getSingleton();
	}

	public static GeyserStrategy getGeyserStrategy(int interval){
		GeyserStrategy instance = GeyserStrategy.getSingleton();
		instance.setInterval(interval);
		return instance;
	}

	public static InstantStrategy getInstantStrategy(){
		return new InstantStrategy();
	}
}
