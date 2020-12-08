package com.sraft.core.role.sender;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sraft.common.DateHelper;
import com.sraft.common.IdGenerateHelper;
import com.sraft.common.flow.IFlowWorker;
import com.sraft.core.log.LogData;
import com.sraft.core.message.AppendLogEntryMsg;
import com.sraft.core.message.BaseLog;
import com.sraft.core.message.Msg;
import com.sraft.core.message.Packet;
import com.sraft.core.message.ReplyAppendLogEntryMsg;
import com.sraft.core.net.ConnManager;
import com.sraft.core.net.ServerAddress;
import com.sraft.core.role.AppendTask;
import com.sraft.core.role.FollowStatus;
import com.sraft.core.role.Leader;
import com.sraft.enums.EnumAppendLogResult;
import com.sraft.enums.EnumNodeStatus;

public class SendAppendLogWorker implements IFlowWorker {
	private static Logger LOG = LoggerFactory.getLogger(SendAppendLogWorker.class);
	private Leader leader;
	private ServerAddress serverAddress;
	private FollowStatus followStatus;
	/**
	 * 是否是领导者节点
	 */
	private boolean isLeader = false;

	public SendAppendLogWorker(ServerAddress serverAddress, Leader leader, boolean isLeader) {
		this.isLeader = isLeader;
		this.leader = leader;
		if (!isLeader) {
			this.serverAddress = serverAddress;
			this.followStatus = leader.getFollowStatusMap().get(serverAddress.getNodeId());
		}
	}

	@Override
	public void deliver(Object object) {
		// 需要判断是否 领导者节点
		AppendLogEntryMsg appendLogEntryMsg = (AppendLogEntryMsg) object;
		if (isLeader) {
			appendLogEntryLocally(appendLogEntryMsg);
		} else {
			if (appendLogEntryMsg.getAppendType() == AppendLogEntryMsg.TYPE_APPEND_ORDINARY) {
				appendLogEntry(appendLogEntryMsg);
			} else {
				synLogEntry(appendLogEntryMsg);
			}
		}

	}

	public void appendLogEntryLocally(AppendLogEntryMsg appendLogEntryMsg) {
		boolean isSuccess = false;
		try {
			int appendResult = leader.getRoleController().getiLogEntry().appendLogEntry(appendLogEntryMsg);
			if (appendResult != EnumAppendLogResult.LOG_APPEND_SUCCESS.getValue()) {
				LOG.error("【严重异常，写日志到本地出错，退出程序！！！】");
				isSuccess = false;
			} else {
				isSuccess = true;
			}
		} catch (IOException e) {
			isSuccess = false;
			e.printStackTrace();
			LOG.error(e.getMessage(), e);
			LOG.error("【严重异常，写日志到本地出错，退出程序！！！】");
			System.exit(0);
		} finally {
			synchronized (leader.getPendingQueue()) {
				AppendTask appendTask = leader.getPendingQueue().peek();
				if (isSuccess) {
					appendTask.increSuccessNum();
					if (appendTask.isOverHalfSuccess()) {
						appendTask.notify();
					}
				} else {
					appendTask.increSuccessNum();
					if (appendTask.isOverHalfFail()) {
						appendTask.notify();
					}
				}
			}
		}
	}

