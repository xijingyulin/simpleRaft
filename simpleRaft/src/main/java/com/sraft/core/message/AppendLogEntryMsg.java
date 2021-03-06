package com.sraft.core.message;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.msgpack.annotation.Message;

import com.sraft.core.session.Session;

@Message
public class AppendLogEntryMsg extends ServerMsg {
	private long taskId;
	private long transactionId;
	private int appendType;
	private long prevLogIndex;
	private long prevLogTerm;
	private long leaderCommit;
	private List<BaseLog> baseLogList;
	private Map<Long, Session> sessionMap = new HashMap<Long, Session>();
	/**
	 * 空日志
	 */
	public static final int TYPE_APPEND_NULL = 1;
	/**
	 * 同步日志
	 */
	public static final int TYPE_APPEND_SYN = 2;
	/**
	 * 普通追加日志
	 */
	public static final int TYPE_APPEND_ORDINARY = 3;

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
		builder.append("taskId:");
		builder.append(taskId);
		builder.append(",transactionId:");
		builder.append(transactionId);
		builder.append(",appendType:");
		builder.append(appendType);
		builder.append(",prevLogIndex:");
		builder.append(prevLogIndex);
		builder.append(",prevLogTerm:");
		builder.append(prevLogTerm);
		builder.append(",leaderCommit:");
		builder.append(leaderCommit);
		builder.append(",baseLogList:");
		builder.append(baseLogList);
		builder.append(",sessionMap:");
		builder.append(sessionMap);
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

	public Map<Long, Session> getSessionMap() {
		return sessionMap;
	}

	public void setSessionMap(Map<Long, Session> sessionMap) {
		this.sessionMap = sessionMap;
	}

	public long getTaskId() {
		return taskId;
	}

	public void setTaskId(long taskId) {
		this.taskId = taskId;
	}

}
