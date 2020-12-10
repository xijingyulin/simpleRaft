package com.sraft.core.role;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sraft.common.DateHelper;
import com.sraft.common.IdGenerateHelper;
import com.sraft.common.flow.FlowHeader;
import com.sraft.common.flow.IFlowWorker;
import com.sraft.common.flow.NoFlowLineException;
import com.sraft.core.log.LogData;
import com.sraft.core.message.AppendLogEntryMsg;
import com.sraft.core.message.AppendSnapshotMsg;
import com.sraft.core.message.BaseLog;
import com.sraft.core.message.ClientActionMsg;
import com.sraft.core.message.HeartbeatMsg;
import com.sraft.core.message.Msg;
import com.sraft.core.message.Packet;
import com.sraft.core.net.ConnManager;
import com.sraft.core.net.ServerAddress;
import com.sraft.core.role.sender.SendAppendLogWorker;
import com.sraft.core.role.sender.SendHeartbeartWorker;
import com.sraft.core.schedule.ScheduleSession;
import com.sraft.core.schedule.ScheduleHeartbeat;
import com.sraft.core.session.Session;
import com.sraft.enums.EnumNodeStatus;
import com.sraft.enums.EnumRole;

public class Leader extends AbstractRoles implements ILeader {
	private static Logger LOG = LoggerFactory.getLogger(Leader.class);
	private List<ServerAddress> connAddressList;
	public static final String HEARTBEAT_WORKER = "HEARTBEAT_WORK:";
	public static final String APPEND_LOG_WORKER = "APPEND_LOG_WORKER:";
	/**
	 * 追加日志的追加任务
	 */
	private BlockingQueue<AppendTask> pendingQueue = new LinkedBlockingQueue<AppendTask>();

	/**
	 * 状态为正常的节点过半，才是真的过半；待完善
	 */
	private volatile boolean isAliveOverHalf = true;

	/**
	 * key:节点id value:节点状态
	 */
	private Map<Integer, FollowStatus> followStatusMap = new ConcurrentHashMap<Integer, FollowStatus>();

	public Leader(RoleController roleController) throws IOException {
		super(EnumRole.LEADER, roleController);
		connAddressList = roleController.getConfig().getConnAddressList();
		IdGenerateHelper.initializeNextSession(selfId);
	}

	@Override
	public void run() {
		try {
			LOG.info("初始化每个节点状态");
			initNodeStatus();
			LOG.info("对每个跟随者分配消息发送通道");
			assignWorker();
			LOG.info("激活消息接收通道");
			enableWorker(this);
			LOG.info("立即发送心跳");
			sendHeartbeat();
			LOG.info("设置心跳超时");
			startHeartbeat();
			LOG.info("设置会话超时");
			startSessionTimeout();
		} catch (Throwable e) {
			e.printStackTrace();
			LOG.error(e.getMessage(), e);
			LOG.error("启动候选者异常,退出程序");
		}
	}

	@Override
	public void sendEmptyLog(int nodeId) {
		AppendLogEntryMsg emptyLogMsg = getEmptyLogMsg();
		emptyLogMsg.setAppendType(AppendLogEntryMsg.TYPE_APPEND_NULL);
		emptyLogMsg.setPrevLogIndex(roleController.getiLogSnap().getLastLogIndex());
		emptyLogMsg.setPrevLogTerm(roleController.getiLogSnap().getLastLogTerm());

		String actionWorker = getAppendWorkerCard(nodeId);
		try {
			FlowHeader.putProducts(actionWorker, emptyLogMsg);
		} catch (NoFlowLineException e) {
			e.printStackTrace();
			LOG.error(e.getMessage(), e);
		}
	}

	/**
	 * 缺少追加类型，上一条日志的索引和任期，以及日志内容，需要根据具体情况补全
	 * 
	 * @return
	 */
	public AppendLogEntryMsg getEmptyLogMsg() {

		AppendLogEntryMsg msg = new AppendLogEntryMsg();
		msg.setLeaderCommit(roleController.getiStatement().getLastCommitId());
		msg.setMsgId(IdGenerateHelper.getMsgId());
		msg.setMsgType(Msg.TYPE_APPEND_LOG);
		msg.setNodeId(selfId);
		msg.setSendTime(DateHelper.formatDate2Long(new Date(), DateHelper.YYYYMMDDHHMMSSsss));
		msg.setTerm(getCurrentTerm());
		msg.setTransactionId(IdGenerateHelper.getNextSessionId());
		msg.setSessionMap(roleController.getSessionMap());
		return msg;
	}

