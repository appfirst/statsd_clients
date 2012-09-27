package com.appfirst.statsd.strategy;

public class BucketTypeMismatchException extends Exception {
	public BucketTypeMismatchException(){
		super();
	}

	public BucketTypeMismatchException(String message){
		super(message);
    }
}
