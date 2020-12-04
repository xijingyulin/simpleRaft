package com.sraft.core.role.sender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sraft.common.flow.IFlowWorker;
import com.sraft.core.message.Msg;
import com.sraft.core.net.ConnManager;
import com.sraft.core.net.ServerAddress;
import com.sraft.core.role.Leader;

public class SendHeartbeartWorker implements IFlowWorker {
	private static Logger LOG = LoggerFactory.getLogger(SendHeartbeartWorker.class);
	private ServerAddress serverAddress;
	private Leader leader;

	public SendHeartbeartWorker(ServerAddress serverAddress, Leader leader) {
		this.serverAddress = serverAddress;
		this.leader = leader;
	}

	@Override
	public void deliver(Object object) {
		Msg msg = (Msg) object;
		boolean isSuccess = ConnManager.getInstance().sendMsg(serverAddress, msg);
		//LOG.info("消息类型【{}】,发送结果:{}", msg.getMsgType(), result);
//		if (!isSuccess) {
//			
//		}
	}

}
