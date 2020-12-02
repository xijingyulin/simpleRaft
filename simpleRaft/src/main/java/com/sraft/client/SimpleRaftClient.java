package com.sraft.client;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sraft.common.DateHelper;
import com.sraft.common.IdGenerateHelper;
import com.sraft.common.flow.FlowHeader;
import com.sraft.core.message.LoginMsg;
import com.sraft.core.message.Msg;
import com.sraft.core.net.ConnManager;
import com.sraft.core.net.ServerAddress;
import com.sraft.core.role.RoleController;
import com.sraft.core.role.worker.LoginWorker;
import com.sraft.enums.EnumLoginStatus;

import io.netty.channel.Channel;

public class SimpleRaftClient {
	private static Logger LOG = LoggerFactory.getLogger(SimpleRaftClient.class);
	private volatile long sessionId = -1;
	private volatile EnumLoginStatus loginStatus = EnumLoginStatus.FALSE;
	public static final int CLIENT_HEARTBEAT_INTERVAL = 200;

	public SimpleRaftClient(String address) {
		try {
			// 解析地址
			AddrManager.getInstance().explainAddr(address);
		} catch (Exception e) {
			e.printStackTrace();
			LOG.error(e.getMessage());
			System.exit(0);
		}
	}

	public void run() {
		addWorker();
		sendLoginMsg();
	}

	private void addWorker() {
		FlowHeader.employ(RoleController.LOGIN_WORKER, new LoginWorker(this));
	}

	public void sendLoginMsg() {
		ServerAddress serverAddress = AddrManager.getInstance().nextAddr();
		Channel channel = ConnManager.getInstance().connect(serverAddress);
		if (channel == null) {
			LOG.error("连接服务器失败:{}", serverAddress.toString());
			sendLoginMsg();
		} else {
			LOG.info("连接服务器成功:{}", serverAddress.toString());
			LoginMsg loginMsg = getLoginMsg();
			ConnManager.getInstance().sendMsg(serverAddress, loginMsg);
		}
	}

	private LoginMsg getLoginMsg() {
		LoginMsg msg = new LoginMsg();
		msg.setMsgId(IdGenerateHelper.getMsgId());
		msg.setMsgType(Msg.TYPE_CLIENT_LOGIN);
		msg.setSendTime(DateHelper.formatDate2Long(new Date(), DateHelper.YYYYMMDDHHMMSSsss));
		msg.setSessionId(sessionId);
		return msg;
	}

	public void updateLoginStatus(EnumLoginStatus newStatus) {
		synchronized (EnumLoginStatus.class) {
			loginStatus = newStatus;
		}
	}

	public synchronized void updateSessionId(long newSession) {
		if (newSession != sessionId) {
			sessionId = newSession;
		}
	}
}
