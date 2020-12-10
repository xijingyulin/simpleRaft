package com.sraft.core.session;

import org.msgpack.annotation.Message;

@Message
public class Session {

	/**
	 * 客户端会话ID，由领导者分配
	 */
	private long sessionId;
	/**
	 * 领导者最新接收到客户端消息的时间
	 */
	private long lastReceiveTime;
	/**
	 * 客户端操作最新事务Id
	 */
	private long lastClientTransactionId = -1;

	public Session() {

	}

	public Session(long sessionId, long lastReceiveTime, long lastClientTransactionId) {
		super();
		this.sessionId = sessionId;
		this.lastReceiveTime = lastReceiveTime;
		this.lastClientTransactionId = lastClientTransactionId;
	}

	public long getSessionId() {
		return sessionId;
	}

	public void setSessionId(long sessionId) {
		this.sessionId = sessionId;
	}

	public long getLastReceiveTime() {
		return lastReceiveTime;
	}

	public void setLastReceiveTime(long lastReceiveTime) {
		if (lastReceiveTime > this.lastReceiveTime) {
			this.lastReceiveTime = lastReceiveTime;
		}
	}

	public long getLastClientTransactionId() {
		return lastClientTransactionId;
	}

	public void setLastClientTransactionId(long lastClientTransactionId) {
		this.lastClientTransactionId = lastClientTransactionId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("sessionId:");
		builder.append(sessionId);
		builder.append(",lastReceiveTime:");
		builder.append(lastReceiveTime);
		builder.append(",lastClientTransactionId:");
		builder.append(lastClientTransactionId);
		return builder.toString();
	}

}
