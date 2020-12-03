package com.sraft.core.role.worker;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sraft.client.ClientConnManager;
import com.sraft.common.DateHelper;
import com.sraft.common.IdGenerateHelper;
import com.sraft.core.message.ClientHeartbeatMsg;
import com.sraft.core.message.Msg;
import com.sraft.core.message.ReplyClientHeartbeatMsg;
import com.sraft.core.role.Candidate;
import com.sraft.core.role.Follower;
import com.sraft.core.role.Leader;
import com.sraft.core.role.RoleController;
import com.sraft.enums.EnumLoginStatus;
import com.sraft.enums.EnumServiceStatus;

import io.netty.channel.ChannelHandlerContext;

public class ClientHeartbeatWorker extends Workder {
	private static Logger LOG = LoggerFactory.getLogger(ClientHeartbeatWorker.class);

	private ClientConnManager clientConnManager;
	private RoleController roleController;

	public ClientHeartbeatWorker() {

	}

	public ClientHeartbeatWorker(ClientConnManager clientConnManager) {
		this.clientConnManager = clientConnManager;
		setEnable(true);
	}

	public ClientHeartbeatWorker(RoleController roleController) {
		this.roleController = roleController;
		setEnable(true);
	}

	@Override
	public void doWork(Object object) {
		List<Object> params = (List<Object>) object;
		ChannelHandlerContext ctx = (ChannelHandlerContext) params.get(0);
		Object msg = params.get(1);
		if (msg instanceof ClientHeartbeatMsg) {
			ClientHeartbeatMsg clientHeartbeatMsg = (ClientHeartbeatMsg) params.get(1);
			dealClientHeartbeatMsg(ctx, clientHeartbeatMsg);
		} else if (msg instanceof ReplyClientHeartbeatMsg) {
			ReplyClientHeartbeatMsg replyClientHeartbeatMsg = (ReplyClientHeartbeatMsg) params.get(1);
			dealReplyClientHeartbeatMsg(replyClientHeartbeatMsg);
		}
	}

	public void dealClientHeartbeatMsg(ChannelHandlerContext ctx, ClientHeartbeatMsg clientHeartbeatMsg) {
		long sessionId = clientHeartbeatMsg.getSessionId();
		long receviceTime = clientHeartbeatMsg.getReceviceTime();
		ReplyClientHeartbeatMsg replyClientHeartbeatMsg = new ReplyClientHeartbeatMsg();
		replyClientHeartbeatMsg.setMsgId(IdGenerateHelper.getMsgId());
		replyClientHeartbeatMsg.setMsgType(Msg.TYPE_REPLY_CLIENT_HEARTBEAT);
		replyClientHeartbeatMsg.setSendTime(DateHelper.formatDate2Long(new Date(), DateHelper.YYYYMMDDHHMMSSsss));
		replyClientHeartbeatMsg.setSessionId(sessionId);
		boolean isUpdate = roleController.updateSession(sessionId, receviceTime, -1);
		if (role == null) {
			replyClientHeartbeatMsg.setResult(Msg.RETURN_STATUS_FALSE);
			replyClientHeartbeatMsg.setErrCode(Msg.ERR_CODE_LOGIN_FOLLOWER);
		} else if (role.isChangedRole()) {
			replyClientHeartbeatMsg.setResult(Msg.RETURN_STATUS_FALSE);
			replyClientHeartbeatMsg.setErrCode(Msg.ERR_CODE_ROLE_CHANGED);
		} else if (role instanceof Follower) {
			replyClientHeartbeatMsg.setResult(Msg.RETURN_STATUS_FALSE);
			replyClientHeartbeatMsg.setErrCode(Msg.ERR_CODE_LOGIN_FOLLOWER);
		} else if (role instanceof Candidate) {
			replyClientHeartbeatMsg.setResult(Msg.RETURN_STATUS_FALSE);
			replyClientHeartbeatMsg.setErrCode(Msg.ERR_CODE_LOGIN_CANDIDATE);
		} else if (role instanceof Leader) {
			Leader leader = (Leader) role;
			if (isUpdate) {
				if (leader.isAliveOverHalf()) {
					replyClientHeartbeatMsg.setResult(Msg.RETURN_STATUS_OK);
				} else {
					replyClientHeartbeatMsg.setResult(Msg.RETURN_STATUS_FALSE);
					replyClientHeartbeatMsg.setErrCode(Msg.ERR_CODE_LOGIN_LEADER_NO_MAJOR);
				}
			} else {
				replyClientHeartbeatMsg.setResult(Msg.RETURN_STATUS_FALSE);
				replyClientHeartbeatMsg.setErrCode(Msg.ERR_CODE_SESSION_TIMEOUT);
			}
		}
		ctx.writeAndFlush(replyClientHeartbeatMsg);
	}

	public void dealReplyClientHeartbeatMsg(ReplyClientHeartbeatMsg replyClientHeartbeatMsg) {
		int result = replyClientHeartbeatMsg.getResult();
		if (result == Msg.RETURN_STATUS_OK) {
			clientConnManager.updateServiceStatus(EnumServiceStatus.USEFULL);
			clientConnManager.updateLastReceiveMsg(replyClientHeartbeatMsg);
		} else {
			int errCode = replyClientHeartbeatMsg.getErrCode();
			switch (errCode) {
			case Msg.ERR_CODE_LOGIN_FOLLOWER:
				clientConnManager.updateLoginStatus(EnumLoginStatus.FALSE);
				LOG.error("连接到跟随者,需要重新登录");
				break;
			case Msg.ERR_CODE_LOGIN_CANDIDATE:
				clientConnManager.updateLoginStatus(EnumLoginStatus.FALSE);
				LOG.error("连接到候选者,需要重新登录");
				break;
			case Msg.ERR_CODE_LOGIN_LEADER_NO_MAJOR:
				clientConnManager.updateLastReceiveMsg(replyClientHeartbeatMsg);
				clientConnManager.updateServiceStatus(EnumServiceStatus.UN_USEFULL);
				LOG.error("由于没有过半存活机器，领导者暂停服务");
				break;
			case Msg.ERR_CODE_SESSION_TIMEOUT:
				clientConnManager.updateLoginStatus(EnumLoginStatus.FALSE);
				LOG.error("会话超时,需要重新登录");
				break;
			case Msg.ERR_CODE_ROLE_CHANGED:
				clientConnManager.updateLoginStatus(EnumLoginStatus.FALSE);
				LOG.error("角色已改变,需要重新登录");
				break;
			default:
				clientConnManager.updateLoginStatus(EnumLoginStatus.FALSE);
				LOG.error("其它原因,需要重新登录");
				break;
			}
		}
	}
}
