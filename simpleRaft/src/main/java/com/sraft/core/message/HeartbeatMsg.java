package com.sraft.core.message;

import org.msgpack.annotation.Message;

@Message
public class HeartbeatMsg extends ServerMsg {

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("nodeId:");
		builder.append(nodeId);
		builder.append(",term:");
		builder.append(term);
		builder.append(",msgType:");
		builder.append(msgType);
		builder.append(",msgId:");
		builder.append(msgId);
		builder.append(",sendTime:");
		builder.append(sendTime);
		return builder.toString();
	}

}
