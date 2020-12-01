package com.sraft.core.role;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sraft.common.DateHelper;
import com.sraft.common.IdGenerateHelper;
import com.sraft.common.flow.FlowHeader;
import com.sraft.common.flow.IFlowWorker;
import com.sraft.common.flow.NoFlowLineException;
import com.sraft.core.message.HeartbeatMsg;
import com.sraft.core.message.Msg;
import com.sraft.core.net.ConnManager;
import com.sraft.core.net.ServerAddress;
import com.sraft.core.role.sender.LeaderMsgWork;
import com.sraft.core.schedule.ScheduleSession;
import com.sraft.core.schedule.ScheduleHeartbeat;
import com.sraft.core.session.Session;
import com.sraft.enums.EnumRole;

public class Leader extends AbstractRoles implements ILeader {
	private static Logger LOG = LoggerFactory.getLogger(Leader.class);
	private List<ServerAddress> connAddressList;
	public static final String WORKCARD = "WORKCARD";
	/**
	 * key:节点id value:收到每个节点的最新的消息
	 */
	private Map<Integer, Msg> lastReceiveMsgMap = new ConcurrentHashMap<Integer, Msg>();

	/**
	 * key:节点id value:节点是否存活
	 */
	private Map<Integer, Boolean> nodeStatusMap = new ConcurrentHashMap<Integer, Boolean>();

	private volatile boolean isAliveOverHalf = true;

	public Leader(RoleController roleController) throws IOException {
		super(EnumRole.LEADER, roleController);
		connAddressList = roleController.getConfig().getConnAddressList();
		IdGenerateHelper.initializeNextSession(roleController.getConfig().getSelfId());
	}

	@Override
	public void run() {
		try {
			LOG.info("初始化每个节点存活状态");
			initNodeStatus();
			LOG.info("对每个跟随者分配消息发送通道");
			assignWorker();
			LOG.info("激活消息接收通道");
			enableWorker(this);
			LOG.info("立即发送心跳");
			sendHeartbeat();
			LOG.info("设置心跳超时");
			startHeartbeat();
			LOG.info("发送空日志同步数据");
			LOG.info("激活客户端消息通道");
			enableClientWorker(this);
			LOG.info("设置会话超时");
			startSessionTimeout();
		} catch (Throwable e) {
			e.printStackTrace();
			LOG.error(e.getMessage(), e);
			LOG.error("启动候选者异常,退出程序");
		}
	}

	public void initNodeStatus() {
		for (ServerAddress serverAddress : connAddressList) {
			nodeStatusMap.put(serverAddress.getNodeId(), true);
		}
	}

	/**
	 * 给每个跟随者分配专门的消息通道
	 */
	public void assignWorker() {
		for (ServerAddress serverAddress : connAddressList) {
			String workerCard = WORKCARD + serverAddress.getNodeId();
			IFlowWorker worker = new LeaderMsgWork(serverAddress);
			FlowHeader.employ(workerCard, worker);
		}
	}

	/**
	 * 删除每个跟随者的消息通道
	 */
	public void fireWorker() {
		for (ServerAddress serverAddress : connAddressList) {
			String workerCard = WORKCARD + serverAddress.getNodeId();
			FlowHeader.unEmploy(workerCard);
		}
	}

	@Override
	public void sendHeartbeat() {
		HeartbeatMsg heartBeartMsg = getHeartbeatMsg();
		for (ServerAddress serverAddress : connAddressList) {
			String workerCard = WORKCARD + serverAddress.getNodeId();
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
		return msg;
	}

	@Override
	public void enableWorker(AbstractRoles role) {
		roleController.getHeatBeatWorkder().setRole(role);
		roleController.getHeatBeatWorkder().setEnable(true);

		roleController.getAppendLogWorkder().setRole(role);
		roleController.getAppendLogWorkder().setEnable(true);

		roleController.getRequestVoteWorker().setRole(role);
		roleController.getRequestVoteWorker().setEnable(true);
	}

	/**
	 * 同步完日志后，再处理客户端的消息
	 * 
	 * @param role
	 */
	public void enableClientWorker(AbstractRoles role) {
		roleController.getLoginWorkder().setRole(role);
		roleController.getLoginWorkder().setEnable(true);

		roleController.getClientHeartbeatWorker().setRole(role);
		roleController.getClientHeartbeatWorker().setEnable(true);

		roleController.getClientActionWorkder().setRole(role);
		roleController.getClientActionWorkder().setEnable(true);
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

	public Map<Integer, Boolean> getNodeStatusMap() {
		return nodeStatusMap;
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
					nodeStatusMap.put(nodeId, true);
				} else {
					nodeStatusMap.put(nodeId, false);
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
			//算上领导者自己
			long minSessionTime = DateHelper.addMillSecond(new Date(), -checkRange);
			Map<Long, Session> sessionMap = roleController.getSessionMap();
			Iterator<Long> it = sessionMap.keySet().iterator();
			while (it.hasNext()) {
				Long sessionId = it.next();
				Session session = sessionMap.get(sessionId);
				if (session.getLastReceiveTime() < minSessionTime) {
					it.remove();
				}
			}
		} catch (ParseException e) {
			e.printStackTrace();
			LOG.error(e.getMessage(), e);
		}
	}

	public synchronized boolean updateSession(long newSessionId, long newLastReceiveTime,
			long newLastClientTransactionId) {
		boolean isUpdate = false;
		Map<Long, Session> sessionMap = roleController.getSessionMap();
		Session oldSession = sessionMap.get(newSessionId);
		if (oldSession == null) {
			isUpdate = false;
		} else {
			isUpdate = true;
			oldSession.setLastReceiveTime(newLastReceiveTime);
			if (newLastClientTransactionId != -1) {
				oldSession.setLastClientTransactionId(newLastClientTransactionId);
			}
		}
		return isUpdate;
	}
}
