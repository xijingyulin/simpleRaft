package com.sraft.client.exception;

public class UnavailableException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public UnavailableException() {
		super("集群不可用");
	}
}
