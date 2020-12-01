package com.sraft.enums;

public enum EnumLoginStatus {

	OK(1), FALSE(2);

	private final int initValue;

	EnumLoginStatus(int initValue) {
		this.initValue = initValue;
	}

	public int getValue() {
		return this.initValue;
	}
}
