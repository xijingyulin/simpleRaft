package com.sraft.core.message;

import org.msgpack.annotation.Message;

@Message
public class ReplyHeartbeatMsg extends ServerReply {

	public ReplyHeartbeatMsg() {

	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("result:");
		builder.append(result);
		builder.append(",errCode:");
		builder.append(errCode);
		builder.append(",remark:");
		builder.append(remark);
		builder.append(",nodeId:");
		builder.append(nodeId);
		builder.append(",leaderPort:");
		builder.append(leaderPort);
		builder.append(",term:");
		builder.append(term);
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
