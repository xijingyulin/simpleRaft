package com.sraft.core.role.worker;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sraft.client.AddrManager;
import com.sraft.client.SimpleRaftClient;
import com.sraft.common.DateHelper;
import com.sraft.common.IdGenerateHelper;
import com.sraft.common.StringHelper;
import com.sraft.core.message.LoginMsg;
import com.sraft.core.message.Msg;
import com.sraft.core.message.ReplyLoginMsg;
import com.sraft.core.net.ServerAddress;
import com.sraft.core.role.Candidate;
import com.sraft.core.role.Follower;
import com.sraft.core.role.Leader;
import com.sraft.enums.EnumLoginStatus;

import io.netty.channel.ChannelHandlerContext;

public class LoginWorker extends Workder {
	private static Logger LOG = LoggerFactory.getLogger(LoginWorker.class);

	private SimpleRaftClient client;

	public LoginWorker() {

	}

	public LoginWorker(SimpleRaftClient client) {
		this.client = client;
		setEnable(true);
	}

	@Override
	public void doWork(Object object) {
		List<Object> params = (List<Object>) object;
		ChannelHandlerContext ctx = (ChannelHandlerContext) params.get(0);
		Object msg = params.get(1);
		if (msg instanceof LoginMsg) {
			LoginMsg loginMsg = (LoginMsg) params.get(1);
			dealLoginMsg(ctx, loginMsg);
		} else if (msg instanceof ReplyLoginMsg) {
			ReplyLoginMsg replyLoginMsg = (ReplyLoginMsg) params.get(1);
			dealReplyLoginMsg(replyLoginMsg);
		}
	}

	private void dealLoginMsg(ChannelHandlerContext ctx, LoginMsg loginMsg) {
		ReplyLoginMsg replyLoginMsg = new ReplyLoginMsg();
		replyLoginMsg.setMsgType(Msg.TYPE_REPLY_CLIENT_LOGIN);
		replyLoginMsg.setMsgId(IdGenerateHelper.getMsgId());
		replyLoginMsg.setSendTime(DateHelper.formatDate2Long(new Date(), DateHelper.YYYYMMDDHHMMSSsss));
		if (role instanceof Follower) {
			Follower follower = (Follower) role;
			int leaderId = follower.getLeaderId();
			if (leaderId != -1) {
				ServerAddress serverAddress = follower.getRoleController().getConfig().getServerAddressMap()
						.get(leaderId);
				replyLoginMsg.setRemark(serverAddress.getHost() + ":" + serverAddress.getPort());
			}
			replyLoginMsg.setSessionId(loginMsg.getSessionId());
			replyLoginMsg.setResult(Msg.RETURN_STATUS_FALSE);
			replyLoginMsg.setErrCode(Msg.ERR_CODE_LOGIN_FOLLOWER);
		} else if (role instanceof Candidate) {
			replyLoginMsg.setSessionId(loginMsg.getSessionId());
			replyLoginMsg.setResult(Msg.RETURN_STATUS_FALSE);
			replyLoginMsg.setErrCode(Msg.ERR_CODE_LOGIN_CANDIDATE);
		} else if (role instanceof Leader) {
			Leader leader = (Leader) role;
			long receviceTime = loginMsg.getReceviceTime();
			long sessionId = loginMsg.getSessionId();
			if (sessionId == -1) {
				//领导者正常
				if (leader.isAliveOverHalf()) {
					// 分配新会话
					sessionId = IdGenerateHelper.getNextSessionId();
					leader.addSession(sessionId, receviceTime, -1);
					replyLoginMsg.setSessionId(sessionId);
					replyLoginMsg.setResult(Msg.RETURN_STATUS_OK);
				} else {
					//领导者不正常，没有过半存活跟随者
					replyLoginMsg.setResult(Msg.RETURN_STATUS_FALSE);
					replyLoginMsg.setErrCode(Msg.ERR_CODE_LOGIN_LEADER_NO_MAJOR);
				}

			} else {
				boolean isUpdate = leader.updateSession(sessionId, receviceTime, -1);
				if (!isUpdate) {
					LOG.info("会话已过期,分配新会话");
					//领导者正常
					if (leader.isAliveOverHalf()) {
						// 分配新会话
						sessionId = IdGenerateHelper.getNextSessionId();
						leader.addSession(sessionId, receviceTime, -1);
						replyLoginMsg.setSessionId(sessionId);
						replyLoginMsg.setResult(Msg.RETURN_STATUS_OK);
					} else {
						//领导者不正常，没有过半存活跟随者，不分配新会话
						replyLoginMsg.setResult(Msg.RETURN_STATUS_FALSE);
						replyLoginMsg.setErrCode(Msg.ERR_CODE_LOGIN_LEADER_NO_MAJOR);
					}
				} else {
					replyLoginMsg.setSessionId(sessionId);
					replyLoginMsg.setResult(Msg.RETURN_STATUS_OK);
				}
			}
		}
		ctx.writeAndFlush(replyLoginMsg);
	}

	public void dealReplyLoginMsg(ReplyLoginMsg replyLoginMsg) {
		int result = replyLoginMsg.getResult();
		if (result == Msg.RETURN_STATUS_OK) {
			LOG.info("登录成功");
			// 有可能在心跳超时内，没有登录成功，重新登录，可能造成重复登录，避免会话ID被后面的登录修改
			if (!client.isLogin()) {
				client.updateSessionId(replyLoginMsg.getSessionId());
				client.updateLoginStatus(EnumLoginStatus.OK);
				client.updateLastReceiveMsg(replyLoginMsg);
			}
		} else {
			int errCode = replyLoginMsg.getErrCode();
			switch (errCode) {
			case Msg.ERR_CODE_LOGIN_FOLLOWER:
				LOG.error("连接到跟随者,需要重新登录");
				String remark = replyLoginMsg.getRemark();
				if (StringHelper.checkIsNotNull(remark)) {
					String[] leaderArr = remark.split(":");
					String host = leaderArr[0].trim();
					int port = Integer.parseInt(leaderArr[1].trim());
					AddrManager.getInstance().addAddr(host, port);
				}
				break;
			case Msg.ERR_CODE_LOGIN_CANDIDATE:
				LOG.error("连接到候选者,需要重新登录");
				break;
			case Msg.ERR_CODE_LOGIN_LEADER_NO_MAJOR:
				LOG.error("由于没有过半存活机器，领导者暂停服务,需要重新登录");
				break;
			default:
				break;
			}
			client.updateLoginStatus(EnumLoginStatus.FALSE);
		}
	}
}
