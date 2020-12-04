package com.sraft.client;

import java.util.Date;

import com.sraft.common.DateHelper;
import com.sraft.common.IdGenerateHelper;
import com.sraft.core.message.ClientActionMsg;
import com.sraft.core.message.Msg;
import com.sraft.core.message.Packet;
import com.sraft.core.message.ReplyClientActionMsg;

public class SimpleRaftClient implements IClientTransaction {

	private ClientConnManager clientConnManager = null;

	public SimpleRaftClient(String address) throws Exception {
		clientConnManager = new ClientConnManager(address);
		clientConnManager.isUseFullSyn();
	}

	@Override
	public void add(String key, String value) {
		clientConnManager.isUseFullSyn();
		Packet packet = getPacket(TYPE_ACTION_ADD, key, value);
		clientConnManager.sendActionMsg(packet);
		synchronized (packet) {
			try {
				packet.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		ReplyClientActionMsg replyClientActionMsg = (ReplyClientActionMsg) packet.getReplyMsg();
	}

	@Override
	public void update(String key, String value) {
		// TODO Auto-generated method stub

	}

	@Override
	public void remove(String key) {
		// TODO Auto-generated method stub

	}

	@Override
	public String get(String key) {
		// TODO Auto-generated method stub
		return null;
	}

	private Packet getPacket(int actionType, String key, String value) {
		Packet packet = new Packet();
		ClientActionMsg clientActionMsg = new ClientActionMsg();
		clientActionMsg.setActionType(actionType);
		clientActionMsg.setKey(key);
		clientActionMsg.setMsgId(IdGenerateHelper.getMsgId());
		clientActionMsg.setMsgType(Msg.TYPE_CLIENT_ACTION);
		clientActionMsg.setSendTime(DateHelper.formatDate2Long(new Date(), DateHelper.YYYYMMDDHHMMSSsss));
		clientActionMsg.setSessionId(clientConnManager.getSessionId());
		clientActionMsg.setTransactionId(IdGenerateHelper.getNextSessionId());
		packet.setSendMsg(clientActionMsg);
		switch (actionType) {
		case TYPE_ACTION_ADD:
			clientActionMsg.setValue(value);
			break;
		case TYPE_ACTION_UPDATE:
			clientActionMsg.setValue(value);
			break;
		case TYPE_ACTION_REMOVE:
			break;
		case TYPE_ACTION_GET:
			break;
		default:
			break;
		}
		return packet;
	}
}
