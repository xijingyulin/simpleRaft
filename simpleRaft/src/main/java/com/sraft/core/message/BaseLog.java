package com.sraft.core.message;

import org.msgpack.annotation.Message;

@Message
public class BaseLog {

	/**
	 * 索引ID-8个字节
	 */
	protected long logIndex;
	/**
	 * 任期号
	 */
	protected long logTerm;
	/**
	 * 日志类型-4个字节
	 * 
	 * 1：新增日志
	 * 
	 * 2：更新
	 * 
	 * 3：删除
	 * 
	 * 4：读
	 * 
	 * 5：空
	 */
	protected int logType;

	/**
	 * 领导者ID-4个字节
	 */
	protected int leaderId;
	/**
	 * 客户端会话ID-8个字节，只有客户端发起的命令才有，
	 * 
	 * 正常日志都是客户端发起的，除了追加空日志
	 */
	protected long clientSessionId;
	/**
	 * 客户端事务ID-8个字节，只有客户端发起的命令才有，
	 */
	protected long clientTransactionId;

	/**
	 * 集群事务ID-8个字节，全局唯一，领导者生成
	 */
	protected long sraftTransactionId;

	/**
	 * 创建时间-8个字节
	 */
	protected long createTime;
	/**
	 * 修改时间-8个字节
	 */
	protected long updateTime;
	/**
	 * key值
	 */

	protected String key = "";
	/**
	 * value
	 */
	protected String value = "";

	public long getLogIndex() {
		return logIndex;
	}

	public void setLogIndex(long logIndex) {
		this.logIndex = logIndex;
	}

	public int getLogType() {
		return logType;
	}

	public void setLogType(int logType) {
		this.logType = logType;
	}

	public int getLeaderId() {
		return leaderId;
	}

	public void setLeaderId(int leaderId) {
		this.leaderId = leaderId;
	}

	public long getClientSessionId() {
		return clientSessionId;
	}

	public void setClientSessionId(long clientSessionId) {
		this.clientSessionId = clientSessionId;
	}

	public long getClientTransactionId() {
		return clientTransactionId;
	}

	public void setClientTransactionId(long clientTransactionId) {
		this.clientTransactionId = clientTransactionId;
	}

	public long getCreateTime() {
		return createTime;
	}

	public void setCreateTime(long createTime) {
		this.createTime = createTime;
	}

	public long getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(long updateTime) {
		this.updateTime = updateTime;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("logIndex:");
		builder.append(logIndex);
		builder.append(",logType:");
		builder.append(logType);
		builder.append(",leaderId:");
		builder.append(leaderId);
		builder.append(",clientSessionId:");
		builder.append(clientSessionId);
		builder.append(",clientTransactionId:");
		builder.append(clientTransactionId);
		builder.append(",createTime:");
		builder.append(createTime);
		builder.append(",updateTime:");
		builder.append(updateTime);
		builder.append(",key:");
		builder.append(key);
		builder.append(",value:");
		builder.append(value);
		builder.append(",logTerm:");
		builder.append(logTerm);
		builder.append(",sraftTransactionId:");
		builder.append(sraftTransactionId);
		return builder.toString();
	}

	public long getLogTerm() {
		return logTerm;
	}

	public void setLogTerm(long logTerm) {
		this.logTerm = logTerm;
	}

	public long getSraftTransactionId() {
		return sraftTransactionId;
	}

	public void setSraftTransactionId(long sraftTransactionId) {
		this.sraftTransactionId = sraftTransactionId;
	}
}
