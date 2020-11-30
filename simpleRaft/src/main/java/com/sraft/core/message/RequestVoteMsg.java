package com.sraft.core.message;

import org.msgpack.annotation.Message;

@Message
public class RequestVoteMsg extends ServerMsg {

	private long lastLogIndex;
	private long lastLogTerm;

	public RequestVoteMsg() {

	}

	public long getLastLogIndex() {
		return lastLogIndex;
	}

	public void setLastLogIndex(long lastLogIndex) {
		this.lastLogIndex = lastLogIndex;
	}

	public long getLastLogTerm() {
		return lastLogTerm;
	}

	public void setLastLogTerm(long lastLogTerm) {
		this.lastLogTerm = lastLogTerm;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("lastLogIndex:");
		builder.append(lastLogIndex);
		builder.append(",lastLogTerm:");
		builder.append(lastLogTerm);
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
