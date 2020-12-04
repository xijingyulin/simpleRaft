package com.sraft.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sraft.common.flow.IFlowWorker;
import com.sraft.core.message.ClientActionMsg;
import com.sraft.core.message.Packet;
import com.sraft.core.net.ConnManager;

public class ClientSenderWorker implements IFlowWorker {
	private static Logger LOG = LoggerFactory.getLogger(ClientSenderWorker.class);
	private ClientConnManager clientConnManager;

	public ClientSenderWorker(ClientConnManager clientConnManager) {
		this.clientConnManager = clientConnManager;
	}

	@Override
	public void deliver(Object object) {
		Packet packet = (Packet) object;
		ClientActionMsg clientActionMsg = (ClientActionMsg) packet.getSendMsg();
		synchronized (clientConnManager.getPendingQueue()) {
			boolean isSuccess = ConnManager.getInstance().sendMsg(AddrManager.getInstance().getLeaderConn(),
					clientActionMsg);
			if (isSuccess) {
				LOG.info("事务消息发送成功:{}", clientActionMsg.toString());
				clientConnManager.getPendingQueue().add(packet);
			} else {
				LOG.info("事务消息发送失败:{}", clientActionMsg.toString());
				// 构造失败响应，并且packet.notify
			}
		}
	}
}
