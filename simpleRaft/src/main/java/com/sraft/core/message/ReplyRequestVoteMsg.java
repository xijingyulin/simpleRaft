package com.sraft.core.message;

import org.msgpack.annotation.Message;

@Message
public class ReplyRequestVoteMsg extends ServerMsg {

	private int result;

	public ReplyRequestVoteMsg() {

	}

	public int getResult() {
		return result;
	}

	public void setResult(int result) {
		this.result = result;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("result:");
		builder.append(result);
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
		return builder.toString();
	}

}
