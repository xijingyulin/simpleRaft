package com.sraft.enums;

public enum EnumNodeStatus {
	/**
	 * NODE_DEAD:宕机
	 * 
	 * NODE_LOG_UNSYN:连上节点,日志未同步
	 * 
	 * NODE_LOG_SYNING:日志正在同步
	 * 
	 * NODE_NORMAL:日志同步完成，正常工作
	 */
	NODE_DEAD(1), NODE_LOG_UNSYN(2), NODE_LOG_SYNING(3), NODE_NORMAL(4);
	private final int initValue;

	EnumNodeStatus(int initValue) {
		this.initValue = initValue;
	}

	public int getValue() {
		return this.initValue;
	}
}
