package com.appfirst.statsd.strategy;

public class BucketTypeMismatchException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public BucketTypeMismatchException(){
		super();
	}

	public BucketTypeMismatchException(String message){
		super(message);
    }
}
