package com.sraft.client;

import java.text.ParseException;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sraft.common.DateHelper;
import com.sraft.common.IdGenerateHelper;
import com.sraft.common.flow.FlowHeader;
import com.sraft.common.flow.NoFlowLineException;
import com.sraft.core.message.ClientActionMsg;
import com.sraft.core.message.ClientHeartbeatMsg;
import com.sraft.core.message.ClientMsg;
import com.sraft.core.message.LoginMsg;
import com.sraft.core.message.Msg;
import com.sraft.core.message.Packet;
import com.sraft.core.message.ReplyClientActionMsg;
import com.sraft.core.net.ConnManager;
import com.sraft.core.net.ServerAddress;
import com.sraft.core.role.RoleController;
import com.sraft.core.role.worker.ClientActionWorker;
import com.sraft.core.role.worker.ClientHeartbeatWorker;
import com.sraft.core.role.worker.LoginWorker;
import com.sraft.core.schedule.ScheduleClientHeartbeat;
import com.sraft.enums.EnumLoginStatus;
import com.sraft.enums.EnumServiceStatus;

import io.netty.channel.Channel;

public class ClientConnManager {
	private static Logger LOG = LoggerFactory.getLogger(ClientConnManager.class);
	private volatile long sessionId = -1;

	private String LOGIN_SUCCESS = new String("LOGIN_SUCCESS");
	private volatile EnumLoginStatus loginStatus = EnumLoginStatus.FALSE;
	/**
	 * 服务是否可用；登录成功并且过半节点存活，才是可用；否则都是不可用
	 */
	private volatile EnumServiceStatus serviceStatus = EnumServiceStatus.UN_USEFULL;
	private final int CLIENT_HEARTBEAT_INTERVAL = 300;
	private volatile ClientMsg lastReceiveMsg = null;

	/**
	 * 发送却还没收到回复的队列，只有事务操作才会进队列
	 */
	private BlockingQueue<Packet> pendingQueue = new LinkedBlockingQueue<Packet>();

	/**
	 * 接收到服务端回复后，如果该请求是异步回调方式，则先将packet放进队列中
	 */
	private BlockingQueue<Packet> callBackQueue = new LinkedBlockingQueue<Packet>();

	public static final String CLIENT_ACTION_SENDER_WORKER = new String("CLIENT_ACTION_SENDER_WORKER");

	public ClientConnManager(String address) throws Exception {
		IdGenerateHelper.initializeNextSession(new Random().nextInt(100) + 1);
		LOG.info("解析地址");
		AddrManager.getInstance().explainAddr(address);
		LOG.info("添加消息处理通道");
		addWorker();
		LOG.info("添加发送事务动作通道");
		addSenderWorker();
		LOG.info("发送登录消息");
		sendLoginMsg();
		LOG.info("设置客户端心跳超时");
		startClientHeartbeat();
		// 监控回调队列
		monitorCallBack();
	}

