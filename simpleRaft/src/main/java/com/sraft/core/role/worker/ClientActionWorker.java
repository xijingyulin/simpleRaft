package com.sraft.core.role.worker;

import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sraft.client.ClientConnManager;
import com.sraft.common.DateHelper;
import com.sraft.common.IdGenerateHelper;
import com.sraft.core.log.LogData;
import com.sraft.core.message.BaseLog;
import com.sraft.core.message.ClientActionMsg;
import com.sraft.core.message.Msg;
import com.sraft.core.message.Packet;
import com.sraft.core.message.ReplyClientActionMsg;
import com.sraft.core.role.AppendTask;
import com.sraft.core.role.Candidate;
import com.sraft.core.role.Follower;
import com.sraft.core.role.Leader;
import com.sraft.core.role.RoleController;
import com.sraft.enums.EnumLoginStatus;
import com.sraft.enums.EnumServiceStatus;

import io.netty.channel.ChannelHandlerContext;

public class ClientActionWorker extends Workder {
	private static Logger LOG = LoggerFactory.getLogger(ClientActionWorker.class);

	private ClientConnManager clientConnManager;
	private RoleController roleController;

	public ClientActionWorker(RoleController roleController) {
		this.roleController = roleController;
		setEnable(true);
	}

	public ClientActionWorker(ClientConnManager clientConnManager) {
		this.clientConnManager = clientConnManager;
		setEnable(true);
	}

	@Override
	public void doWork(Object object) {
		List<Object> params = (List<Object>) object;
		ChannelHandlerContext ctx = (ChannelHandlerContext) params.get(0);
		Object msg = params.get(1);
		if (msg instanceof ClientActionMsg) {
			ClientActionMsg clientActionMsg = (ClientActionMsg) params.get(1);
			dealClientActionMsg(ctx, clientActionMsg);
		} else if (msg instanceof ReplyClientActionMsg) {
			ReplyClientActionMsg replyClientActionMsg = (ReplyClientActionMsg) params.get(1);
			dealReplyClientActionMsg(replyClientActionMsg);
		}
	}

