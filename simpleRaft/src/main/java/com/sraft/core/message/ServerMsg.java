package com.sraft.core.message;

import org.msgpack.annotation.Message;

@Message
public class ServerMsg extends Msg {
	protected int nodeId;
	protected int leaderPort;
	protected long term;

	public int getNodeId() {
		return nodeId;
	}

	public void setNodeId(int nodeId) {
		this.nodeId = nodeId;
	}

	public int getLeaderPort() {
		return leaderPort;
	}

	public void setLeaderPort(int leaderPort) {
		this.leaderPort = leaderPort;
	}

	public long getTerm() {
		return term;
	}

	public void setTerm(long term) {
		this.term = term;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("nodeId:");
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
