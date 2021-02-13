package com.sraft.core.role.worker;

import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sraft.common.DateHelper;
import com.sraft.common.IdGenerateHelper;
import com.sraft.common.flow.IFlowWorker;
import com.sraft.core.message.AppendLogEntryMsg;
import com.sraft.core.message.AppendSnapshotMsg;
import com.sraft.core.message.ClientActionMsg;
import com.sraft.core.message.ClientHeartbeatMsg;
import com.sraft.core.message.HeartbeatMsg;
import com.sraft.core.message.LoginMsg;
import com.sraft.core.message.Msg;
import com.sraft.core.message.ReplyAppendLogEntryMsg;
import com.sraft.core.message.ReplyAppendSnapshotMsg;
import com.sraft.core.message.ReplyClientActionMsg;
import com.sraft.core.message.ReplyClientHeartbeatMsg;
import com.sraft.core.message.ReplyHeartbeatMsg;
import com.sraft.core.message.ReplyLoginMsg;
import com.sraft.core.message.ReplyRequestVoteMsg;
import com.sraft.core.message.RequestVoteMsg;
import com.sraft.core.role.AbstractRoles;
import com.sraft.core.role.RoleController;

import io.netty.channel.ChannelHandlerContext;

public abstract class Workder implements IFlowWorker {
	private static Logger LOG = LoggerFactory.getLogger(Workder.class);

	//protected BlockingQueue<Object> msgQueue = new LinkedBlockingQueue<Object>();

	/**
	 * 因为任期不一致，选举超时等消息，需要转换角色，避免在停止角色后继续处理消息，暂时将消息放到队列中
	 */
	private volatile boolean isChangeRole = true;

	protected volatile AbstractRoles role = null;

	protected RoleController roleController;

	protected AtomicInteger MSG_NOT_DEAL = new AtomicInteger(0);

	public Workder(RoleController roleController) {
		this.roleController = roleController;
	}

	@Override
	public void deliver(Object object) {
		if (isChangeRole()) {
			dealChangeRoleMsg(object);
		} else {
			MSG_NOT_DEAL.incrementAndGet();
			doWork(object);
		}
	}

