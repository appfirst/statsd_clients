package com.appfirst.statsd;

import com.appfirst.statsd.strategy.Strategy;
import com.appfirst.statsd.transport.Transport;

public interface StatsdService extends StatsdClient{

	StatsdService setStrategy(Strategy strategy);

	Strategy getStrategy();

	Transport getTransport();

	StatsdService setTransport(Transport transport);

}