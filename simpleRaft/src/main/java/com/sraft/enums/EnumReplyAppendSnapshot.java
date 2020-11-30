package com.sraft.enums;

public enum EnumReplyAppendSnapshot {

	SNAPSHOT_APPEND_TRUE(1),

	SNAPSHOT_APPEND_FALSE(2);

	private final int initValue;

	EnumReplyAppendSnapshot(int initValue) {
		this.initValue = initValue;
	}

	public int getValue() {
		return this.initValue;
	}
}