	public void dealChangeRoleMsg(Object object) {
		List<Object> params = (List<Object>) object;
		ChannelHandlerContext ctx = (ChannelHandlerContext) params.get(0);
		Object msg = params.get(1);
		if (msg instanceof HeartbeatMsg) {
			ReplyHeartbeatMsg reply = new ReplyHeartbeatMsg();
			reply.setMsgId(IdGenerateHelper.getMsgId());
			reply.setMsgType(Msg.TYPE_REPLY_HEARTBEAT);
			reply.setNodeId(role.getSelfId());
			reply.setSendTime(DateHelper.formatDate2Long(new Date(), DateHelper.YYYYMMDDHHMMSSsss));
			reply.setTerm(role.getCurrentTerm());
			reply.setErrCode(Msg.ERR_CODE_ROLE_CHANGED);
			reply.setResult(Msg.RETURN_STATUS_FALSE);
			ctx.writeAndFlush(reply);
		} else if (msg instanceof ReplyHeartbeatMsg) {
			LOG.info("角色已转换,丢弃心跳回复消息:{}", msg);
		} else if (msg instanceof RequestVoteMsg) {
			ReplyRequestVoteMsg reply = new ReplyRequestVoteMsg();
			reply.setMsgId(IdGenerateHelper.getMsgId());
			reply.setMsgType(Msg.TYPE_REPLY_REQUEST_VOTE);
			reply.setNodeId(role.getSelfId());
			reply.setTerm(role.getCurrentTerm());
			reply.setSendTime(DateHelper.formatDate2Long(new Date(), DateHelper.YYYYMMDDHHMMSSsss));
			reply.setErrCode(Msg.ERR_CODE_ROLE_CHANGED);
			reply.setResult(Msg.RETURN_STATUS_FALSE);
			ctx.writeAndFlush(reply);
		} else if (msg instanceof ReplyRequestVoteMsg) {
			LOG.info("角色已转换,丢弃投票回复消息:{}", msg);
		} else if (msg instanceof AppendLogEntryMsg) {
			AppendLogEntryMsg appendLogEntryMsg = (AppendLogEntryMsg) params.get(1);
			ReplyAppendLogEntryMsg reply = new ReplyAppendLogEntryMsg();
			reply.setMsgId(IdGenerateHelper.getMsgId());
			reply.setMsgType(Msg.TYPE_REPLY_APPEND_LOG);
			reply.setNodeId(role.getSelfId());
			reply.setTerm(role.getCurrentTerm());
			reply.setSendTime(DateHelper.formatDate2Long(new Date(), DateHelper.YYYYMMDDHHMMSSsss));
			reply.setAppendType(appendLogEntryMsg.getAppendType());
			reply.setTransactionId(appendLogEntryMsg.getTransactionId());
			reply.setErrCode(Msg.ERR_CODE_ROLE_CHANGED);
			reply.setResult(Msg.RETURN_STATUS_FALSE);
			ctx.writeAndFlush(reply);
		} else if (msg instanceof ReplyAppendLogEntryMsg) {
			LOG.info("角色已转换,丢弃追加日志回复消息:{}", msg);
		} else if (msg instanceof AppendSnapshotMsg) {
			AppendSnapshotMsg appendSnapshotMsg = (AppendSnapshotMsg) params.get(1);
			ReplyAppendSnapshotMsg reply = new ReplyAppendSnapshotMsg();
			reply.setMsgId(IdGenerateHelper.getMsgId());
			reply.setMsgType(Msg.TYPE_REPLY_APPEND_LOG);
			reply.setNodeId(role.getSelfId());
			reply.setTerm(role.getCurrentTerm());
			reply.setTransactionId(appendSnapshotMsg.getTransactionId());
			reply.setSendTime(DateHelper.formatDate2Long(new Date(), DateHelper.YYYYMMDDHHMMSSsss));
			reply.setErrCode(Msg.ERR_CODE_ROLE_CHANGED);
			reply.setResult(Msg.RETURN_STATUS_FALSE);
			ctx.writeAndFlush(reply);
		} else if (msg instanceof ReplyAppendSnapshotMsg) {
			LOG.info("角色已转换,丢弃追加快照回复消息:{}", msg);
		} else if (msg instanceof ClientActionMsg) {
			ClientActionMsg clientActionMsg = (ClientActionMsg) params.get(1);

			long sessionId = clientActionMsg.getSessionId();
			long receviceTime = clientActionMsg.getReceviceTime();
			roleController.updateSession(sessionId, receviceTime);
			ReplyClientActionMsg reply = new ReplyClientActionMsg();
			reply.setActionType(clientActionMsg.getActionType());
			reply.setMsgId(IdGenerateHelper.getMsgId());
			reply.setMsgType(Msg.TYPE_REPLY_CLIENT_ACTION);
			reply.setSessionId(sessionId);
			reply.setSendTime(DateHelper.formatDate2Long(new Date(), DateHelper.YYYYMMDDHHMMSSsss));
			reply.setResult(Msg.RETURN_STATUS_FALSE);
			reply.setErrCode(Msg.ERR_CODE_ROLE_CHANGED);
			ctx.writeAndFlush(reply);
		} else if (msg instanceof LoginMsg) {
			LoginMsg loginMsg = (LoginMsg) params.get(1);
			long sessionId = loginMsg.getSessionId();
			long receviceTime = loginMsg.getReceviceTime();
			ReplyLoginMsg reply = new ReplyLoginMsg();
			reply.setMsgType(Msg.TYPE_REPLY_CLIENT_LOGIN);
			reply.setMsgId(IdGenerateHelper.getMsgId());
			reply.setSendTime(DateHelper.formatDate2Long(new Date(), DateHelper.YYYYMMDDHHMMSSsss));
			reply.setSessionId(sessionId);
			roleController.updateSession(sessionId, receviceTime);
			reply.setResult(Msg.RETURN_STATUS_FALSE);
			reply.setErrCode(Msg.ERR_CODE_ROLE_CHANGED);
			ctx.writeAndFlush(reply);
		} else if (msg instanceof ClientHeartbeatMsg) {
			ClientHeartbeatMsg clientHeartbeatMsg = (ClientHeartbeatMsg) params.get(1);
			long sessionId = clientHeartbeatMsg.getSessionId();
			long receviceTime = clientHeartbeatMsg.getReceviceTime();
			ReplyClientHeartbeatMsg reply = new ReplyClientHeartbeatMsg();
			reply.setMsgId(IdGenerateHelper.getMsgId());
			reply.setMsgType(Msg.TYPE_REPLY_CLIENT_HEARTBEAT);
			reply.setSendTime(DateHelper.formatDate2Long(new Date(), DateHelper.YYYYMMDDHHMMSSsss));
			reply.setSessionId(sessionId);
			roleController.updateSession(sessionId, receviceTime);
			reply.setResult(Msg.RETURN_STATUS_FALSE);
			reply.setErrCode(Msg.ERR_CODE_ROLE_CHANGED);
			ctx.writeAndFlush(reply);
		}
	}

	public abstract void doWork(Object object);

	public AbstractRoles getRole() {
		return role;
	}

	public void setRole(AbstractRoles role) {
		this.role = role;
	}

	/**
	 * 比较任期
	 * 
	 * @param fromTerm
	 * @return
	 */
	protected boolean checkTerm(long fromTerm) {
		if (fromTerm >= role.getCurrentTerm()) {
			return true;
		} else {
			return false;
		}
	}

	public AtomicInteger getMSG_NOT_DEAL() {
		return MSG_NOT_DEAL;
	}

	public void setMSG_NOT_DEAL(AtomicInteger mSG_NOT_DEAL) {
		MSG_NOT_DEAL = mSG_NOT_DEAL;
	}

	public boolean isChangeRole() {
		return isChangeRole;
	}

	public void setChangeRole(boolean isChangeRole) {
		synchronized (Workder.class) {
			this.isChangeRole = isChangeRole;
		}
	}
}
