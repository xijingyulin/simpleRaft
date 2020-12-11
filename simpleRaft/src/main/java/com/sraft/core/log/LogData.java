package com.sraft.core.log;

import java.util.Arrays;

import com.sraft.core.message.BaseLog;

/**
 * 
 * [索引ID],[任期号],[内容总长度-8个字节],[日志类型],[领导者ID],[客户端会话ID],[客户端事务ID],[集群事务ID],[创建时间],[修改时间],[key长度-4个字节],[key],[value长度-4个字节],[value]
 * 
 * 
 * @author 伍尚康-4020年11月19日
 *
 */
public class LogData extends BaseLog {

	/**
	 * 内容总长度-8个字节；整行数据总长度
	 */
	private long logLength;

	/**
	 * key长度-4个字节
	 */
	private int keyLength;
	/**
	 * key值
	 */
	private byte[] bKey;

	/**
	 * value长度-4个字节
	 */
	private int valueLength;
	/**
	 * value
	 */
	private byte[] bValue;

	/**
	 * 每条日志结束的位置，也就是偏移值
	 */
	private long offset;

	public static final int FIXED_BYTE_LENGTH = 80;

	public final static int LOG_PUT = 1;
	public final static int LOG_UPDATE = 2;
	public final static int LOG_REMOVE = 3;
	/**
	 * 不需写入日志，不需要递增索引
	 */
	public final static int LOG_GET = 4;
	/**
	 * 不需写入日志
	 */
	// public final static int LOG_NULL = 5;

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

	public byte[] getbKey() {
		return bKey;
	}

	public void setbKey(byte[] bKey) {
		this.bKey = bKey;
	}

	public int getValueLength() {
		return valueLength;
	}

	public void setValueLength(int valueLength) {
		this.valueLength = valueLength;
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
		builder.append(",logType:");
		builder.append(logType);
		builder.append(",leaderId:");
		builder.append(leaderId);
		builder.append(",clientSessionId:");
		builder.append(clientSessionId);
		builder.append(",clientTransactionId:");
		builder.append(clientTransactionId);
		builder.append(",sraftTransactionId:");
		builder.append(sraftTransactionId);
		builder.append(",createTime:");
		builder.append(createTime);
		builder.append(",updateTime:");
		builder.append(updateTime);
		builder.append(",key:");
		builder.append(key);
		builder.append(",value:");
		builder.append(value);
		builder.append(",logLength:");
		builder.append(logLength);
		builder.append(",keyLength:");
		builder.append(keyLength);
		builder.append(",bKey:");
		builder.append(Arrays.toString(bKey));
		builder.append(",valueLength:");
		builder.append(valueLength);
		builder.append(",bValue:");
		builder.append(Arrays.toString(bValue));
		builder.append(",offset:");
		builder.append(offset);
		return builder.toString();
	}

	public long getOffset() {
		return offset;
	}

	public void setOffset(long offset) {
		this.offset = offset;
	}

	public BaseLog getBaseLog() {
		BaseLog baseLog = new BaseLog();
		baseLog.setClientSessionId(clientSessionId);
		baseLog.setClientTransactionId(clientTransactionId);
		baseLog.setCreateTime(createTime);
		baseLog.setKey(key);
		baseLog.setLeaderId(leaderId);
		baseLog.setLogIndex(logIndex);
		baseLog.setLogTerm(logTerm);
		baseLog.setLogType(logType);
		baseLog.setSraftTransactionId(sraftTransactionId);
		baseLog.setUpdateTime(updateTime);
		baseLog.setValue(value);
		return baseLog;
	}
}
