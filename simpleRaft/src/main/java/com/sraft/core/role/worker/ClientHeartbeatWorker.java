package com.sraft.core.role.worker;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sraft.client.SimpleRaftClient;
import com.sraft.common.DateHelper;
import com.sraft.common.IdGenerateHelper;
import com.sraft.core.message.ClientHeartbeatMsg;
import com.sraft.core.message.Msg;
import com.sraft.core.message.ReplyClientHeartbeatMsg;
import com.sraft.core.role.Candidate;
import com.sraft.core.role.Follower;
import com.sraft.core.role.Leader;
import com.sraft.enums.EnumLoginStatus;

import io.netty.channel.ChannelHandlerContext;

public class ClientHeartbeatWorker extends Workder {
	private static Logger LOG = LoggerFactory.getLogger(ClientHeartbeatWorker.class);

	private SimpleRaftClient client;

	public ClientHeartbeatWorker() {

	}

	public ClientHeartbeatWorker(SimpleRaftClient client) {
		this.client = client;
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
		ReplyClientHeartbeatMsg replyClientHeartbeatMsg = new ReplyClientHeartbeatMsg();
		replyClientHeartbeatMsg.setMsgId(IdGenerateHelper.getMsgId());
		replyClientHeartbeatMsg.setMsgType(Msg.TYPE_REPLY_CLIENT_HEARTBEAT);
		replyClientHeartbeatMsg.setSendTime(DateHelper.formatDate2Long(new Date(), DateHelper.YYYYMMDDHHMMSSsss));
		replyClientHeartbeatMsg.setSessionId(clientHeartbeatMsg.getSessionId());
		if (role instanceof Follower) {
			replyClientHeartbeatMsg.setResult(Msg.RETURN_STATUS_FALSE);
			replyClientHeartbeatMsg.setErrCode(Msg.ERR_CODE_LOGIN_FOLLOWER);
		} else if (role instanceof Candidate) {
			replyClientHeartbeatMsg.setResult(Msg.RETURN_STATUS_FALSE);
			replyClientHeartbeatMsg.setErrCode(Msg.ERR_CODE_LOGIN_CANDIDATE);
		} else {
			Leader leader = (Leader) role;
			boolean isUpdate = leader.updateSession(clientHeartbeatMsg.getSessionId(),
					clientHeartbeatMsg.getReceviceTime(), -1);
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
			client.updateLastReceiveMsg(replyClientHeartbeatMsg);
		} else {
			client.updateLoginStatus(EnumLoginStatus.FALSE);
			int errCode = replyClientHeartbeatMsg.getErrCode();
			switch (errCode) {
			case Msg.ERR_CODE_LOGIN_FOLLOWER:
				LOG.error("连接到跟随者,需要重新登录");
				break;
			case Msg.ERR_CODE_LOGIN_CANDIDATE:
				LOG.error("连接到候选者,需要重新登录");
				break;
			case Msg.ERR_CODE_LOGIN_LEADER_NO_MAJOR:
				LOG.error("由于没有过半存活机器，领导者暂停服务,需要重新登录");
				break;
			case Msg.ERR_CODE_SESSION_TIMEOUT:
				LOG.error("会话超时,需要重新登录");
				break;
			default:
				break;
			}
		}
	}
}
