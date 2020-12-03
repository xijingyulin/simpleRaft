package com.sraft.client;

import com.sraft.core.message.ClientActionMsg;
import com.sraft.core.message.ReplyClientActionMsg;

public class Packet {
	// 领导者发送追加日志时，可用Map<事务ID，执行结果>,发送端进行wait，接收端syn，类型成功结果，过半则notify；并且删除该事务

	private ClientActionMsg clientActionMsg;
	private ReplyClientActionMsg replyClientActionMsg;

	public Packet() {

	}

	public ClientActionMsg getClientActionMsg() {
		return clientActionMsg;
	}

	public void setClientActionMsg(ClientActionMsg clientActionMsg) {
		this.clientActionMsg = clientActionMsg;
	}

	public ReplyClientActionMsg getReplyClientActionMsg() {
		return replyClientActionMsg;
	}

	public void setReplyClientActionMsg(ReplyClientActionMsg replyClientActionMsg) {
		this.replyClientActionMsg = replyClientActionMsg;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("clientActionMsg:");
		builder.append(clientActionMsg);
		builder.append(",replyClientActionMsg:");
		builder.append(replyClientActionMsg);
		return builder.toString();
	}

}
