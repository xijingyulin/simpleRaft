package com.sraft.core.message;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.msgpack.annotation.Message;

import com.sraft.core.session.Session;

@Message
public class AppendSnapshotMsg extends ServerMsg {
	private long transactionId;
	private long prevSnapIndex;
	private long prevSnapTerm;
	List<BaseSnapshot> baseSnapshot;
	private Map<Long, Session> sessionMap = new HashMap<Long, Session>();

	public long getPrevSnapIndex() {
		return prevSnapIndex;
	}

	public void setPrevSnapIndex(long prevSnapIndex) {
		this.prevSnapIndex = prevSnapIndex;
	}

	public long getPrevSnapTerm() {
		return prevSnapTerm;
	}

	public void setPrevSnapTerm(long prevSnapTerm) {
		this.prevSnapTerm = prevSnapTerm;
	}

	public long getSendTime() {
		return sendTime;
	}

	public void setSendTime(long sendTime) {
		this.sendTime = sendTime;
	}

	public List<BaseSnapshot> getBaseSnapshot() {
		return baseSnapshot;
	}

	public void setBaseSnapshot(List<BaseSnapshot> baseSnapshot) {
		this.baseSnapshot = baseSnapshot;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("transactionId:");
		builder.append(transactionId);
		builder.append(",prevSnapIndex:");
		builder.append(prevSnapIndex);
		builder.append(",prevSnapTerm:");
		builder.append(prevSnapTerm);
		builder.append(",baseSnapshot:");
		builder.append(baseSnapshot);
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

	public long getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(long transactionId) {
		this.transactionId = transactionId;
	}

	public Map<Long, Session> getSessionMap() {
		return sessionMap;
	}

	public void setSessionMap(Map<Long, Session> sessionMap) {
		this.sessionMap = sessionMap;
	}

}