	public void dealClientActionMsg(ChannelHandlerContext ctx, ClientActionMsg clientActionMsg) {
		long sessionId = clientActionMsg.getSessionId();
		long receviceTime = clientActionMsg.getReceviceTime();
		long clientTransactionId = clientActionMsg.getTransactionId();
		boolean isUpdate = roleController.updateSession(sessionId, receviceTime, clientTransactionId);
		ReplyClientActionMsg replyClientActionMsg = new ReplyClientActionMsg();
		if (role == null) {
			replyClientActionMsg.setResult(Msg.RETURN_STATUS_FALSE);
			replyClientActionMsg.setErrCode(Msg.ERR_CODE_LOGIN_FOLLOWER);
		} else if (role.isChangedRole()) {
			replyClientActionMsg.setResult(Msg.RETURN_STATUS_FALSE);
			replyClientActionMsg.setErrCode(Msg.ERR_CODE_ROLE_CHANGED);
		} else if (role instanceof Follower) {
			replyClientActionMsg.setResult(Msg.RETURN_STATUS_FALSE);
			replyClientActionMsg.setErrCode(Msg.ERR_CODE_LOGIN_FOLLOWER);
		} else if (role instanceof Candidate) {
			replyClientActionMsg.setResult(Msg.RETURN_STATUS_FALSE);
			replyClientActionMsg.setErrCode(Msg.ERR_CODE_LOGIN_CANDIDATE);
		} else if (role instanceof Leader) {
			if (!isUpdate) {
				replyClientActionMsg.setResult(Msg.RETURN_STATUS_FALSE);
				replyClientActionMsg.setErrCode(Msg.ERR_CODE_SESSION_TIMEOUT);
			} else {
				Leader leader = (Leader) role;
				if (!leader.isAliveOverHalf()) {
					replyClientActionMsg.setResult(Msg.RETURN_STATUS_FALSE);
					replyClientActionMsg.setErrCode(Msg.ERR_CODE_LOGIN_LEADER_NO_MAJOR);
				} else {
					// 注意读操作，不需累计索引
					BaseLog baseLog = leader.getBaseLog(clientActionMsg);
					AppendTask appendTask = new AppendTask(baseLog);
					synchronized (appendTask) {
						leader.submitAppendTask(appendTask);
						try {
							appendTask.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					if (appendTask.isOverHalfSuccess()) {
						// 提交状态机
						leader.getRoleController().commit();
						if (baseLog.getLogType() == LogData.LOG_GET) {
							replyClientActionMsg.setValue(leader.getRoleController().getValue(baseLog.getKey()));
						}
						replyClientActionMsg.setResult(Msg.RETURN_STATUS_OK);
					} else {
						replyClientActionMsg.setResult(Msg.RETURN_STATUS_FALSE);
						replyClientActionMsg.setErrCode(Msg.ERR_CODE_LOG_APPEND_FALSE);
					}
				}
			}
		}
		replyClientActionMsg.setActionType(clientActionMsg.getActionType());
		replyClientActionMsg.setMsgId(IdGenerateHelper.getMsgId());
		replyClientActionMsg.setMsgType(Msg.TYPE_REPLY_CLIENT_ACTION);
		replyClientActionMsg.setSessionId(sessionId);
		replyClientActionMsg.setSendTime(DateHelper.formatDate2Long(new Date(), DateHelper.YYYYMMDDHHMMSSsss));
		ctx.writeAndFlush(replyClientActionMsg);
	}

	public void dealReplyClientActionMsg(ReplyClientActionMsg replyClientActionMsg) {
		int result = replyClientActionMsg.getResult();
		if (result == Msg.RETURN_STATUS_OK) {
			clientConnManager.updateServiceStatus(EnumServiceStatus.USEFULL);
			clientConnManager.updateLastReceiveMsg(replyClientActionMsg);

			BlockingQueue<Packet> pendingQueue = clientConnManager.getPendingQueue();
			synchronized (pendingQueue) {
				if (!pendingQueue.isEmpty()) {
					Packet packet = pendingQueue.remove();
					synchronized (packet) {
						packet.setReplyMsg(replyClientActionMsg);
						packet.notify();
					}
				}
			}

		} else {
			int errCode = replyClientActionMsg.getErrCode();
			switch (errCode) {
			case Msg.ERR_CODE_LOGIN_FOLLOWER:
				clientConnManager.updateServiceStatus(EnumServiceStatus.UN_USEFULL);
				clientConnManager.updateLoginStatus(EnumLoginStatus.FALSE);
				LOG.error("连接到跟随者,需要重新登录");
				break;
			case Msg.ERR_CODE_LOGIN_CANDIDATE:
				clientConnManager.updateServiceStatus(EnumServiceStatus.UN_USEFULL);
				clientConnManager.updateLoginStatus(EnumLoginStatus.FALSE);
				LOG.error("连接到候选者,需要重新登录");
				break;
			case Msg.ERR_CODE_LOGIN_LEADER_NO_MAJOR:
				clientConnManager.updateServiceStatus(EnumServiceStatus.UN_USEFULL);
				clientConnManager.updateLastReceiveMsg(replyClientActionMsg);
				LOG.error("由于没有过半存活机器，领导者暂停服务");
				break;
			case Msg.ERR_CODE_SESSION_TIMEOUT:
				clientConnManager.updateServiceStatus(EnumServiceStatus.UN_USEFULL);
				clientConnManager.updateLoginStatus(EnumLoginStatus.FALSE);
				LOG.error("会话超时,需要重新登录");
				break;
			case Msg.ERR_CODE_ROLE_CHANGED:
				clientConnManager.updateServiceStatus(EnumServiceStatus.UN_USEFULL);
				clientConnManager.updateLoginStatus(EnumLoginStatus.FALSE);
				LOG.error("角色已改变,需要重新登录");
				break;
			case Msg.ERR_CODE_LOG_APPEND_FALSE:
				clientConnManager.updateLoginStatus(EnumLoginStatus.FALSE);
				LOG.error("追加失败");
				break;
			default:
				clientConnManager.updateServiceStatus(EnumServiceStatus.UN_USEFULL);
				clientConnManager.updateLoginStatus(EnumLoginStatus.FALSE);
				LOG.error("其它原因,需要重新登录");
				break;
			}
		}
	}
}
