package com.sraft.client;

import java.text.ParseException;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sraft.common.DateHelper;
import com.sraft.common.IdGenerateHelper;
import com.sraft.common.flow.FlowHeader;
import com.sraft.core.message.ClientHeartbeatMsg;
import com.sraft.core.message.ClientMsg;
import com.sraft.core.message.LoginMsg;
import com.sraft.core.message.Msg;
import com.sraft.core.net.ConnManager;
import com.sraft.core.net.ServerAddress;
import com.sraft.core.role.RoleController;
import com.sraft.core.role.worker.ClientHeartbeatWorker;
import com.sraft.core.role.worker.LoginWorker;
import com.sraft.core.schedule.ScheduleClientHeartbeat;
import com.sraft.enums.EnumLoginStatus;

import io.netty.channel.Channel;

public class SimpleRaftClient {
	private static Logger LOG = LoggerFactory.getLogger(SimpleRaftClient.class);
	private volatile long sessionId = -1;
	private volatile EnumLoginStatus loginStatus = EnumLoginStatus.FALSE;
	public static final int CLIENT_HEARTBEAT_INTERVAL = 200;
	private volatile ClientMsg lastReceiveMsg = null;

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
		startClientHeartbeat();
	}

	/**
	 * 登录成功后，才能发送心跳
	 */
	public void startClientHeartbeat() {
		ScheduleClientHeartbeat.getInstance().schedule(CLIENT_HEARTBEAT_INTERVAL, this);
	}

	public void stopClientHeartbeat() {
		ScheduleClientHeartbeat.getInstance().stop();
	}

	private void addWorker() {
		FlowHeader.employ(RoleController.LOGIN_WORKER, new LoginWorker(this));
		FlowHeader.employ(RoleController.CLIENT_HEARTBEAT_WORKER, new ClientHeartbeatWorker(this));
	}

	public void sendLoginMsg() {
		try {
			//在没有领导者的情况下，重复登录过于频繁，所以睡眠1s
			Thread.sleep(1000 * 1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		ServerAddress serverAddress = AddrManager.getInstance().nextAddr();
		Channel channel = ConnManager.getInstance().connect(serverAddress);
		if (channel == null) {
			LOG.error("连接服务器失败:{}", serverAddress.toString());
			sendLoginMsg();
		} else {
			LOG.info("连接服务器成功:{}", serverAddress.toString());
			LoginMsg loginMsg = getLoginMsg();
			if (!ConnManager.getInstance().sendMsg(serverAddress, loginMsg)) {
				LOG.error("发送登录消息失败,重新登录");
				sendLoginMsg();
			}
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

	public boolean isLogin() {
		return loginStatus == EnumLoginStatus.OK;
	}

	public synchronized void updateSessionId(long newSession) {
		if (newSession != sessionId) {
			sessionId = newSession;
		}
	}

	public ClientMsg getLastReceiveMsg() {
		synchronized (ClientMsg.class) {
			return lastReceiveMsg;
		}
	}

	public void updateLastReceiveMsg(ClientMsg lastReceiveMsg) {
		synchronized (ClientMsg.class) {
			this.lastReceiveMsg = lastReceiveMsg;
		}
	}

	public void checkClientHeartbeat(int checkRange) {
		try {
			long minSessionTime = DateHelper.addMillSecond(new Date(), -checkRange);
			ClientMsg lastReceiveMsg = getLastReceiveMsg();
			if (lastReceiveMsg == null) {
				LOG.error("心跳超时,重新登录");
				updateLoginStatus(EnumLoginStatus.FALSE);
			} else {
				long lastReceiveTime = lastReceiveMsg.getReceviceTime();
				if (lastReceiveTime < minSessionTime) {
					LOG.error("心跳超时,重新登录");
					updateLoginStatus(EnumLoginStatus.FALSE);
				}
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	public void sendClientHeartbeat() {
		ClientHeartbeatMsg msg = getClientHeartbeatMsg();
		ConnManager.getInstance().sendMsg(AddrManager.getInstance().getLeaderConn(), msg);
	}

	public ClientHeartbeatMsg getClientHeartbeatMsg() {
		ClientHeartbeatMsg msg = new ClientHeartbeatMsg();
		msg.setMsgId(IdGenerateHelper.getMsgId());
		msg.setMsgType(Msg.TYPE_CLIENT_HEARTBEAT);
		msg.setSendTime(DateHelper.formatDate2Long(new Date(), DateHelper.YYYYMMDDHHMMSSsss));
		msg.setSessionId(sessionId);
		return msg;
	}

}
