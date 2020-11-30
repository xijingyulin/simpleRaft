package com.sraft.core.role.sender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sraft.core.message.RequestVoteMsg;
import com.sraft.core.net.ConnManager;
import com.sraft.core.net.ServerAddress;

public class SendRequestVoteThread implements Runnable {
	private static Logger LOG = LoggerFactory.getLogger(SendRequestVoteThread.class);

	private RequestVoteMsg requestVoteMsg;
	private ServerAddress serverAddress;

	public SendRequestVoteThread(RequestVoteMsg requestVoteMsg, ServerAddress serverAddress) {
		this.requestVoteMsg = requestVoteMsg;
		this.serverAddress = serverAddress;
	}

	@Override
	public void run() {
		boolean isSuccess = ConnManager.getInstance().sendMsg(serverAddress, requestVoteMsg);
		if (isSuccess) {
			LOG.info("发送请求投票消息成功,地址:{}", serverAddress);
		} else {
			LOG.info("发送请求投票消息失败,地址:{}", serverAddress);
		}
	}

}
