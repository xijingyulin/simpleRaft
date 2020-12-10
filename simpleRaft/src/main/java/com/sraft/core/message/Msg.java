package com.sraft.core.message;

import org.msgpack.annotation.Message;

@Message
public class Msg {
	/**
	 * 消息类型
	 */
	protected int msgType;
	/**
	 * 消息ID，发送端生成，不具备全局唯一性
	 */
	protected long msgId;
	/**
	 * 发送者发送时间
	 */
	protected long sendTime;
	/**
	 * 接收者接收到的时间
	 */
	protected long receviceTime;
	// 执行结果
	public static final int RETURN_STATUS_OK = 1;
	public static final int RETURN_STATUS_FALSE = 2;

	// 消息类型
	public static final int TYPE_REQUEST_VOTE = 1;
	public static final int TYPE_REPLY_REQUEST_VOTE = 2;
	public static final int TYPE_HEARTBEAT = 3;
	public static final int TYPE_REPLY_HEARTBEAT = 4;
	public static final int TYPE_CLIENT_LOGIN = 5;
	public static final int TYPE_REPLY_CLIENT_LOGIN = 6;
	public static final int TYPE_CLIENT_HEARTBEAT = 7;
	public static final int TYPE_REPLY_CLIENT_HEARTBEAT = 8;
	public static final int TYPE_CLIENT_ACTION = 9;
	public static final int TYPE_REPLY_CLIENT_ACTION = 10;
	public static final int TYPE_APPEND_LOG = 11;
	public static final int TYPE_REPLY_APPEND_LOG = 12;
	public static final int TYPE_APPEND_SNAPSHOT = 13;
	public static final int TYPE_REPLY_APPEND_SNAPSHOT = 14;

	// 客户端交互异常码
	public static final int ERR_CODE_LOGIN_FOLLOWER = 1;
	public static final int ERR_CODE_LOGIN_CANDIDATE = 2;
	/**
	 * 该种情况下，不需要重新登录，但禁用服务，客户端不可用
	 */
	public static final int ERR_CODE_LOGIN_LEADER_NO_MAJOR = 3;
	/**
	 * 会话过期，需要重新登录
	 */
	public static final int ERR_CODE_SESSION_TIMEOUT = 4;
	/**
	 * 角色已转换
	 */
	public static final int ERR_CODE_ROLE_CHANGED = 5;

	// 领导者追加日志异常码
	/**
	 * 一致性检查失败，递减索引，再次发送
	 */
	public static final int ERR_CODE_LOG_CHECK_FALSE = 6;
	/**
	 * 空服务器，优先批量发送快照，再批量发送日志
	 */
	public static final int ERR_CODE_LOG_NULL_SERVER = 7;
	/**
	 * 接收端任期更大，领导者需要转换成跟随者，丢弃消息
	 */
	public static final int ERR_CODE_LOG_LARGE_TERM = 8;

	/**
	 * 接收端是候选者，更新为NODE_LOG_UNSYN（未同步）
	 */
	public static final int ERR_CODE_LOG_CANDIDATE = 9;
	/**
	 * 接收端是领导者，更新为NODE_LOG_UNSYN（未同步）
	 */
	public static final int ERR_CODE_LOG_LEADER = 10;

	/**
	 * 日志追加失败，一般由于过半节点宕机，或者内部异常引起
	 */
	public static final int ERR_CODE_LOG_APPEND_FALSE = 11;

	public Msg() {

	}

	public long getMsgId() {
		return msgId;
	}

	public void setMsgId(long msgId) {
		this.msgId = msgId;
	}

	public long getSendTime() {
		return sendTime;
	}

	public void setSendTime(long sendTime) {
		this.sendTime = sendTime;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("msgType:");
		builder.append(msgType);
		builder.append(",msgId:");
		builder.append(msgId);
		builder.append(",sendTime:");
		builder.append(sendTime);
		return builder.toString();
	}

	public int getMsgType() {
		return msgType;
	}

	public void setMsgType(int msgType) {
		this.msgType = msgType;
	}

	public long getReceviceTime() {
		return receviceTime;
	}

	public void setReceviceTime(long receviceTime) {
		this.receviceTime = receviceTime;
	}

}
