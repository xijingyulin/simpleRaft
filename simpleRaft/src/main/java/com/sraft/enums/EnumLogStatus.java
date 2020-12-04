package com.sraft.enums;

public enum EnumLogStatus {
	/**
	 * LOG_CONSISTENCY:日志一致
	 * 
	 * LOG_INCONSISTENCY:日志不一致，不发送事务消息，直到日志一致
	 */
	LOG_CONSISTENCY(1), LOG_INCONSISTENCY(2);

	private final int initValue;

	EnumLogStatus(int initValue) {
		this.initValue = initValue;
	}

	public int getValue() {
		return this.initValue;
	}
}
