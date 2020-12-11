package com.sraft.core.role.worker;

import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sraft.common.DateHelper;
import com.sraft.common.IdGenerateHelper;
import com.sraft.core.message.AppendLogEntryMsg;
import com.sraft.core.message.AppendSnapshotMsg;
import com.sraft.core.message.Msg;
import com.sraft.core.message.Packet;
import com.sraft.core.message.ReplyAppendLogEntryMsg;
import com.sraft.core.message.ReplyAppendSnapshotMsg;
import com.sraft.core.role.Candidate;
import com.sraft.core.role.FollowStatus;
import com.sraft.core.role.Follower;
import com.sraft.core.role.Leader;
import com.sraft.enums.EnumAppendLogResult;
import com.sraft.enums.EnumAppendSnapshotResult;
import com.sraft.enums.EnumRole;

import io.netty.channel.ChannelHandlerContext;

public class AppendLogWorkder extends Workder {
	private static Logger LOG = LoggerFactory.getLogger(AppendLogWorkder.class);

	@Override
	public void doWork(Object object) {
		List<Object> params = (List<Object>) object;
		ChannelHandlerContext ctx = (ChannelHandlerContext) params.get(0);
		Object msg = params.get(1);
		if (msg instanceof AppendLogEntryMsg) {
			AppendLogEntryMsg appendLogEntryMsg = (AppendLogEntryMsg) params.get(1);
			dealAppendLogEntryMsg(ctx, appendLogEntryMsg);
		} else if (msg instanceof ReplyAppendLogEntryMsg) {
			ReplyAppendLogEntryMsg replyAppendLogEntryMsg = (ReplyAppendLogEntryMsg) params.get(1);
			dealReplyAppendLogEntryMsg(replyAppendLogEntryMsg);
		} else if (msg instanceof AppendSnapshotMsg) {
			AppendSnapshotMsg appendSnapshotMsg = (AppendSnapshotMsg) params.get(1);
			dealAppendSnapshotMsg(ctx, appendSnapshotMsg);
		} else if (msg instanceof ReplyAppendSnapshotMsg) {
			ReplyAppendSnapshotMsg replyAppendSnapshotMsg = (ReplyAppendSnapshotMsg) params.get(1);
			dealReplyAppendSnapshotMsg(replyAppendSnapshotMsg);
		}
	}

	public void dealAppendLogEntryMsg(ChannelHandlerContext ctx, AppendLogEntryMsg appendLogEntryMsg) {
		long fromTerm = appendLogEntryMsg.getTerm();
		boolean isPassTerm = checkTerm(fromTerm);
		ReplyAppendLogEntryMsg replyAppendLogEntryMsg = new ReplyAppendLogEntryMsg();
		boolean isUpdate = role.updateTerm(fromTerm);
		// （1）遇到任期更大的，更新自己任期，转成跟随者
		if (isPassTerm) {
			role.getRoleController().updateSession(appendLogEntryMsg.getSessionMap(), false);
			if (role instanceof Candidate) {
				replyAppendLogEntryMsg.setResult(Msg.RETURN_STATUS_FALSE);
				replyAppendLogEntryMsg.setErrCode(Msg.ERR_CODE_LOG_CANDIDATE);
				Candidate candidate = (Candidate) role;
				candidate.setHeartbeatMsg(appendLogEntryMsg);
				candidate.nextRole(EnumRole.FOLLOWER);
			} else if (role instanceof Leader) {
				replyAppendLogEntryMsg.setResult(Msg.RETURN_STATUS_FALSE);
				replyAppendLogEntryMsg.setErrCode(Msg.ERR_CODE_LOG_LEADER);
				if (!isUpdate) {
					LOG.error("【严重故障,同任期存在两个领导者,任期:{}】", fromTerm);
					System.exit(0);
				}
				Leader leader = (Leader) role;
				leader.nextRole(EnumRole.FOLLOWER);
			} else {
				Follower follower = (Follower) role;
				follower.setHeartbeatMsg(appendLogEntryMsg);
				follower.setLeaderId(appendLogEntryMsg.getNodeId());
				//如果有未提交的日志，就先提交旧日志
				follower.getRoleController().commit(appendLogEntryMsg.getLeaderCommit());
				EnumAppendLogResult appendLogResult = follower.getRoleController().appendLogEntry(appendLogEntryMsg);
				switch (appendLogResult) {
				case LOG_NULL:
					replyAppendLogEntryMsg.setResult(Msg.RETURN_STATUS_FALSE);
					replyAppendLogEntryMsg.setErrCode(Msg.ERR_CODE_LOG_NULL_SERVER);
					break;
				case LOG_CHECK_FALSE:
					replyAppendLogEntryMsg.setResult(Msg.RETURN_STATUS_FALSE);
					replyAppendLogEntryMsg.setErrCode(Msg.ERR_CODE_LOG_CHECK_FALSE);
					break;
				case LOG_APPEND_SUCCESS:
					replyAppendLogEntryMsg.setResult(Msg.RETURN_STATUS_OK);
					break;
				default:
					break;
				}
			}
		} else {
			replyAppendLogEntryMsg.setResult(Msg.RETURN_STATUS_FALSE);
			replyAppendLogEntryMsg.setErrCode(Msg.ERR_CODE_LOG_LARGE_TERM);
		}

		replyAppendLogEntryMsg.setAppendType(appendLogEntryMsg.getAppendType());
		replyAppendLogEntryMsg.setMsgId(IdGenerateHelper.getMsgId());
		replyAppendLogEntryMsg.setMsgType(Msg.TYPE_REPLY_APPEND_LOG);
		replyAppendLogEntryMsg.setNodeId(role.getSelfId());
		replyAppendLogEntryMsg.setTerm(role.getCurrentTerm());
		replyAppendLogEntryMsg.setTransactionId(appendLogEntryMsg.getTransactionId());
		replyAppendLogEntryMsg.setSendTime(DateHelper.formatDate2Long(new Date(), DateHelper.YYYYMMDDHHMMSSsss));
		ctx.writeAndFlush(replyAppendLogEntryMsg);

	}

