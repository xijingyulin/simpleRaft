package com.sraft.core.message;

import org.msgpack.annotation.Message;

@Message
public class ReplyLoginMsg extends ClientReply {

	public ReplyLoginMsg() {

	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("sessionId:");
		builder.append(sessionId);
		builder.append(",result:");
		builder.append(result);
		builder.append(",errCode:");
		builder.append(errCode);
		builder.append(",remark:");
		builder.append(remark);
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
