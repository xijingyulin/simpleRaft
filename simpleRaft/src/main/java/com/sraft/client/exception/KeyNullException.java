package com.sraft.client.exception;

public class KeyNullException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public KeyNullException() {
		super("key不允许为空");
	}
}
