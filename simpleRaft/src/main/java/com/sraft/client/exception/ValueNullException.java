package com.sraft.client.exception;

public class ValueNullException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ValueNullException() {
		super("value不允许为空");
	}
}