	public AppendSnapshotMsg getEmptyAppendSnapshotMsg() {
		AppendSnapshotMsg appendSnapshotMsg = new AppendSnapshotMsg();
		appendSnapshotMsg.setMsgId(IdGenerateHelper.getMsgId());
		appendSnapshotMsg.setMsgType(Msg.TYPE_APPEND_SNAPSHOT);
		appendSnapshotMsg.setNodeId(selfId);
		appendSnapshotMsg.setSendTime(DateHelper.formatDate2Long(new Date(), DateHelper.YYYYMMDDHHMMSSsss));
		appendSnapshotMsg.setTerm(getCurrentTerm());
		appendSnapshotMsg.setTransactionId(IdGenerateHelper.getNextSessionId());
		appendSnapshotMsg.setSessionMap(roleController.getSessionMap());
		return appendSnapshotMsg;
	}

	public BaseLog getBaseLog(ClientActionMsg clientActionMsg) {
		BaseLog baseLog = new BaseLog();
		baseLog.setClientSessionId(clientActionMsg.getSessionId());
		baseLog.setClientTransactionId(clientActionMsg.getTransactionId());
		baseLog.setCreateTime(clientActionMsg.getSendTime());
		baseLog.setKey(clientActionMsg.getKey());
		baseLog.setLeaderId(selfId);
		baseLog.setLogIndex(getRoleController().getiLogSnap().getLastLogIndex() + 1);
		baseLog.setLogTerm(getCurrentTerm());
		baseLog.setLogType(clientActionMsg.getActionType());
		baseLog.setSraftTransactionId(IdGenerateHelper.getNextSessionId());
		baseLog.setUpdateTime(clientActionMsg.getSendTime());
		baseLog.setValue(clientActionMsg.getValue());
		return baseLog;
	}

	/**
	 * 提交追加日志任务
	 * 
	 * @param appendTask
	 */
	public void submitAppendTask(AppendTask appendTask) {
		pendingQueue.add(appendTask);
		appendTask.setAllAppendNum(connAddressList.size() + 1);
		BaseLog baseLog = appendTask.getBaseLog();
		for (int i = 0; i <= connAddressList.size(); i++) {
			int nodeId = -1;
			// 发给领导者自己
			if (i == connAddressList.size()) {
				nodeId = selfId;
			} else {
				nodeId = connAddressList.get(i).getNodeId();
			}
			AppendLogEntryMsg emptyMsg = getEmptyLogMsg();
			emptyMsg.setAppendType(AppendLogEntryMsg.TYPE_APPEND_ORDINARY);
			emptyMsg.setPrevLogIndex(roleController.getiLogSnap().getLastLogIndex());
			emptyMsg.setPrevLogTerm(roleController.getiLogSnap().getLastLogTerm());
			List<BaseLog> baseLogList = new ArrayList<BaseLog>();
			baseLogList.add(baseLog);
			emptyMsg.setBaseLogList(baseLogList);
			String appendWorker = getAppendWorkerCard(nodeId);
			try {
				FlowHeader.putProducts(appendWorker, emptyMsg);
			} catch (NoFlowLineException e) {
				e.printStackTrace();
				LOG.error(e.getMessage(), e);
			}
		}

	}

	public void initNodeStatus() {
		for (ServerAddress serverAddress : connAddressList) {
			int nodeId = serverAddress.getNodeId();
			FollowStatus followStatus = new FollowStatus(nodeId);
			followStatus.setStatus(EnumNodeStatus.NODE_DEAD);
			followStatus.setMatchIndex(0);
			followStatus.setPendingQueue(new LinkedBlockingQueue<Packet>());
			followStatusMap.put(nodeId, followStatus);
		}
	}

