package com.sraft.enums;

public enum EnumServiceStatus {

	/**
	 * 心跳正常，集群过半存活才是可用
	 */
	USEFULL(1), UN_USEFULL(2);

	private final int initValue;

	EnumServiceStatus(int initValue) {
		this.initValue = initValue;
	}

	public int getValue() {
		return this.initValue;
	}
}
