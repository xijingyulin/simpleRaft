package com.sraft.core.role.worker;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sraft.common.DateHelper;
import com.sraft.common.IdGenerateHelper;
import com.sraft.core.message.HeartbeatMsg;
import com.sraft.core.message.Msg;
import com.sraft.core.message.ReplyHeartbeatMsg;
import com.sraft.core.role.Candidate;
import com.sraft.core.role.Follower;
import com.sraft.core.role.Leader;
import com.sraft.enums.EnumRole;

import io.netty.channel.ChannelHandlerContext;

public class HeartbeatWorker extends Workder {
	private static Logger LOG = LoggerFactory.getLogger(HeartbeatWorker.class);

	@Override
	public void doWork(Object object) {
		List<Object> params = (List<Object>) object;
		ChannelHandlerContext ctx = (ChannelHandlerContext) params.get(0);
		Object msg = params.get(1);
		if (msg instanceof HeartbeatMsg) {
			HeartbeatMsg heartbeatMsg = (HeartbeatMsg) params.get(1);
			dealHeartbeatMsgMsg(ctx, heartbeatMsg);
		} else if (msg instanceof ReplyHeartbeatMsg) {
			ReplyHeartbeatMsg replyHeartbeatMsg = (ReplyHeartbeatMsg) params.get(1);
			dealReplyHeartbeatMsg(replyHeartbeatMsg);
		}
	}

	public void dealHeartbeatMsgMsg(ChannelHandlerContext ctx, HeartbeatMsg heartbeatMsg) {
		long fromTerm = heartbeatMsg.getTerm();
		boolean isPassTerm = checkTerm(fromTerm);
		ReplyHeartbeatMsg replyHeartbeatMsg = new ReplyHeartbeatMsg();
		replyHeartbeatMsg.setMsgId(IdGenerateHelper.getMsgId());
		replyHeartbeatMsg.setMsgType(Msg.TYPE_REPLY_HEARTBEAT);
		replyHeartbeatMsg.setNodeId(role.getRoleController().getConfig().getSelfId());
		replyHeartbeatMsg.setSendTime(DateHelper.formatDate2Long(new Date(), DateHelper.YYYYMMDDHHMMSSsss));
		replyHeartbeatMsg.setTerm(role.getCurrentTerm());
		if (isPassTerm) {
			replyHeartbeatMsg.setResult(Msg.RETURN_STATUS_OK);
		} else {
			replyHeartbeatMsg.setResult(Msg.RETURN_STATUS_FALSE);
		}
		ctx.writeAndFlush(replyHeartbeatMsg);
		// 是否需要更新任期，比自己大就要更新
		boolean isUpdate = role.updateTerm(fromTerm);
		// （1）遇到任期更大的，更新自己任期，转成跟随者
		if (isPassTerm) {
			if (role instanceof Candidate) {
				Candidate candidate = (Candidate) role;
				candidate.setHeartbeatMsg(heartbeatMsg);
				candidate.nextRole(EnumRole.FOLLOWER);
			} else if (role instanceof Leader) {
				if (!isUpdate) {
					LOG.error("【严重故障,同任期存在两个领导者】");
					System.exit(0);
				}
				Leader leader = (Leader) role;
				leader.nextRole(EnumRole.FOLLOWER);
			} else {
				Follower follower = (Follower) role;
				follower.setHeartbeatMsg(heartbeatMsg);
				follower.setLeaderId(heartbeatMsg.getNodeId());
				follower.updateSession(heartbeatMsg.getSessionMap());
			}
		}
	}

	public boolean checkTerm(long fromTerm) {
		if (fromTerm >= role.getCurrentTerm()) {
			return true;
		} else {
			return false;
		}
	}

	public void dealReplyHeartbeatMsg(ReplyHeartbeatMsg replyHeartbeatMsg) {
		if (role instanceof Leader) {
			Leader leader = (Leader) role;
			int result = replyHeartbeatMsg.getResult();
			if (result == Msg.RETURN_STATUS_OK) {
				leader.getLastReceiveMsgMap().put(replyHeartbeatMsg.getNodeId(), replyHeartbeatMsg);
			} else {
				long fromTerm = replyHeartbeatMsg.getTerm();
				// 是否需要更新任期，比自己大就要更新
				boolean isUpdate = role.updateTerm(fromTerm);
				if (isUpdate) {
					leader.nextRole(EnumRole.FOLLOWER);
				}
			}
		} else {
			LOG.info("角色已转换，丢弃该消息");
		}
	}

}
