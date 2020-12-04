package com.sraft.core.role;

public interface ILeader {

	/**
	 * 发送心跳
	 */
	void sendHeartbeat();

	/**
	 * 发送空日志来同步日志
	 * 
	 * @param nodeId
	 */
	void sendEmptyLog(int nodeId);
}
