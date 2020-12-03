package com.sraft.core.role.worker;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sraft.client.ClientConnManager;
import com.sraft.common.DateHelper;
import com.sraft.common.IdGenerateHelper;
import com.sraft.core.message.ClientActionMsg;
import com.sraft.core.message.Msg;
import com.sraft.core.message.ReplyClientActionMsg;
import com.sraft.core.role.Candidate;
import com.sraft.core.role.Follower;
import com.sraft.core.role.Leader;
import com.sraft.core.role.RoleController;

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
		replyClientActionMsg.setActionType(clientActionMsg.getActionType());
		replyClientActionMsg.setMsgId(IdGenerateHelper.getMsgId());
		replyClientActionMsg.setMsgType(Msg.TYPE_REPLY_CLIENT_ACTION);
		replyClientActionMsg.setSessionId(sessionId);
		replyClientActionMsg.setSendTime(DateHelper.formatDate2Long(new Date(), DateHelper.YYYYMMDDHHMMSSsss));
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
			Leader leader = (Leader) role;
			
		}
	}

	public void dealReplyClientActionMsg(ReplyClientActionMsg replyClientActionMsg) {

	}
}
