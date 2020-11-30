package com.sraft.core.log;

import java.util.Arrays;

import com.sraft.core.message.BaseSnapshot;

public class Snapshot extends BaseSnapshot {
	/**
	 * 内容总长度-8个字节；整行数据总长度
	 */
	private long logLength;

	/**
	 * key长度-4个字节
	 */
	private int keyLength;

	/**
	 * value长度-4个字节
	 */
	private int valueLength;

	public static final int FIXED_BYTE_LENGTH = 32;

	public Snapshot() {

	}

	public long getLogLength() {
		return logLength;
	}

	public void setLogLength(long logLength) {
		this.logLength = logLength;
	}

	public int getKeyLength() {
		return keyLength;
	}

	public void setKeyLength(int keyLength) {
		this.keyLength = keyLength;
	}

	public int getValueLength() {
		return valueLength;
	}

	public void setValueLength(int valueLength) {
		this.valueLength = valueLength;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("logLength:");
		builder.append(logLength);
		builder.append(",keyLength:");
		builder.append(keyLength);
		builder.append(",valueLength:");
		builder.append(valueLength);
		builder.append(",logIndex:");
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
