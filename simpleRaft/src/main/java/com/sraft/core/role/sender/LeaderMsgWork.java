package com.sraft.core.role.sender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sraft.common.flow.IFlowWorker;
import com.sraft.core.message.Msg;
import com.sraft.core.net.ConnManager;
import com.sraft.core.net.ServerAddress;

public class LeaderMsgWork implements IFlowWorker {
	private static Logger LOG = LoggerFactory.getLogger(LeaderMsgWork.class);
	private ServerAddress serverAddress;

	public LeaderMsgWork(ServerAddress serverAddress) {
		this.serverAddress = serverAddress;
	}

	@Override
	public void deliver(Object object) {
		Msg msg = (Msg) object;
		boolean result = ConnManager.getInstance().sendMsg(serverAddress, msg);
		//LOG.info("消息类型【{}】,发送结果:{}", msg.getMsgType(), result);
	}

}
