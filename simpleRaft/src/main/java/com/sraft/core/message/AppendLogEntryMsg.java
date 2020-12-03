package com.sraft.core.message;

import java.util.List;

import org.msgpack.annotation.Message;

@Message
public class AppendLogEntryMsg extends ServerMsg {
	private long transactionId;
	private long prevLogIndex;
	private long prevLogTerm;
	private long leaderCommit;
	private List<BaseLog> baseLogList;

	public long getPrevLogIndex() {
		return prevLogIndex;
	}

	public void setPrevLogIndex(long prevLogIndex) {
		this.prevLogIndex = prevLogIndex;
	}

	public long getPrevLogTerm() {
		return prevLogTerm;
	}

	public void setPrevLogTerm(long prevLogTerm) {
		this.prevLogTerm = prevLogTerm;
	}

	public long getLeaderCommit() {
		return leaderCommit;
	}

	public void setLeaderCommit(long leaderCommit) {
		this.leaderCommit = leaderCommit;
	}

	public long getSendTime() {
		return sendTime;
	}

	public void setSendTime(long sendTime) {
		this.sendTime = sendTime;
	}

	public List<BaseLog> getBaseLogList() {
		return baseLogList;
	}

	public void setBaseLogList(List<BaseLog> baseLogList) {
		this.baseLogList = baseLogList;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("msgId:");
		builder.append(msgId);
		builder.append(",term:");
		builder.append(term);
		builder.append(",prevLogIndex:");
		builder.append(prevLogIndex);
		builder.append(",prevLogTerm:");
		builder.append(prevLogTerm);
		builder.append(",leaderCommit:");
		builder.append(leaderCommit);
		builder.append(",sendTime:");
		builder.append(sendTime);
		builder.append(",baseLogList:");
		builder.append(baseLogList);
		return builder.toString();
	}

}
