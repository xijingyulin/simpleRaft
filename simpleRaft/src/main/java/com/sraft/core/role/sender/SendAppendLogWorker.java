package com.sraft.core.role.sender;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sraft.common.flow.IFlowWorker;
import com.sraft.core.log.LogData;
import com.sraft.core.log.Snapshot;
import com.sraft.core.message.AppendLogEntryMsg;
import com.sraft.core.message.AppendSnapshotMsg;
import com.sraft.core.message.BaseLog;
import com.sraft.core.message.BaseSnapshot;
import com.sraft.core.message.Msg;
import com.sraft.core.message.Packet;
import com.sraft.core.message.ReplyAppendLogEntryMsg;
import com.sraft.core.message.ReplyAppendSnapshotMsg;
import com.sraft.core.net.ConnManager;
import com.sraft.core.net.ServerAddress;
import com.sraft.core.role.FollowStatus;
import com.sraft.core.role.Leader;
import com.sraft.enums.EnumAppendLogResult;
import com.sraft.enums.EnumNodeStatus;

public class SendAppendLogWorker implements IFlowWorker {
	private static Logger LOG = LoggerFactory.getLogger(SendAppendLogWorker.class);
	private Leader leader;
	private ServerAddress serverAddress;
	private FollowStatus followStatus;

	private static final int DATA_BATCH_NUM = 2000;

	public SendAppendLogWorker(ServerAddress serverAddress, Leader leader) {
		this.leader = leader;
		this.serverAddress = serverAddress;
		this.followStatus = leader.getFollowStatusMap().get(serverAddress.getNodeId());
	}

