package com.sraft.core.message;

import java.util.List;

import org.msgpack.annotation.Message;

@Message
public class AppendSnapshotMsg extends ServerMsg {
	private long prevSnapIndex;
	private long prevSnapTerm;
	List<BaseSnapshot> baseSnapshot;

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
		builder.append("msgId:");
		builder.append(msgId);
		builder.append(",term:");
		builder.append(term);
		builder.append(",prevSnapIndex:");
		builder.append(prevSnapIndex);
		builder.append(",prevSnapTerm:");
		builder.append(prevSnapTerm);
		builder.append(",sendTime:");
		builder.append(sendTime);
		builder.append(",baseSnapshot:");
		builder.append(baseSnapshot);
		return builder.toString();
	}
}
