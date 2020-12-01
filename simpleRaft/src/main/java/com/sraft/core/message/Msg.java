package com.sraft.core.message;

import org.msgpack.annotation.Message;

@Message
public class Msg {
	protected int msgType;
	protected long msgId;
	/**
	 * 发送者发送时间
	 */
	protected long sendTime;
	/**
	 * 接收者接收到的时间
	 */
	protected long receviceTime;

	public static final int RETURN_STATUS_OK = 1;
	public static final int RETURN_STATUS_FALSE = 2;

	public static final int TYPE_REQUEST_VOTE = 1;
	public static final int TYPE_REPLY_REQUEST_VOTE = 2;
	public static final int TYPE_HEARTBEAT = 3;
	public static final int TYPE_REPLY_HEARTBEAT = 4;
	public static final int TYPE_CLIENT_LOGIN = 5;
	public static final int TYPE_REPLY_CLIENT_LOGIN = 6;

	public static final int ERR_CODE_LOGIN_FOLLOWER = 1;
	public static final int ERR_CODE_LOGIN_CANDIDATE = 2;
	public static final int ERR_CODE_LOGIN_LEADER_NO_MAJOR = 3;

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
