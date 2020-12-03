package com.sraft.core.message;

import org.msgpack.annotation.Message;

@Message
public class ClientActionMsg extends ClientMsg {
	private long transactionId;
	private String key;
	private String value;
	private int actionType;

	public ClientActionMsg() {

	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public int getActionType() {
		return actionType;
	}

	public void setActionType(int actionType) {
		this.actionType = actionType;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("transactionId:");
		builder.append(transactionId);
		builder.append(",key:");
		builder.append(key);
		builder.append(",value:");
		builder.append(value);
		builder.append(",actionType:");
		builder.append(actionType);
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

	public long getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(long transactionId) {
		this.transactionId = transactionId;
	}

}