	@Override
	public void deliver(Object object) {
		// 需要判断是否 领导者节点
		AppendLogEntryMsg appendLogEntryMsg = (AppendLogEntryMsg) object;
		if (appendLogEntryMsg.getAppendType() == AppendLogEntryMsg.TYPE_APPEND_ORDINARY) {
			appendLogEntry(appendLogEntryMsg);
		} else {
			synLogEntry(appendLogEntryMsg);
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
			}
		}
		try {
			if (isSend) {
				// 发送成功就开始等待结果，但有超时限制
				synchronized (packet) {
					packet.wait(1000);
				}
				synchronized (followStatus.getPendingQueue()) {
					followStatus.getPendingQueue().clear();
				}
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
							LOG.error("节点:{},其它异常", nodeId);
							break;
						}
						followStatus.setStatus(EnumNodeStatus.NODE_LOG_UNSYN);
						leader.updateAppendTask(false);
					}
				}
			} else {
				LOG.error("节点:{},发送追加日志消息失败", nodeId);
				followStatus.setStatus(EnumNodeStatus.NODE_DEAD);
				leader.updateAppendTask(false);
			}
		} catch (InterruptedException e) {
			followStatus.setStatus(EnumNodeStatus.NODE_LOG_UNSYN);
			leader.updateAppendTask(false);
			e.printStackTrace();
			LOG.error(e.getMessage(), e);
		}
		return isAppend;
	}

	public boolean synLogEntry(AppendLogEntryMsg appendLogEntryMsg) {
		if (leader.isChangedRole()) {
			followStatus.setStatus(EnumNodeStatus.NODE_LOG_UNSYN);
			LOG.error("角色已改变,不再是领导者");
			return false;
		}
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
			}
		}
		try {
			if (isSend) {
				// 发送成功就开始等待结果，但有超时限制
				synchronized (packet) {
					packet.wait(1000);
				}
				followStatus.getPendingQueue().clear();
				LOG.info("节点:{},同步日志处理完成:{}", nodeId, packet.toString());
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
							followStatus.setStatus(EnumNodeStatus.NODE_NORMAL);
							LOG.info("节点:{},初始化同步数据完成", nodeId);
						} else {
							followStatus.setMatchIndex(appendLogEntryMsg.getBaseLogList()
									.get(appendLogEntryMsg.getBaseLogList().size() - 1).getLogIndex());
						}
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
							if (synSnapshot()) {
								LOG.info("同步快照成功");
								if (synAllLogEntry()) {
									LOG.info("同步日志成功");
								}
							}
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
				LOG.info("节点:{},发送日志失败", nodeId);
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
		LogData logData = leader.getRoleController().getiLogSnap().getLogDataByIndex(failLogEntryMsg.getPrevLogIndex());
		// logData == null 只有两种可能
		// （1）getPrevLogIndex 为-1，那就没有上一条日志；所以找不到日志；但这样的话，发送给跟随者时，必定能成功，不会一致性检查失败，可以直接清空跟随者的日志
		// （2）这是第一条日志，旧日志已经生成了快照，所以，应该发送快照了
		if (logData == null) {
			//发送快照
			synSnapshot();
		} else {
			long prevLogIndex = logData.getLogIndex() - 1;
			long prevLogTerm = -1;
			if (prevLogIndex != -1) {
				LogData preLogData = leader.getRoleController().getiLogSnap().getLogDataByIndex(prevLogIndex);
				// 说明是第一条日志，prevLogTerm应该取快照最后一条数据任期
				if (preLogData == null) {
					prevLogTerm = leader.getRoleController().getiLogSnap().getLastSnapTerm();
				} else {
					prevLogTerm = preLogData.getLogTerm();
				}
			}
			AppendLogEntryMsg msg = leader.getEmptyLogMsg();
			msg.setAppendType(AppendLogEntryMsg.TYPE_APPEND_SYN);
			msg.setPrevLogIndex(prevLogIndex);
			msg.setPrevLogTerm(prevLogTerm);
			List<BaseLog> baseLogList = new ArrayList<BaseLog>();
			baseLogList.add(tranLogData2BaseLog(logData));
			msg.setBaseLogList(baseLogList);
			boolean isSuccess = synLogEntry(msg);
			if (isSuccess) {
				long synPrevLogIndex = logData.getLogIndex();
				long synPrevLogTerm = logData.getLogTerm();
				while (true) {
					long matchIndex = followStatus.getMatchIndex();
					long leaderLogLastIndex = leader.getRoleController().getiLogSnap().getLastLogIndex();
					if (matchIndex == leaderLogLastIndex) {
						leader.sendEmptyLog(followStatus.getNodeId());
						break;
					}
					long beginLogIndex = matchIndex + 1;
					List<LogData> logDataList = leader.getRoleController().getiLogSnap()
							.getLogDataByCount(beginLogIndex, DATA_BATCH_NUM);
					List<BaseLog> synBaseLogList = tranLogData2BaseLog(logDataList);
					AppendLogEntryMsg synMsg = leader.getEmptyLogMsg();
					synMsg.setAppendType(AppendLogEntryMsg.TYPE_APPEND_SYN);
					synMsg.setPrevLogIndex(synPrevLogIndex);
					synMsg.setPrevLogTerm(synPrevLogTerm);
					synMsg.setBaseLogList(synBaseLogList);

					if (synLogEntry(synMsg)) {
						LOG.info("同步日志成功,开始位置:{}", beginLogIndex);
						synPrevLogIndex = synBaseLogList.get(synBaseLogList.size() - 1).getLogIndex();
						synPrevLogTerm = synBaseLogList.get(synBaseLogList.size() - 1).getLogTerm();
					} else {
						LOG.error("严重异常,同步日志失败:{}", synMsg.toString());
						break;
					}
				}
			}
		}
	}

	public List<BaseLog> tranLogData2BaseLog(List<LogData> logDataList) {
		List<BaseLog> baseLogList = new ArrayList<BaseLog>();
		for (LogData logData : logDataList) {
			baseLogList.add(tranLogData2BaseLog(logData));
		}
		return baseLogList;
	}

	public BaseLog tranLogData2BaseLog(LogData logData) {
		BaseLog baseLog = new BaseLog();
		baseLog.setClientSessionId(logData.getClientSessionId());
		baseLog.setClientTransactionId(logData.getClientTransactionId());
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

	public List<BaseSnapshot> transSnapshot2BaseSnapshot(List<Snapshot> snapshotList) {
		List<BaseSnapshot> baseSnapshotList = new ArrayList<BaseSnapshot>();
		for (Snapshot snapshot : snapshotList) {
			BaseSnapshot baseSnapshot = new BaseSnapshot();
			baseSnapshot.setbKey(snapshot.getbKey());
			baseSnapshot.setbValue(snapshot.getbValue());
			baseSnapshot.setLogIndex(snapshot.getLogIndex());
			baseSnapshot.setLogTerm(snapshot.getLogTerm());
			baseSnapshotList.add(baseSnapshot);
		}
		return baseSnapshotList;
	}

	/**
	 * 既然要发送快照,跟随者在接收第一条快照是，就应该清空所有已有的快照和日志数据
	 */
	public boolean synSnapshot() {
		boolean isSuccess = true;
		long beginSnapshotIndex = 0;
		long prevSnapIndex = -1;
		long prevSnapTerm = -1;
		while (true) {
			List<Snapshot> snapshotList = leader.getRoleController().getiLogSnap().getSnapshotList(beginSnapshotIndex,
					DATA_BATCH_NUM);
			if (snapshotList.size() == 0) {
				break;
			}
			List<BaseSnapshot> baseSnapshotList = transSnapshot2BaseSnapshot(snapshotList);
			AppendSnapshotMsg appendSnapshotMsg = leader.getEmptyAppendSnapshotMsg();
			appendSnapshotMsg.setPrevSnapIndex(prevSnapIndex);
			appendSnapshotMsg.setPrevSnapTerm(prevSnapTerm);
			appendSnapshotMsg.setBaseSnapshot(baseSnapshotList);
			if (appendSnapshot(appendSnapshotMsg)) {
				prevSnapIndex = baseSnapshotList.get(baseSnapshotList.size() - 1).getLogIndex();
				prevSnapTerm = baseSnapshotList.get(baseSnapshotList.size() - 1).getLogTerm();
				beginSnapshotIndex = prevSnapIndex + 1;
				LOG.info("发送快照成功");
			} else {
				isSuccess = false;
				LOG.error("严重异常,同步快照失败:{}", appendSnapshotMsg.toString());
				break;
			}
		}
		return isSuccess;
	}

	public boolean appendSnapshot(AppendSnapshotMsg appendSnapshotMsg) {
		if (leader.isChangedRole()) {
			LOG.error("角色已改变,不再是领导者");
			followStatus.setStatus(EnumNodeStatus.NODE_LOG_UNSYN);
			return false;
		}
		int nodeId = followStatus.getNodeId();
		boolean isAppend = false;
		Packet packet = new Packet();
		packet.setSendMsg(appendSnapshotMsg);
		boolean isSend = false;
		synchronized (followStatus.getPendingQueue()) {
			isSend = ConnManager.getInstance().sendMsg(serverAddress, appendSnapshotMsg);
			// 发送成功
			if (isSend) {
				followStatus.getPendingQueue().add(packet);
			} else {
				LOG.info("节点:{},追加快照失败", nodeId);
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
				} else {
					ReplyAppendSnapshotMsg replyAppendSnapshotMsg = (ReplyAppendSnapshotMsg) packet.getReplyMsg();
					int result = replyAppendSnapshotMsg.getResult();
					if (result == Msg.RETURN_STATUS_OK) {
						LOG.info("节点:{},追加快照处理完成:{}", nodeId, packet.toString());
						// 更新跟随者已复制ID
						isAppend = true;
					} else {
						int errCode = replyAppendSnapshotMsg.getErrCode();
						switch (errCode) {
						case Msg.ERR_CODE_LOG_CHECK_FALSE:
							LOG.info("节点:{},发送快照,一致性检查失败", nodeId);
							break;
						case Msg.ERR_CODE_LOG_LARGE_TERM:
							LOG.info("节点:{},发送快照,发现任期更大的服务器，丢弃消息", nodeId);
							break;
						case Msg.ERR_CODE_LOG_CANDIDATE:
							LOG.error("节点:{},发送快照,出现候选者，心跳超时或出现其它异常", nodeId);
							break;
						case Msg.ERR_CODE_LOG_LEADER:
							LOG.error("节点:{},发送快照,出现领导者，网络分区或出现其它异常", nodeId);
							break;
						default:
							LOG.error("节点:{},发送快照,出现其它异常！！！", nodeId);
							break;
						}
						followStatus.setStatus(EnumNodeStatus.NODE_LOG_UNSYN);
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
	 * 对空服务器，需要将所有已经日志发送给它
	 */
	public boolean synAllLogEntry() {
		boolean isSuccess = true;
		long prevLogIndex = leader.getRoleController().getiLogSnap().getLastSnapIndex();
		long prevLogTerm = leader.getRoleController().getiLogSnap().getLastSnapTerm();
		while (true) {
			long beginLogIndex = followStatus.getMatchIndex() + 1;
			long lastLogTerm = leader.getRoleController().getiLogSnap().getLastLogIndex();
			if (beginLogIndex == lastLogTerm) {
				leader.sendEmptyLog(followStatus.getNodeId());
				break;
			}

			List<LogData> logDataList = leader.getRoleController().getiLogSnap().getLogDataByCount(beginLogIndex,
					DATA_BATCH_NUM);
			List<BaseLog> baseLogList = tranLogData2BaseLog(logDataList);
			AppendLogEntryMsg msg = leader.getEmptyLogMsg();
			msg.setAppendType(AppendLogEntryMsg.TYPE_APPEND_SYN);
			msg.setPrevLogIndex(prevLogIndex);
			msg.setPrevLogTerm(prevLogTerm);
			msg.setBaseLogList(baseLogList);

			if (synLogEntry(msg)) {
				LOG.info("同步日志成功,开始位置:{}", beginLogIndex);
				prevLogIndex = baseLogList.get(baseLogList.size() - 1).getLogIndex();
				prevLogTerm = baseLogList.get(baseLogList.size() - 1).getLogTerm();
			} else {
				isSuccess = false;
				LOG.error("严重异常,同步日志失败:{}", msg.toString());
				break;
			}
		}
		return isSuccess;
	}
}
