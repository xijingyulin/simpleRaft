package com.sraft.client;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sraft.client.exception.KeyNullException;
import com.sraft.client.exception.UnavailableException;
import com.sraft.client.exception.ValueNullException;
import com.sraft.common.DateHelper;
import com.sraft.common.IdGenerateHelper;
import com.sraft.common.StringHelper;
import com.sraft.core.log.LogData;
import com.sraft.core.message.ClientActionMsg;
import com.sraft.core.message.Msg;
import com.sraft.core.message.Packet;
import com.sraft.core.message.ReplyClientActionMsg;

public class SimpleRaftClient implements IClientTransaction {
	private static Logger LOG = LoggerFactory.getLogger(SimpleRaftClient.class);
	private ClientConnManager clientConnManager = null;
	/**
	 * 当返回Msg.ERR_CODE_LOG_APPEND_FALSE错误码时，重试次数
	 */
	private int reTryNum = 3;

	public SimpleRaftClient(String address) throws Exception {
		clientConnManager = new ClientConnManager(address);
		clientConnManager.isUseFullSyn();
	}

	public SimpleRaftClient(String address, int reTryNum) throws Exception {
		clientConnManager = new ClientConnManager(address);
		clientConnManager.isUseFullSyn();
		this.reTryNum = reTryNum;
	}

	@Override
	public ActionResult put(String key, String value)
			throws UnavailableException, KeyNullException, ValueNullException {
		if (!StringHelper.checkIsNotNull(key)) {
			throw new KeyNullException();
		}
		if (!StringHelper.checkIsNotNull(value)) {
			throw new ValueNullException();
		}
		Packet packet = getPacket(LogData.LOG_PUT, key, value, null);
		return sendMsg(packet, LogData.LOG_PUT);

	}

	@Override
	public ActionResult update(String key, String value)
			throws UnavailableException, KeyNullException, ValueNullException {
		if (!StringHelper.checkIsNotNull(key)) {
			throw new KeyNullException();
		}
		if (!StringHelper.checkIsNotNull(value)) {
			throw new ValueNullException();
		}
		Packet packet = getPacket(LogData.LOG_UPDATE, key, value, null);
		return sendMsg(packet, LogData.LOG_UPDATE);
	}

	@Override
	public ActionResult remove(String key) throws UnavailableException, KeyNullException {
		if (!StringHelper.checkIsNotNull(key)) {
			throw new KeyNullException();
		}

		Packet packet = getPacket(LogData.LOG_REMOVE, key, "", null);
		///////////
		//		for (int i = 0; i < 10000; i++) {
		//			sendMsg(packet, LogData.LOG_REMOVE);
		//		}	
		//		return sendMsg(packet, LogData.LOG_REMOVE);
		/////////////
		return sendMsg(packet, LogData.LOG_REMOVE);
	}

	@Override
	public ActionResult get(String key) throws UnavailableException, KeyNullException {
		if (!StringHelper.checkIsNotNull(key)) {
			throw new KeyNullException();
		}
		Packet packet = getPacket(LogData.LOG_GET, key, "", null);
		return sendMsg(packet, LogData.LOG_GET);
	}

	private Packet getPacket(int actionType, String key, String value, IDataCallBack callBack) {
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
		packet.setCall(callBack);
		switch (actionType) {
		case LogData.LOG_PUT:
			clientActionMsg.setValue(value);
			break;
		case LogData.LOG_UPDATE:
			clientActionMsg.setValue(value);
			break;
		case LogData.LOG_REMOVE:
			break;
		case LogData.LOG_GET:
			break;
		default:
			break;
		}
		return packet;
	}

	private ActionResult sendMsg(Packet packet, int actionType) throws UnavailableException {
		int failCount = reTryNum;
		ActionResult actionResult = new ActionResult();
		actionResult.setActionType(actionType);
		while (true) {
			//			if (!clientConnManager.isUseFull()) {
			//				LOG.error("集群不可用，发送失败");
			//				throw new UnavailableException();
			//			}
			clientConnManager.isUseFullSyn();
			clientConnManager.sendActionMsg(packet);
			synchronized (packet) {
				try {
					packet.wait(1000);
					packet.setFinish(true);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			if (packet.getReplyMsg() == null) {
				clientConnManager.fillReply(packet);
			}
			ReplyClientActionMsg replyClientActionMsg = (ReplyClientActionMsg) packet.getReplyMsg();
			int result = replyClientActionMsg.getResult();
			if (result == Msg.RETURN_STATUS_OK) {
				actionResult.setStatus(result);
				actionResult.setValue(replyClientActionMsg.getValue());
				break;
			} else {
				int errCode = replyClientActionMsg.getErrCode();
				if (errCode == Msg.ERR_CODE_LOG_APPEND_FALSE) {
					failCount--;
				} else {
					actionResult.setStatus(result);
					actionResult.setErrCode(errCode);
					break;
				}
				if (failCount == 0) {
					actionResult.setStatus(result);
					actionResult.setErrCode(errCode);
					break;
				}
			}
		}
		return actionResult;
	}

	@Override
	public void put(String key, String value, IDataCallBack callBack)
			throws UnavailableException, KeyNullException, ValueNullException {
		if (!StringHelper.checkIsNotNull(key)) {
			throw new KeyNullException();
		}
		if (!StringHelper.checkIsNotNull(value)) {
			throw new ValueNullException();
		}
		Packet packet = getPacket(LogData.LOG_PUT, key, value, callBack);
		sendMsgAsyn(packet);
	}

	@Override
	public void update(String key, String value, IDataCallBack callBack)
			throws UnavailableException, KeyNullException, ValueNullException {
		if (!StringHelper.checkIsNotNull(key)) {
			throw new KeyNullException();
		}
		if (!StringHelper.checkIsNotNull(value)) {
			throw new ValueNullException();
		}
		Packet packet = getPacket(LogData.LOG_UPDATE, key, value, callBack);
		sendMsgAsyn(packet);
	}

	@Override
	public void remove(String key, IDataCallBack callBack) throws UnavailableException, KeyNullException {
		if (!StringHelper.checkIsNotNull(key)) {
			throw new KeyNullException();
		}

		Packet packet = getPacket(LogData.LOG_REMOVE, key, "", callBack);
		///////////
		//		for (int i = 0; i < 10000; i++) {
		//			sendMsg(packet, LogData.LOG_REMOVE);
		//		}	
		//		return sendMsg(packet, LogData.LOG_REMOVE);
		/////////////
		sendMsgAsyn(packet);
	}

	@Override
	public void get(String key, IDataCallBack callBack) throws UnavailableException, KeyNullException {
		if (!StringHelper.checkIsNotNull(key)) {
			throw new KeyNullException();
		}
		Packet packet = getPacket(LogData.LOG_GET, key, "", callBack);
		sendMsgAsyn(packet);
	}

	public void sendMsgAsyn(Packet packet) {
		clientConnManager.isUseFullSyn();
		clientConnManager.sendActionMsg(packet);
	}
}
