package com.sraft.core.message;

import java.util.Arrays;

import org.msgpack.annotation.Message;

@Message
public class BaseSnapshot {
	/**
	 * 索引ID-8个字节
	 */
	protected long logIndex;

	/**
	 * 任期号-8个字节
	 */
	protected long logTerm;

	/**
	 * key值
	 */
	protected byte[] bKey;

	/**
	 * value
	 */
	protected byte[] bValue;

	public long getLogIndex() {
		return logIndex;
	}

	public void setLogIndex(long logIndex) {
		this.logIndex = logIndex;
	}

	public long getLogTerm() {
		return logTerm;
	}

	public void setLogTerm(long logTerm) {
		this.logTerm = logTerm;
	}

	public byte[] getbKey() {
		return bKey;
	}

	public void setbKey(byte[] bKey) {
		this.bKey = bKey;
	}

	public byte[] getbValue() {
		return bValue;
	}

	public void setbValue(byte[] bValue) {
		this.bValue = bValue;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("logIndex:");
		builder.append(logIndex);
		builder.append(",logTerm:");
		builder.append(logTerm);
		builder.append(",bKey:");
		builder.append(Arrays.toString(bKey));
		builder.append(",bValue:");
		builder.append(Arrays.toString(bValue));
		return builder.toString();
	}
}
