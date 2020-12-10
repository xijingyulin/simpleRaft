package com.sraft.core.role.worker;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sraft.common.DateHelper;
import com.sraft.core.log.ILogSnap;
import com.sraft.core.message.Msg;
import com.sraft.core.message.ReplyRequestVoteMsg;
import com.sraft.core.message.RequestVoteMsg;
import com.sraft.core.role.Candidate;
import com.sraft.core.role.Leader;
import com.sraft.enums.EnumRole;

import io.netty.channel.ChannelHandlerContext;

public class RequestVoteWorker extends Workder {
	private static Logger LOG = LoggerFactory.getLogger(RequestVoteWorker.class);

	@Override
	public void doWork(Object object) {

		List<Object> params = (List<Object>) object;
		ChannelHandlerContext ctx = (ChannelHandlerContext) params.get(0);
		Object msg = params.get(1);
		if (msg instanceof RequestVoteMsg) {
			RequestVoteMsg requestVoteMsg = (RequestVoteMsg) params.get(1);
			dealRequestVoteMsg(ctx, requestVoteMsg);
		} else if (msg instanceof ReplyRequestVoteMsg) {
			ReplyRequestVoteMsg replyRequestVoteMsg = (ReplyRequestVoteMsg) params.get(1);
			dealReplyRequestVoteMsg(replyRequestVoteMsg);
		}
	}

	/**
	 * 处理接收到的【请求投票消息】
	 */
	public void dealRequestVoteMsg(ChannelHandlerContext ctx, RequestVoteMsg requestVoteMsg) {

		long fromTerm = requestVoteMsg.getTerm();
		//boolean isLargeTerm = isLargeTerm(fromTerm);
		boolean isPassTerm = checkTerm(fromTerm);
		boolean isPassLog = checkLogIndexAndTerm(requestVoteMsg.getLastLogTerm(), requestVoteMsg.getLastLogIndex());
		ReplyRequestVoteMsg reply = new ReplyRequestVoteMsg();
		reply.setMsgType(Msg.TYPE_REPLY_REQUEST_VOTE);
		reply.setMsgId(requestVoteMsg.getMsgId());
		reply.setNodeId(role.getSelfId());
		reply.setTerm(role.getCurrentTerm());
		reply.setSendTime(DateHelper.formatDate2Long(new Date(), DateHelper.YYYYMMDDHHMMSSsss));

		if (isPassTerm && isPassLog) {
			reply.setResult(Msg.RETURN_STATUS_OK);
			role.updateVotedFor(requestVoteMsg.getNodeId());
		} else {
			reply.setResult(Msg.RETURN_STATUS_FALSE);
		}
		ctx.writeAndFlush(reply);
		// 是否需要更新任期，比自己大就要更新
		boolean isUpdate = role.updateTerm(fromTerm);
		// （1）遇到任期更大的，更新自己任期，转成跟随者（2）或者投票成功，也要转
		if (isUpdate || (isPassTerm && isPassLog)) {
			if (role instanceof Candidate) {
				Candidate candidate = (Candidate) role;
				candidate.nextRole(EnumRole.FOLLOWER);
			} else if (role instanceof Leader) {
				Leader leader = (Leader) role;
				leader.nextRole(EnumRole.FOLLOWER);
			}
		}
	}

	/**
	 * 处理接收到的【请求投票消息的回复】
	 */
	public void dealReplyRequestVoteMsg(ReplyRequestVoteMsg replyRequestVoteMsg) {
		if (role instanceof Candidate) {
			Candidate candidate = (Candidate) role;
			int result = replyRequestVoteMsg.getResult();
			if (result == Msg.RETURN_STATUS_OK) {
				candidate.addVoted();
				if (candidate.isVotedOverHalf()) {
					LOG.info("获取过半投票,转换成领导者");
					candidate.nextRole(EnumRole.LEADER);
				}
			} else {
				long fromTerm = replyRequestVoteMsg.getTerm();
				// 遇到更大的任期，转成跟随者
				if (candidate.updateTerm(fromTerm)) {
					candidate.nextRole(EnumRole.FOLLOWER);
				}
			}
		} else {
			LOG.info("角色已转换，丢弃该消息");
		}
	}

	/**
	 * 比较任期
	 * 
	 * @param fromTerm
	 * @return
	 */
	public boolean checkTerm(long fromTerm) {
		boolean isPass = false;
		if (fromTerm > role.getCurrentTerm()) {
			isPass = true;
		} else if (fromTerm == role.getCurrentTerm()) {
			// 在该任期已经给其它候选人投票过了
			if (role.getVotedFor() != -1) {
				isPass = false;
			} else {
				isPass = true;
			}
		} else {
			isPass = false;
		}
		return isPass;
	}

	/**
	 * 比较日志任期和索引
	 * 
	 * （1）先比较任期，比自己大的，就投票
	 * 
	 * （2）任期相等，比较索引，不比自己小，就投票
	 * 
	 * （3）否则，拒绝投票
	 * 
	 * @param fromTerm
	 * @param fromIndex
	 * @return
	 */
	public boolean checkLogIndexAndTerm(long fromTerm, long fromIndex) {
		boolean isPass = false;
		ILogSnap iLogEntry = role.getRoleController().getiLogSnap();
		long currentLogTerm = iLogEntry.getLastLogTerm();
		long currentLogIndex = iLogEntry.getLastLogTerm();
		if (fromTerm > currentLogTerm) {
			isPass = true;
		} else if (fromTerm == currentLogTerm) {
			if (fromIndex >= currentLogIndex) {
				isPass = true;
			} else {
				isPass = false;
			}
		} else {
			isPass = false;
		}
		return isPass;
	}
}
