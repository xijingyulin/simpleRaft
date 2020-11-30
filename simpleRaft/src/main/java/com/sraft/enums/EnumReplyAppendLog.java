package com.sraft.enums;

public enum EnumReplyAppendLog {

	/**
	 * 空服务器，领导者收到空服务器回复，便会从头发送日志，优先发送快照
	 */
	LOG_NULL(1),

	/**
	 * 一致性检查失败
	 */
	LOG_CHECK_FALSE(2),

	/**
	 * 追加成功
	 */
	LOG_APPEND_SUCCESS(3);

	private final int initValue;

	EnumReplyAppendLog(int initValue) {
		this.initValue = initValue;
	}

	public int getValue() {
		return this.initValue;
	}
}
