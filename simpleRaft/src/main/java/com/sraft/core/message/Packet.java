package com.sraft.core.message;

public class Packet {
	// 领导者发送追加日志时，可用Map<事务ID，执行结果>,发送端进行wait，接收端syn，类型成功结果，过半则notify；并且删除该事务

	private Msg sendMsg;
	private Msg replyMsg;
	private volatile boolean isFinish = false;

	public Packet() {

	}

	public Msg getSendMsg() {
		return sendMsg;
	}

	public void setSendMsg(Msg sendMsg) {
		this.sendMsg = sendMsg;
	}

	public Msg getReplyMsg() {
		return replyMsg;
	}

	public void setReplyMsg(Msg replyMsg) {
		this.replyMsg = replyMsg;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("sendMsg:");
		builder.append(sendMsg);
		builder.append(",replyMsg:");
		builder.append(replyMsg);
		return builder.toString();
	}

	public boolean isFinish() {
		return isFinish;
	}

	public void setFinish(boolean isFinish) {
		this.isFinish = isFinish;
	}

}
