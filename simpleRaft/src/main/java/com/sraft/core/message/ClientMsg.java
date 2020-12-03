package com.sraft.core.message;

import org.msgpack.annotation.Message;

@Message
public class ClientMsg extends Msg {

	/**
	 * 会话ID，具备全局唯一性
	 */
	protected long sessionId;

	public ClientMsg() {

	}

	public long getSessionId() {
		return sessionId;
	}

	public void setSessionId(long sessionId) {
		this.sessionId = sessionId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("sessionId:");
		builder.append(sessionId);
		builder.append(",msgType:");
		builder.append(msgType);
		builder.append(",msgId:");
		builder.append(msgId);
		builder.append(",sendTime:");
		builder.append(sendTime);
		builder.append(",receviceTime:");
		builder.append(receviceTime);
		return builder.toString();
	}

}