	public void addSenderWorker() {
		FlowHeader.employ(CLIENT_ACTION_SENDER_WORKER, new ClientSenderWorker(this));
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
		FlowHeader.employ(RoleController.CLIENT_ACTION_WORKDER, new ClientActionWorker(this));
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
			if (!ConnManager.getInstance().sendMsg(serverAddress, loginMsg)) {
				LOG.error("发送登录消息失败,重新登录");
				sendLoginMsg();
			}
		}
	}

	/**
	 * 重连
	 */
	public void reConnected() {
		//清空缓存队列
		synchronized (pendingQueue) {
			while (!pendingQueue.isEmpty()) {
				try {
					Packet packet = pendingQueue.take();
					fillReplyAndNotice(packet);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		sendLoginMsg();
	}

	public void fillReplyAndNotice(Packet packet) {
		synchronized (packet) {
			if (!packet.isFinish()) {
				ClientActionMsg clientActionMsg = (ClientActionMsg) packet.getSendMsg();
				ReplyClientActionMsg reply = new ReplyClientActionMsg();
				reply.setActionType(clientActionMsg.getActionType());
				reply.setErrCode(Msg.ERR_CODE_LOG_APPEND_FALSE);
				reply.setMsgId(clientActionMsg.getMsgId());
				reply.setMsgType(Msg.TYPE_REPLY_CLIENT_ACTION);
				reply.setReceviceTime(-1);
				reply.setResult(Msg.RETURN_STATUS_FALSE);
				reply.setSessionId(clientActionMsg.getSessionId());
				reply.setSendTime(-1);
				packet.setReplyMsg(reply);
				packet.notify();
			}
		}
	}

	public void fillReply(Packet packet) {
		ClientActionMsg clientActionMsg = (ClientActionMsg) packet.getSendMsg();
		ReplyClientActionMsg reply = new ReplyClientActionMsg();
		reply.setActionType(clientActionMsg.getActionType());
		reply.setErrCode(Msg.ERR_CODE_LOG_APPEND_FALSE);
		reply.setMsgId(clientActionMsg.getMsgId());
		reply.setMsgType(Msg.TYPE_REPLY_CLIENT_ACTION);
		reply.setReceviceTime(-1);
		reply.setResult(Msg.RETURN_STATUS_FALSE);
		reply.setSessionId(clientActionMsg.getSessionId());
		reply.setSendTime(-1);
		packet.setReplyMsg(reply);
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
			if (loginStatus != newStatus) {
				loginStatus = newStatus;
			}
			if (isLogin()) {
				synchronized (LOGIN_SUCCESS) {
					LOGIN_SUCCESS.notify();
				}
			}
		}
	}

	public void updateServiceStatus(EnumServiceStatus newStatus) {
		synchronized (EnumServiceStatus.class) {
			if (serviceStatus != newStatus) {
				serviceStatus = newStatus;
				LOG.info("服务状态变更:{}", serviceStatus);
			}
			if (isService()) {
				synchronized (LOGIN_SUCCESS) {
					LOGIN_SUCCESS.notifyAll();
				}
			}
		}
	}

	public boolean isService() {
		return serviceStatus == EnumServiceStatus.USEFULL;
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

	public boolean isUseFull() {
		if (isLogin() && isService()) {
			return true;
		} else {
			return false;
		}
	}

	public boolean isUseFullSyn() {
		while (true) {
			synchronized (LOGIN_SUCCESS) {
				if (isLogin() && isService()) {
					return true;
				} else {
					try {
						LOGIN_SUCCESS.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	public long getSessionId() {
		return sessionId;
	}

	public void sendActionMsg(Packet packet) {
		try {
			FlowHeader.putProducts(CLIENT_ACTION_SENDER_WORKER, packet);
		} catch (NoFlowLineException e) {
			e.printStackTrace();
			LOG.error(e.getMessage(), e);
		}
	}

	public BlockingQueue<Packet> getPendingQueue() {
		return pendingQueue;
	}

	public void setPendingQueue(BlockingQueue<Packet> pendingQueue) {
		this.pendingQueue = pendingQueue;
	}

	public BlockingQueue<Packet> getCallBackQueue() {
		return callBackQueue;
	}

	public void setCallBackQueue(BlockingQueue<Packet> callBackQueue) {
		this.callBackQueue = callBackQueue;
	}

	public void monitorCallBack() {
		new Thread(new DealCallBack()).start();
	}

	class DealCallBack implements Runnable {

		@Override
		public void run() {
			while (true) {
				synchronized (callBackQueue) {
					if (callBackQueue.isEmpty()) {
						try {
							callBackQueue.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					} else {
						try {
							Packet packet = callBackQueue.take();
							packet.getCall().call((ClientActionMsg) packet.getSendMsg(),
									(ReplyClientActionMsg) packet.getReplyMsg());
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}

		}

	}

	//	public void close() {
	//		LOG.info("停止心跳发送");
	//		stopClientHeartbeat();
	//		LOG.info("移除消息通道");
	//		FlowHeader.unEmploy(CLIENT_ACTION_SENDER_WORKER);
	//		FlowHeader.unEmploy(RoleController.LOGIN_WORKER);
	//		FlowHeader.unEmploy(RoleController.CLIENT_HEARTBEAT_WORKER);
	//		FlowHeader.unEmploy(RoleController.CLIENT_ACTION_WORKDER);
	//		LOG.info("关闭网络");
	//		ConnManager.getInstance().closeAll();
	//	}
}
