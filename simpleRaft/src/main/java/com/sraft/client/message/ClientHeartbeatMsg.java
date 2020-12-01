package com.sraft.client.message;

import org.msgpack.annotation.Message;

import com.sraft.core.message.Msg;

@Message
public class ClientHeartbeatMsg extends Msg {
	private long sessionId;

	public ClientHeartbeatMsg() {

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