	public static String getAppendWorkerCard(int nodeId) {
		return APPEND_LOG_WORKER + nodeId;
	}

	public static String getHeartbeatWorkerCard(int nodeId) {
		return HEARTBEAT_WORKER + nodeId;
	}

	/**
	 * 给每个跟随者分配专门的消息通道
	 */
	public void assignWorker() {
		for (int i = 0; i <= connAddressList.size(); i++) {
			int nodeId = -1;
			ServerAddress serverAddress = null;
			boolean isLeader = false;
			if (i == connAddressList.size()) {
				serverAddress = null;
				nodeId = selfId;
				isLeader = true;
			} else {
				// 领导者自己不需心跳通道
				serverAddress = connAddressList.get(i);
				nodeId = serverAddress.getNodeId();
				String heartbeatWorkerCard = getHeartbeatWorkerCard(nodeId);
				IFlowWorker heartbeatWorker = new SendHeartbeartWorker(serverAddress, this);
				FlowHeader.employ(heartbeatWorkerCard, heartbeatWorker);

			}
			String actionWorkerCard = getAppendWorkerCard(nodeId);
			IFlowWorker actionWorker = new SendAppendLogWorker(null, this, isLeader);
			FlowHeader.employ(actionWorkerCard, actionWorker);
		}
	}

	/**
	 * 删除每个跟随者的消息通道
	 */
	public void fireWorker() {
		for (int i = 0; i <= connAddressList.size(); i++) {
			int nodeId = -1;
			if (i == connAddressList.size()) {
				nodeId = selfId;
			} else {
				nodeId = connAddressList.get(i).getNodeId();
				String heartbeatWorkerCard = getHeartbeatWorkerCard(nodeId);
				FlowHeader.unEmploy(heartbeatWorkerCard);
				FollowStatus followStatus = followStatusMap.get(nodeId);
				BlockingQueue<Packet> followPendingQueue = followStatus.getPendingQueue();
				synchronized (followPendingQueue) {
					while (!followPendingQueue.isEmpty()) {
						try {
							Packet packet = followPendingQueue.take();
							synchronized (packet) {
								packet.notify();
							}
						} catch (InterruptedException e) {
							e.printStackTrace();
							LOG.error(e.getMessage(), e);
						}
					}
				}
			}
			String actionWorkerCard = getAppendWorkerCard(nodeId);
			FlowHeader.unEmploy(actionWorkerCard);
		}
	}

	@Override
	public void sendHeartbeat() {
		for (ServerAddress serverAddress : connAddressList) {
			HeartbeatMsg heartBeartMsg = getHeartbeatMsg();
			String workerCard = getHeartbeatWorkerCard(serverAddress.getNodeId());
			try {
				FlowHeader.putProducts(workerCard, heartBeartMsg);
			} catch (NoFlowLineException e) {
				e.printStackTrace();
			}
		}
	}

	public HeartbeatMsg getHeartbeatMsg() {
		HeartbeatMsg msg = new HeartbeatMsg();
		msg.setLeaderCommit(roleController.getiStatement().getLastCommitId());
		msg.setMsgId(IdGenerateHelper.getMsgId());
		msg.setMsgType(Msg.TYPE_HEARTBEAT);
		msg.setNodeId(selfId);
		msg.setTerm(getCurrentTerm());
		msg.setSendTime(DateHelper.formatDate2Long(new Date(), DateHelper.YYYYMMDDHHMMSSsss));
		msg.getSessionMap().putAll(roleController.getSessionMap());
		return msg;
	}

	/**
	 * 转换角色
	 * 
	 * 清除通道
	 * 
	 * 删除定时器
	 * 
	 * @param newRole
	 */
	@Override
	public synchronized void nextRole(EnumRole newRole) {
		if (isChangedRole) {
			return;
		}
		isChangedRole = true;
		LOG.info("转换角色，关闭心跳超时");
		stopHeartbeat();
		LOG.info("转换角色,禁用消息接收通道...");
		disableWorker();
		LOG.info("转换角色,删除每个跟随者的消息发送通道...");
		fireWorker();
		if (newRole == EnumRole.FOLLOWER) {
			LOG.info("转换角色,关闭连接其它服务器...");
			ConnManager.getInstance().closeAll();
		}
		roleController.changeRole(newRole);
	}