	public boolean appendLogEntry(AppendLogEntryMsg appendLogEntryMsg) {
		int nodeId = followStatus.getNodeId();
		boolean isAppend = false;
		// 先检查节点的状态，只有正常状态才能追加日志
		if (followStatus.getStatus() != EnumNodeStatus.NODE_NORMAL) {
			LOG.error("节点:{},状态为：{},不能追加日志", nodeId, followStatus.getStatus());
			leader.updateAppendTask(false);
			return isAppend;
		}
		Packet packet = new Packet();
		packet.setSendMsg(appendLogEntryMsg);
		boolean isSend = false;
		synchronized (followStatus.getPendingQueue()) {
			isSend = ConnManager.getInstance().sendMsg(serverAddress, appendLogEntryMsg);
			// 发送成功
			if (isSend) {
				followStatus.getPendingQueue().add(packet);
			} else {
				LOG.info("节点:{},追加日志失败", nodeId);
				followStatus.setStatus(EnumNodeStatus.NODE_LOG_UNSYN);
			}
		}
		try {
			if (isSend) {
				// 发送成功就开始等待结果，但有超时限制
				synchronized (packet) {
					packet.wait(1000);
				}
				followStatus.getPendingQueue().clear();
				if (packet.getReplyMsg() == null) {
					LOG.info("节点:{},没有响应", nodeId, packet.toString());
					followStatus.setStatus(EnumNodeStatus.NODE_LOG_UNSYN);
					leader.updateAppendTask(false);
				} else {
					ReplyAppendLogEntryMsg replyAppendLogEntryMsg = (ReplyAppendLogEntryMsg) packet.getReplyMsg();
					int result = replyAppendLogEntryMsg.getResult();
					if (result == Msg.RETURN_STATUS_OK) {
						LOG.info("节点:{},追加日志处理完成:{}", nodeId, packet.toString());
						// 更新跟随者已复制ID
						followStatus.setMatchIndex(appendLogEntryMsg.getBaseLogList()
								.get(appendLogEntryMsg.getBaseLogList().size() - 1).getLogIndex());
						leader.updateAppendTask(true);
						isAppend = true;
					} else {
						int errCode = replyAppendLogEntryMsg.getErrCode();
						switch (errCode) {
						case Msg.ERR_CODE_LOG_CHECK_FALSE:
							LOG.info("节点:{},一致性检查失败", nodeId);
							break;
						case Msg.ERR_CODE_LOG_NULL_SERVER:
							LOG.info("节点:{},空服务器,需要先发送快照", nodeId);
							break;
						case Msg.ERR_CODE_LOG_LARGE_TERM:
							LOG.info("节点:{},发现任期更大的服务器，丢弃消息", nodeId);
							break;
						case Msg.ERR_CODE_LOG_CANDIDATE:
							LOG.error("节点:{},出现候选者，心跳超时或出现其它异常", nodeId);
							break;
						case Msg.ERR_CODE_LOG_LEADER:
							LOG.error("节点:{},出现领导者，网络分区或出现其它异常", nodeId);
							break;
						default:
							break;
						}
						followStatus.setStatus(EnumNodeStatus.NODE_LOG_UNSYN);
						leader.updateAppendTask(false);
					}
				}
			} else {
				followStatus.setStatus(EnumNodeStatus.NODE_DEAD);
				leader.updateAppendTask(false);
			}
		} catch (InterruptedException e) {
			leader.updateAppendTask(false);
			e.printStackTrace();
			LOG.error(e.getMessage(), e);
		}
		return isAppend;
	}

	public boolean synLogEntry(AppendLogEntryMsg appendLogEntryMsg) {
		boolean isAppend = false;
		int nodeId = followStatus.getNodeId();
		Packet packet = new Packet();
		packet.setSendMsg(appendLogEntryMsg);
		boolean isSend = false;
		synchronized (followStatus.getPendingQueue()) {
			isSend = ConnManager.getInstance().sendMsg(serverAddress, appendLogEntryMsg);
			// 发送成功
			if (isSend) {
				followStatus.getPendingQueue().add(packet);
			} else {
				LOG.info("节点:{},追加日志失败", nodeId);
				followStatus.setStatus(EnumNodeStatus.NODE_LOG_UNSYN);
			}
		}
		try {
			if (isSend) {
				// 发送成功就开始等待结果，但有超时限制
				synchronized (packet) {
					packet.wait(1000);
				}
				followStatus.getPendingQueue().clear();
				LOG.info("节点:{},追加日志处理完成:{}", nodeId, packet.toString());
				if (packet.getReplyMsg() == null) {
					LOG.info("节点:{},没有响应", nodeId, packet.toString());
					followStatus.setStatus(EnumNodeStatus.NODE_LOG_UNSYN);
				} else {
					ReplyAppendLogEntryMsg replyAppendLogEntryMsg = (ReplyAppendLogEntryMsg) packet.getReplyMsg();
					int result = replyAppendLogEntryMsg.getResult();
					if (result == Msg.RETURN_STATUS_OK) {
						// 更新跟随者已发送ID
						if (appendLogEntryMsg.getAppendType() == AppendLogEntryMsg.TYPE_APPEND_NULL) {
							followStatus.setMatchIndex(appendLogEntryMsg.getPrevLogIndex());
						} else {
							followStatus.setMatchIndex(appendLogEntryMsg.getBaseLogList()
									.get(appendLogEntryMsg.getBaseLogList().size() - 1).getLogIndex());
						}
						followStatus.setStatus(EnumNodeStatus.NODE_NORMAL);
						isAppend = true;
					} else {
						int errCode = replyAppendLogEntryMsg.getErrCode();
						switch (errCode) {
						case Msg.ERR_CODE_LOG_CHECK_FALSE:
							LOG.info("节点:{},一致性检查失败,需要同步数据", nodeId);
							dealCheckConsistenctyFalse(appendLogEntryMsg);
							break;
						case Msg.ERR_CODE_LOG_NULL_SERVER:
							// 说明之前发送的不是第一条日志，并且跟随者是个新服务器，先发送快照，再发送所有已有日志
							LOG.info("节点:{},空服务器,需要先发送快照", nodeId);
							appendSnapshot();
							synAllLogEntry();
							break;
						case Msg.ERR_CODE_LOG_LARGE_TERM:
							// 丢弃消息
							LOG.info("节点:{},发现任期更大的服务器,丢弃消息", nodeId);
							break;
						case Msg.ERR_CODE_LOG_CANDIDATE:
							LOG.error("节点:{},出现候选者,心跳超时或出现其它异常", nodeId);
							followStatus.setStatus(EnumNodeStatus.NODE_LOG_UNSYN);
							break;
						case Msg.ERR_CODE_LOG_LEADER:
							LOG.error("节点:{},出现领导者,网络分区或出现其它异常", nodeId);
							followStatus.setStatus(EnumNodeStatus.NODE_LOG_UNSYN);
							break;
						default:
							break;
						}
					}
				}
			} else {
				followStatus.setStatus(EnumNodeStatus.NODE_DEAD);
			}
		} catch (InterruptedException e) {
			followStatus.setStatus(EnumNodeStatus.NODE_LOG_UNSYN);
			e.printStackTrace();
			LOG.error(e.getMessage(), e);
		}
		return isAppend;
	}

