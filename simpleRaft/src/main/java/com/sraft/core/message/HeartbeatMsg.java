package com.sraft.core.message;

import java.util.HashMap;
import java.util.Map;

import org.msgpack.annotation.Message;

import com.sraft.core.session.Session;

@Message
public class HeartbeatMsg extends ServerMsg {
	private long leaderCommit;
	private Map<Long, Session> sessionMap = new HashMap<Long, Session>();

	public HeartbeatMsg() {

	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("commitStatus:");
		builder.append(",leaderCommit:");
		builder.append(leaderCommit);
		builder.append(",sessionMap:");
		builder.append(sessionMap);
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

	public Map<Long, Session> getSessionMap() {
		return sessionMap;
	}

	public void setSessionMap(Map<Long, Session> sessionMap) {
		this.sessionMap = sessionMap;
	}

	public long getLeaderCommit() {
		return leaderCommit;
	}

	public void setLeaderCommit(long leaderCommit) {
		this.leaderCommit = leaderCommit;
	}
}