	public List<ServerAddress> getConnAddressList() {
		return connAddressList;
	}

	public boolean isAliveOverHalf() {
		return isAliveOverHalf;
	}

	public void setAliveOverHalf(boolean isAliveOverHalf) {
		this.isAliveOverHalf = isAliveOverHalf;
	}

	/**
	 * 在心跳时间内是否仍然有过半节点存活
	 * 
	 * @return
	 */
	public void checkNodeStatus(int checkRange) {
		try {
			//算上领导者自己
			int countAlive = 1;
			int half = roleController.getConfig().getServerAddressMap().size() / 2;
			long minHeartTime = DateHelper.addMillSecond(new Date(), -checkRange);
			boolean isAlive = false;
			for (ServerAddress serverAddress : connAddressList) {
				isAlive = false;
				int nodeId = serverAddress.getNodeId();
				FollowStatus followStatus = followStatusMap.get(nodeId);
				Msg lastMsg = followStatus.getLastReceviceMsg();
				if (lastMsg != null) {
					long receviceTime = lastMsg.getReceviceTime();
					if (receviceTime >= minHeartTime) {
						isAlive = true;
					}
				}
				if (isAlive) {
					if (followStatus.getStatus() == EnumNodeStatus.NODE_DEAD
							|| followStatus.getStatus() == EnumNodeStatus.NODE_LOG_UNSYN) {
						followStatus.setStatus(EnumNodeStatus.NODE_LOG_SYNING);
						sendEmptyLog(nodeId);
					} else if (followStatus.getStatus() == EnumNodeStatus.NODE_NORMAL) {
						countAlive++;
					}
				} else {
					followStatus.setStatus(EnumNodeStatus.NODE_DEAD);
				}
			}
			if (countAlive > half) {
				isAliveOverHalf = true;
			} else {
				isAliveOverHalf = false;
			}
		} catch (ParseException e) {
			e.printStackTrace();
			LOG.error(e.getMessage(), e);
		}
	}

	public void updateAppendTask(boolean isSuccess) {
		synchronized (pendingQueue) {
			AppendTask appendTask = pendingQueue.peek();
			if (appendTask != null) {
				if (isSuccess) {
					appendTask.increSuccessNum();
					if (appendTask.isOverHalfSuccess()) {
						appendTask.notify();
						pendingQueue.clear();
					}
				} else {
					appendTask.increFailNum();
					if (appendTask.isOverHalfFail()) {
						appendTask.notify();
						pendingQueue.clear();
					}
				}
			}
		}
	}

	/**
	 * 设置心跳超时定时器
	 */
	public void startHeartbeat() {
		ScheduleHeartbeat.getInstance().schedule(roleController.getConfig().getTickTime(), this);
	}

	/**
	 * 停止选举超时
	 */
	public void stopHeartbeat() {
		ScheduleHeartbeat.getInstance().stop();
	}

	public void startSessionTimeout() {
		ScheduleSession.getInstance().schedule(roleController.getConfig().getTickTime(), this);
	}

	public void stopSessionTimeout() {
		ScheduleSession.getInstance().stop();
	}

	/**
	 * 检查过期会话并删除
	 * 
	 * @return
	 */
	public void expiredSession(int checkRange) {
		try {
			long minSessionTime = DateHelper.addMillSecond(new Date(), -checkRange);
			Map<Long, Session> sessionMap = roleController.getSessionMap();
			Iterator<Long> it = sessionMap.keySet().iterator();
			while (it.hasNext()) {
				Long sessionId = it.next();
				Session session = sessionMap.get(sessionId);
				if (session.getLastReceiveTime() < minSessionTime) {
					LOG.info("【客户端会话过期:{}】", sessionId);
					it.remove();
				}
			}
		} catch (ParseException e) {
			e.printStackTrace();
			LOG.error(e.getMessage(), e);
		}
	}

	public Map<Integer, FollowStatus> getFollowStatusMap() {
		return followStatusMap;
	}

	public BlockingQueue<AppendTask> getPendingQueue() {
		return pendingQueue;
	}

}
