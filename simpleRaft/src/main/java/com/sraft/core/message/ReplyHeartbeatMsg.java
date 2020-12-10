package com.sraft.core.message;

import org.msgpack.annotation.Message;

@Message
public class ReplyHeartbeatMsg extends ServerReply {

	public ReplyHeartbeatMsg() {

	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("getResult:");
		builder.append(getResult());
		builder.append(",getErrCode:");
		builder.append(getErrCode());
		builder.append(",getRemark:");
		builder.append(getRemark());
		builder.append(",toString:");
		builder.append(super.toString());
		builder.append(",getMsgId:");
		builder.append(getMsgId());
		builder.append(",getTerm:");
		builder.append(getTerm());
		builder.append(",getSendTime:");
		builder.append(getSendTime());
		builder.append(",getNodeId:");
		builder.append(getNodeId());
		builder.append(",getMsgType:");
		builder.append(getMsgType());
		builder.append(",getReceviceTime:");
		builder.append(getReceviceTime());
		builder.append(",getClass:");
		builder.append(getClass());
		builder.append(",hashCode:");
		builder.append(hashCode());
		return builder.toString();
	}

}
