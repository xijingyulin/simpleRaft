package com.sraft.core.role;

import java.io.IOException;
import java.text.ParseException;
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
import com.sraft.core.message.AppendLogEntryMsg;
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
	 * key:节点id value:收到每个节点的最新的消息
	 */
	private Map<Integer, Msg> lastReceiveMsgMap = new ConcurrentHashMap<Integer, Msg>();

	/**
	 * key:节点id value:节点状态
	 */
	private Map<Integer, EnumNodeStatus> nodeStatusMap = new ConcurrentHashMap<Integer, EnumNodeStatus>();
	/**
	 * key:节点id value:日志状态
	 */
	//private Map<Integer, EnumLogStatus> logStatusMap = new ConcurrentHashMap<Integer, EnumLogStatus>();

	/**
	 * key:节点id value:发送的消息队列
	 */
	private Map<Integer, BlockingQueue<Packet>> pendingQueueMap = new ConcurrentHashMap<Integer, BlockingQueue<Packet>>();

	private volatile boolean isAliveOverHalf = true;

	public Leader(RoleController roleController) throws IOException {
		super(EnumRole.LEADER, roleController);
		connAddressList = roleController.getConfig().getConnAddressList();
		IdGenerateHelper.initializeNextSession(roleController.getConfig().getSelfId());
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
			//LOG.info("发送空日志同步数据");
			//sendEmptyLog();
			LOG.info("设置会话超时");
			startSessionTimeout();
		} catch (Throwable e) {
			e.printStackTrace();
			LOG.error(e.getMessage(), e);
			LOG.error("启动候选者异常,退出程序");
		}
	}

	public void sendEmptyLog() {
		AppendLogEntryMsg emptyLogMsg = getEmptyLogMsg();
		for (ServerAddress serverAddress : connAddressList) {
			String actionWorker = APPEND_LOG_WORKER + serverAddress.getNodeId();
			try {
				FlowHeader.putProducts(actionWorker, emptyLogMsg);
			} catch (NoFlowLineException e) {
				e.printStackTrace();
				LOG.error(e.getMessage(), e);
			}
		}
	}

	@Override
	public void sendEmptyLog(int nodeId) {
		AppendLogEntryMsg emptyLogMsg = getEmptyLogMsg();
		String actionWorker = APPEND_LOG_WORKER + nodeId;
		try {
			FlowHeader.putProducts(actionWorker, emptyLogMsg);
		} catch (NoFlowLineException e) {
			e.printStackTrace();
			LOG.error(e.getMessage(), e);
		}
	}

	public AppendLogEntryMsg getEmptyLogMsg() {
		AppendLogEntryMsg msg = new AppendLogEntryMsg();
		msg.setAppendType(AppendLogEntryMsg.TYPE_APPEND_NULL);
		msg.setLeaderCommit(roleController.getiStatement().getLastCommitId());
		msg.setMsgId(IdGenerateHelper.getMsgId());
		msg.setMsgType(Msg.TYPE_APPEND_LOG);
		msg.setNodeId(roleController.getConfig().getSelfId());
		msg.setPrevLogIndex(roleController.getiLogEntry().getLastLogIndex());
		msg.setPrevLogTerm(roleController.getiLogEntry().getLastLogTerm());
		msg.setSendTime(DateHelper.formatDate2Long(new Date(), DateHelper.YYYYMMDDHHMMSSsss));
		msg.setTerm(getCurrentTerm());
		msg.setTransactionId(IdGenerateHelper.getNextSessionId());
		return msg;
	}

	public void initNodeStatus() {
		for (ServerAddress serverAddress : connAddressList) {
			nodeStatusMap.put(serverAddress.getNodeId(), EnumNodeStatus.NODE_DEAD);
			//logStatusMap.put(serverAddress.getNodeId(), EnumLogStatus.LOG_INCONSISTENCY);
		}
	}

	/**
	 * 给每个跟随者分配专门的消息通道
	 */
	public void assignWorker() {
		for (ServerAddress serverAddress : connAddressList) {
			String heartbeatWorkerCard = HEARTBEAT_WORKER + serverAddress.getNodeId();
			IFlowWorker heartbeatWorker = new SendHeartbeartWorker(serverAddress, this);
			FlowHeader.employ(heartbeatWorkerCard, heartbeatWorker);

			BlockingQueue<Packet> pendingQueue = new LinkedBlockingQueue<Packet>();
			pendingQueueMap.put(serverAddress.getNodeId(), pendingQueue);
			String actionWorkerCard = APPEND_LOG_WORKER + serverAddress.getNodeId();
			IFlowWorker actionWorker = new SendAppendLogWorker(serverAddress, this);
			FlowHeader.employ(actionWorkerCard, actionWorker);
		}
	}

	/**
	 * 删除每个跟随者的消息通道
	 */
	public void fireWorker() {
		for (ServerAddress serverAddress : connAddressList) {
			String heartbeatWorkerCard = HEARTBEAT_WORKER + serverAddress.getNodeId();
			FlowHeader.unEmploy(heartbeatWorkerCard);

			String actionWorkerCard = APPEND_LOG_WORKER + serverAddress.getNodeId();
			FlowHeader.unEmploy(actionWorkerCard);
		}
	}

	@Override
	public void sendHeartbeat() {
		for (ServerAddress serverAddress : connAddressList) {
			HeartbeatMsg heartBeartMsg = getHeartbeatMsg();
			String workerCard = HEARTBEAT_WORKER + serverAddress.getNodeId();
			try {
				FlowHeader.putProducts(workerCard, heartBeartMsg);
			} catch (NoFlowLineException e) {
				e.printStackTrace();
			}
		}
	}

	public HeartbeatMsg getHeartbeatMsg() {
		HeartbeatMsg msg = new HeartbeatMsg();
		msg.setMsgId(IdGenerateHelper.getMsgId());
		msg.setMsgType(Msg.TYPE_HEARTBEAT);
		msg.setNodeId(roleController.getConfig().getSelfId());
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

	public Map<Integer, Msg> getLastReceiveMsgMap() {
		return lastReceiveMsgMap;
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
				Msg lastMsg = lastReceiveMsgMap.get(nodeId);
				if (lastMsg != null) {
					long receviceTime = lastMsg.getReceviceTime();
					if (receviceTime >= minHeartTime) {
						isAlive = true;
					}
				}
				if (isAlive) {
					countAlive++;
				}
				updateNodeStatus(nodeId, isAlive);
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

	/**
	 * 心跳专用，心跳超时，才会设为EnumNodeStatus.NODE_DEAD
	 * 
	 * @param nodeId
	 * @param isAlive
	 */
	public void updateNodeStatus(int nodeId, boolean isAlive) {
		synchronized (nodeStatusMap) {
			EnumNodeStatus status = nodeStatusMap.get(nodeId);
			if (isAlive) {
				if (status == EnumNodeStatus.NODE_DEAD || status == EnumNodeStatus.NODE_LOG_UNSYN) {
					nodeStatusMap.put(nodeId, EnumNodeStatus.NODE_LOG_SYNING);
					sendEmptyLog(nodeId);
				}
			} else {
				nodeStatusMap.put(nodeId, EnumNodeStatus.NODE_DEAD);
				BlockingQueue<Packet> pendingQueue = pendingQueueMap.get(nodeId);
				if (pendingQueue != null && !pendingQueue.isEmpty()) {
					synchronized (pendingQueue) {
						try {
							Packet packet = pendingQueue.take();
							packet.notify();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	/**
	 * 心跳有效时才能更新
	 * 
	 * @param nodeId
	 * @param newStatus
	 */
	public void updateNodeStatus(int nodeId, EnumNodeStatus newStatus) {
		synchronized (nodeStatusMap) {
			if (nodeStatusMap.get(nodeId) != EnumNodeStatus.NODE_DEAD) {
				nodeStatusMap.put(nodeId, newStatus);
			}
		}
	}

	public EnumNodeStatus getNodeStatus(int nodeId) {
		return nodeStatusMap.get(nodeId);
	}

	public Map<Integer, EnumNodeStatus> getNodeStatusMap() {
		return nodeStatusMap;
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

	public Map<Integer, BlockingQueue<Packet>> getPendingQueueMap() {
		return pendingQueueMap;
	}
}
