package com.sraft.core.message;

import org.msgpack.annotation.Message;

@Message
public class ReplyAppendLogEntryMsg extends ServerReply {
	private long transactionId;
	private int appendType;

	public ReplyAppendLogEntryMsg() {

	}

	public long getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(long transactionId) {
		this.transactionId = transactionId;
	}

	public int getAppendType() {
		return appendType;
	}

	public void setAppendType(int appendType) {
		this.appendType = appendType;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("transactionId:");
		builder.append(transactionId);
		builder.append(",appendType:");
		builder.append(appendType);
		builder.append(",result:");
		builder.append(result);
		builder.append(",errCode:");
		builder.append(errCode);
		builder.append(",remark:");
		builder.append(remark);
		builder.append(",nodeId:");
		builder.append(nodeId);
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
