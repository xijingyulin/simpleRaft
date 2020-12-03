package com.sraft.enums;

public enum EnumServiceStatus {

	USEFULL(1), UN_USEFULL(2);

	private final int initValue;

	EnumServiceStatus(int initValue) {
		this.initValue = initValue;
	}

	public int getValue() {
		return this.initValue;
	}
}
