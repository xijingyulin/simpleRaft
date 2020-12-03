package com.sraft.core.message;

import org.msgpack.annotation.Message;

@Message
public class ReplyClientActionMsg extends ClientReply {

	private int actionType;
	private String value;

	public ReplyClientActionMsg() {

	}

	public int getActionType() {
		return actionType;
	}

	public void setActionType(int actionType) {
		this.actionType = actionType;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("actionType:");
		builder.append(actionType);
		builder.append(",value:");
		builder.append(value);
		builder.append(",result:");
		builder.append(result);
		builder.append(",errCode:");
		builder.append(errCode);
		builder.append(",remark:");
		builder.append(remark);
		builder.append(",sessionId:");
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