	/**
	 * 递减日志索引，发送上一条日志
	 * 
	 * @param failLogEntryMsg
	 */
	public void dealCheckConsistenctyFalse(AppendLogEntryMsg failLogEntryMsg) {
		LogData logData = leader.getRoleController().getiLogEntry()
				.getLogDataByIndex(failLogEntryMsg.getPrevLogIndex());
		// logData == null 只有两种可能
		// （1）getPrevLogIndex 为-1，那就没有上一条日志；所以找不到日志；但这样的话，发送给跟随者时，必定能成功，不会一致性检查失败，可以直接清空跟随者的日志
		// （2）这是第一条日志，旧日志已经生成了快照，所以，应该发送快照了
		if (logData == null) {
			//发送快照
			appendSnapshot();
		} else {
			long prevLogIndex = logData.getLogIndex() - 1;
			long prevLogTerm = -1;
			if (prevLogIndex != -1) {
				LogData preLogData = leader.getRoleController().getiLogEntry().getLogDataByIndex(prevLogIndex);
				// 说明是第一条日志，prevLogTerm应该取快照最后一条数据任期
				if (preLogData == null) {
					prevLogTerm = leader.getRoleController().getiLogEntry().getLastSnapTerm();
				} else {
					prevLogTerm = preLogData.getLogTerm();
				}
			}
			int appendType = failLogEntryMsg.getAppendType();
			AppendLogEntryMsg msg = new AppendLogEntryMsg();
			if (appendType == AppendLogEntryMsg.TYPE_APPEND_NULL) {
				msg.setAppendType(AppendLogEntryMsg.TYPE_APPEND_SYN);
			} else if (appendType == AppendLogEntryMsg.TYPE_APPEND_SYN) {
				msg.setAppendType(AppendLogEntryMsg.TYPE_APPEND_SYN);
			} else if (appendType == AppendLogEntryMsg.TYPE_APPEND_ORDINARY) {
				msg.setAppendType(AppendLogEntryMsg.TYPE_APPEND_ORDINARY);
			}
			msg.setLeaderCommit(leader.getRoleController().getiStatement().getLastCommitId());
			msg.setMsgId(IdGenerateHelper.getMsgId());
			msg.setMsgType(Msg.TYPE_APPEND_LOG);
			msg.setNodeId(leader.getSelfId());
			msg.setPrevLogIndex(prevLogIndex);
			msg.setPrevLogTerm(prevLogTerm);
			msg.setSendTime(DateHelper.formatDate2Long(new Date(), DateHelper.YYYYMMDDHHMMSSsss));
			msg.setTerm(leader.getCurrentTerm());
			msg.setTransactionId(IdGenerateHelper.getNextSessionId());
			List<BaseLog> baseLogList = new ArrayList<BaseLog>();
			baseLogList.add(tranLogData2BaseLog(logData));
			msg.setBaseLogList(baseLogList);
			synLogEntry(msg);
		}
	}

	public BaseLog tranLogData2BaseLog(LogData logData) {
		BaseLog baseLog = new BaseLog();
		baseLog.setClientSessionId(logData.getClientSessionId());
		baseLog.setClientTransactionId(baseLog.getClientTransactionId());
		baseLog.setCreateTime(logData.getCreateTime());
		baseLog.setKey(logData.getKey());
		baseLog.setLeaderId(logData.getLeaderId());
		baseLog.setLogIndex(logData.getLogIndex());
		baseLog.setLogTerm(logData.getLogTerm());
		baseLog.setLogType(logData.getLogType());
		baseLog.setSraftTransactionId(logData.getSraftTransactionId());
		baseLog.setUpdateTime(logData.getUpdateTime());
		baseLog.setValue(logData.getValue());
		return baseLog;
	}

	/**
	 * 既然要发送快照,跟随者在接收第一条快照是，就应该清空所有已有的快照和日志数据
	 */
	public void appendSnapshot() {

	}

	/**
	 * 对空服务器，需要将所有已经日志发送给它
	 */
	public void synAllLogEntry() {

	}
}