	public void dealReplyAppendLogEntryMsg(ReplyAppendLogEntryMsg replyAppendLogEntryMsg) {
		if ((role instanceof Leader) && !role.isChangedRole()) {
			int nodeId = replyAppendLogEntryMsg.getNodeId();
			Leader leader = (Leader) role;
			FollowStatus followStatus = leader.getFollowStatusMap().get(nodeId);
			followStatus.setLastReceviceMsg(replyAppendLogEntryMsg);
			int result = replyAppendLogEntryMsg.getResult();
			if (result == Msg.RETURN_STATUS_FALSE) {
				if (replyAppendLogEntryMsg.getErrCode() == Msg.ERR_CODE_LOG_LARGE_TERM) {
					boolean isUpdate = leader.updateTerm(replyAppendLogEntryMsg.getTerm());
					if (isUpdate) {
						leader.nextRole(EnumRole.FOLLOWER);
					}
					LOG.info("节点:{},发现任期更大的服务器，丢弃消息", nodeId);
				}
			}
			BlockingQueue<Packet> followPendingQueue = followStatus.getPendingQueue();
			synchronized (followPendingQueue) {
				if (followPendingQueue.isEmpty()) {
					LOG.error("警告,消息返回超时:{}", replyAppendLogEntryMsg.toString());
				} else {
					try {
						Packet packet = followPendingQueue.take();
						synchronized (packet) {
							packet.setReplyMsg(replyAppendLogEntryMsg);
							packet.notify();
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
						LOG.error(e.getMessage(), e);
					}
				}
			}
		} else {
			LOG.info("角色已转换，丢弃该消息");
		}
	}

	public boolean checkTerm(long fromTerm) {
		if (fromTerm >= role.getCurrentTerm()) {
			return true;
		} else {
			return false;
		}
	}

	public void dealAppendSnapshotMsg(ChannelHandlerContext ctx, AppendSnapshotMsg appendSnapshotMsg) {
		long fromTerm = appendSnapshotMsg.getTerm();
		boolean isPassTerm = checkTerm(fromTerm);
		ReplyAppendSnapshotMsg replyAppendSnapshotMsg = new ReplyAppendSnapshotMsg();
		boolean isUpdate = role.updateTerm(fromTerm);
		// （1）遇到任期更大的，更新自己任期，转成跟随者
		if (isPassTerm) {
			role.getRoleController().updateSession(appendSnapshotMsg.getSessionMap(), false);
			if (role instanceof Candidate) {
				replyAppendSnapshotMsg.setResult(Msg.RETURN_STATUS_FALSE);
				replyAppendSnapshotMsg.setErrCode(Msg.ERR_CODE_LOG_CANDIDATE);
				Candidate candidate = (Candidate) role;
				candidate.setHeartbeatMsg(appendSnapshotMsg);
				candidate.nextRole(EnumRole.FOLLOWER);
			} else if (role instanceof Leader) {
				replyAppendSnapshotMsg.setResult(Msg.RETURN_STATUS_FALSE);
				replyAppendSnapshotMsg.setErrCode(Msg.ERR_CODE_LOG_LEADER);
				if (!isUpdate) {
					LOG.error("【严重故障,同任期存在两个领导者,任期:{}】", fromTerm);
					System.exit(0);
				}
				Leader leader = (Leader) role;
				leader.nextRole(EnumRole.FOLLOWER);
			} else {
				Follower follower = (Follower) role;
				follower.setHeartbeatMsg(appendSnapshotMsg);
				follower.setLeaderId(appendSnapshotMsg.getNodeId());
				EnumAppendSnapshotResult appendSnapshotResult = follower.getRoleController()
						.appendSnapshot(appendSnapshotMsg);
				switch (appendSnapshotResult) {
				case SNAPSHOT_APPEND_TRUE:
					replyAppendSnapshotMsg.setResult(Msg.RETURN_STATUS_OK);
					break;
				case SNAPSHOT_APPEND_FALSE:
					replyAppendSnapshotMsg.setResult(Msg.RETURN_STATUS_FALSE);
					replyAppendSnapshotMsg.setErrCode(Msg.ERR_CODE_LOG_CHECK_FALSE);
					break;
				default:
					break;
				}
			}
		} else {
			replyAppendSnapshotMsg.setResult(Msg.RETURN_STATUS_FALSE);
			replyAppendSnapshotMsg.setErrCode(Msg.ERR_CODE_LOG_LARGE_TERM);
		}
		replyAppendSnapshotMsg.setMsgId(IdGenerateHelper.getMsgId());
		replyAppendSnapshotMsg.setMsgType(Msg.TYPE_REPLY_APPEND_LOG);
		replyAppendSnapshotMsg.setNodeId(role.getSelfId());
		replyAppendSnapshotMsg.setTerm(role.getCurrentTerm());
		replyAppendSnapshotMsg.setTransactionId(appendSnapshotMsg.getTransactionId());
		replyAppendSnapshotMsg.setSendTime(DateHelper.formatDate2Long(new Date(), DateHelper.YYYYMMDDHHMMSSsss));
		ctx.writeAndFlush(replyAppendSnapshotMsg);
	}

	public void dealReplyAppendSnapshotMsg(ReplyAppendSnapshotMsg replyAppendSnapshotMsg) {
		if ((role instanceof Leader) && !role.isChangedRole()) {
			int nodeId = replyAppendSnapshotMsg.getNodeId();
			Leader leader = (Leader) role;
			FollowStatus followStatus = leader.getFollowStatusMap().get(nodeId);
			followStatus.setLastReceviceMsg(replyAppendSnapshotMsg);
			int result = replyAppendSnapshotMsg.getResult();
			if (result == Msg.RETURN_STATUS_FALSE) {
				if (replyAppendSnapshotMsg.getErrCode() == Msg.ERR_CODE_LOG_LARGE_TERM) {
					boolean isUpdate = leader.updateTerm(replyAppendSnapshotMsg.getTerm());
					if (isUpdate) {
						leader.nextRole(EnumRole.FOLLOWER);
					}
					LOG.info("节点:{},发现任期更大的服务器，丢弃消息", nodeId);
				}
			}
			BlockingQueue<Packet> followPendingQueue = followStatus.getPendingQueue();
			synchronized (followPendingQueue) {
				if (followPendingQueue.isEmpty()) {
					LOG.error("警告,消息返回超时:{}", replyAppendSnapshotMsg.toString());
				} else {
					try {
						Packet packet = followPendingQueue.take();
						synchronized (packet) {
							packet.setReplyMsg(replyAppendSnapshotMsg);
							packet.notify();
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
						LOG.error(e.getMessage(), e);
					}
				}
			}
		} else {
			LOG.info("角色已转换，丢弃该消息");
		}
	}
}
