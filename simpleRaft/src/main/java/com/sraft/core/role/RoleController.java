package com.sraft.core.role;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sraft.Config;
import com.sraft.common.flow.FlowHeader;
import com.sraft.core.data.IStatement;
import com.sraft.core.data.StatemachineManager;
import com.sraft.core.log.ILogSnap;
import com.sraft.core.log.LogData;
import com.sraft.core.log.LogSnapManager;
import com.sraft.core.message.AppendLogEntryMsg;
import com.sraft.core.message.AppendSnapshotMsg;
import com.sraft.core.message.BaseLog;
import com.sraft.core.message.BaseSnapshot;
import com.sraft.core.message.HeartbeatMsg;
import com.sraft.core.message.ServerMsg;
import com.sraft.core.net.ConnManager;
import com.sraft.core.net.ServerAddress;
import com.sraft.core.role.worker.AppendLogWorkder;
import com.sraft.core.role.worker.ClientActionWorker;
import com.sraft.core.role.worker.ClientHeartbeatWorker;
import com.sraft.core.role.worker.LoginWorker;
import com.sraft.core.role.worker.HeartbeatWorker;
import com.sraft.core.role.worker.RequestVoteWorker;
import com.sraft.core.role.worker.Workder;
import com.sraft.core.session.Session;
import com.sraft.enums.EnumAppendLogResult;
import com.sraft.enums.EnumAppendSnapshotResult;
import com.sraft.enums.EnumRole;

public class RoleController {
	private static Logger LOG = LoggerFactory.getLogger(RoleController.class);

	private EnumRole playRole = EnumRole.FOLLOWER;
	private AbstractRoles role = null;
	private Config config = null;

	private String LOCK_CHANGE_ROLE = new String("LOCK_CHANGE_ROLE");
	private boolean isChanged = false;
	/**
	 * 状态机操作
	 */
	private IStatement iStatement = null;
	/**
	 * 日志和快照持久化操作
	 */
	private ILogSnap iLogSnap = null;
	/**
	 * 持久化任期，和跟随者投票给候选者的ID
	 */
	private TermAndVotedForService termAndVotedForService = null;
	/**
	 * key:会话ID，value:会话信息
	 */
	private Map<Long, Session> sessionMap = new ConcurrentHashMap<Long, Session>();

	private Lock sessionLock = new ReentrantLock();

	private Lock logLock = new ReentrantLock();

	//消息处理操作
	public static final String LOGIN_WORKER = "LOGIN_WORKER";
	public static final String CLIENT_HEARTBEAT_WORKER = "CLIENT_HEARTBEAT_WORKER";
	public static final String CLIENT_ACTION_WORKDER = "CLIENT_ACTION_WORKDER";

	public static final String HEARTBEAT_WORKER = "HEARTBEAT_WORKER";
	public static final String APPEND_LOG_WORKER = "APPEND_LOG_WORKER";
	public static final String REQUEST_VOTE_WORKER = "REQUEST_VOTE_WORKER";
	protected Workder loginWorkder = null;
	protected Workder clientHeartbeatWorker = null;
	protected Workder clientActionWorkder = null;

	protected Workder heatBeatWorkder = null;
	protected Workder appendLogWorkder = null;
	protected Workder requestVoteWorker = null;

	public RoleController(Config config) throws IOException, InterruptedException {
		this.config = config;
		this.iLogSnap = new LogSnapManager(this.config);
		this.iStatement = new StatemachineManager(this.iLogSnap);
		this.termAndVotedForService = new TermAndVotedForService(this.config);
		LOG.info("添加消息传递通道");
		addWorker();
		LOG.info("启动服务");
		openService();
		LOG.info("开始客户端消息通道服务");
		openClientWorker();
		LOG.info("启动状态打印线程");
		new PrintStatusThread().start();
	}

	/**
	 * 注册消息传递通道：
	 * 
	 * （1）处理客户端消息
	 * 
	 * （2）处理心跳消息
	 * 
	 * （3）处理日志消息
	 * 
	 * （4）处理投票请求消息
	 */
	public void addWorker() {
		loginWorkder = new LoginWorker(this);
		clientHeartbeatWorker = new ClientHeartbeatWorker(this);
		clientActionWorkder = new ClientActionWorker(this);

		heatBeatWorkder = new HeartbeatWorker();
		appendLogWorkder = new AppendLogWorkder();
		requestVoteWorker = new RequestVoteWorker();

		FlowHeader.employ(LOGIN_WORKER, loginWorkder);
		FlowHeader.employ(CLIENT_HEARTBEAT_WORKER, clientHeartbeatWorker);
		FlowHeader.employ(CLIENT_ACTION_WORKDER, clientActionWorkder);

		FlowHeader.employ(HEARTBEAT_WORKER, heatBeatWorkder);
		FlowHeader.employ(APPEND_LOG_WORKER, appendLogWorkder);
		FlowHeader.employ(REQUEST_VOTE_WORKER, requestVoteWorker);
	}

	public void play() {
		while (true) {
			if (isChanged) {
				synchronized (LOCK_CHANGE_ROLE) {
					try {
						LOCK_CHANGE_ROLE.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
						LOG.error(e.getMessage(), e);
					}
				}
			}
			if (!isChanged) {
				isChanged = true;
				try {
					if (playRole == EnumRole.FOLLOWER) {
						LOG.info("启动跟随者");
						role = new Follower(this);
						role.start();
					} else if (playRole == EnumRole.CANDIDATE) {
						LOG.info("启动候选者");
						role = new Candidate(this);
						role.start();
					} else if (playRole == EnumRole.LEADER) {
						LOG.info("启动领导者");
						role = new Leader(this);
						role.start();
					} else {
						LOG.info("异常角色");
					}
				} catch (Throwable e) {
					e.printStackTrace();
					LOG.error(e.getMessage(), e);
					LOG.error("角色控制管理异常，退出程序");
					System.exit(0);
					break;
				}
			}
		}
	}

	public void changeRole(EnumRole newRole) {
		LOG.info("转换角色,新角色【{}】", newRole);
		playRole = newRole;
		role.interrupt();
		synchronized (LOCK_CHANGE_ROLE) {
			isChanged = false;
			LOCK_CHANGE_ROLE.notify();
		}
	}

	/**
	 * 开放端口：（1）开放内部通讯端口（2）开放给客户端通讯的端口
	 * 
	 * @throws InterruptedException
	 */
	public void openService() throws InterruptedException {
		ServerAddress serverAddress = config.getServerAddressMap().get(config.getSelfId());
		ConnManager.getInstance().openService(serverAddress.getPort());
		ConnManager.getInstance().openService(config.getClientPort());
	}

	class PrintStatusThread extends Thread {
		StringBuilder sb = null;

		@Override
		public void run() {
			while (true) {
				if (role == null) {
					continue;
				}
				sb = new StringBuilder();
				sb.append("[");
				Set<Long> sessionSet = sessionMap.keySet();
				for (Long sessionId : sessionSet) {
					if (sb.length() > 2) {
						sb.append(",").append(sessionId);
					} else {
						sb.append(sessionId);
					}
				}
				sb.append("]");
				LOG.info("【会话ID:{}】", sb.toString());
				if (role instanceof Follower) {
					Follower follower = (Follower) role;
					int leaderId = follower.getLeaderId();
					long currentTerm = follower.getCurrentTerm();
					ServerMsg lastHeartbeat = follower.getHeartbeatMsg();
					LOG.info("【当前角色:{},当前任期:{},领导者是:{},最后一次心跳接收时间:{}】", "跟随者", currentTerm, leaderId,
							lastHeartbeat == null ? "" : lastHeartbeat.getReceviceTime());

				} else if (role instanceof Leader) {
					Leader leader = (Leader) role;
					int leaderId = config.getSelfId();
					long currentTerm = leader.getCurrentTerm();
					StringBuilder sb = new StringBuilder();
					Map<Integer, FollowStatus> nodeStatusMap = leader.getFollowStatusMap();
					boolean isAllDead = true;
					for (Entry<Integer, FollowStatus> entry : nodeStatusMap.entrySet()) {
						int nodeId = entry.getKey();
						ServerAddress serverAddress = config.getServerAddressMap().get(nodeId);
						sb.append("[").append(serverAddress.getNodeId()).append("_").append(serverAddress.getHost())
								.append(":").append(serverAddress.getPort());
						switch (entry.getValue().getStatus()) {
						case NODE_DEAD:
							sb.append(",宕机]");
							break;
						case NODE_LOG_UNSYN:
							sb.append(",未同步]");
							isAllDead = false;
							break;
						case NODE_LOG_SYNING:
							sb.append(",正在同步]");
							isAllDead = false;
							break;
						case NODE_NORMAL:
							sb.append(",正常运行]");
							isAllDead = false;
							break;
						default:
							break;
						}
					}
					if (isAllDead) {
						LOG.info("【当前角色:{},当前任期:{},领导者是:{},节点状态:所有节点都宕机!停止服务】", "领导者", currentTerm, leaderId);
					} else if (!leader.isAliveOverHalf()) {
						LOG.info("【当前角色:{},当前任期:{},领导者是:{},节点状态:过半节点宕机!停止服务;{}】", "领导者", currentTerm, leaderId,
								sb.toString());
					} else {
						LOG.info("【当前角色:{},当前任期:{},领导者是:{},节点状态:{}】", "领导者", currentTerm, leaderId, sb.toString());
					}
				} else if (role instanceof Candidate) {
					//Candidate candidate = (Candidate) role;
				}

				try {
					sleep(10 * 1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
					LOG.error(e.getMessage(), e);
				}
			}
		}
	}

	public Config getConfig() {
		return config;
	}

	public TermAndVotedForService getTermAndVotedForService() {
		return termAndVotedForService;
	}

	public void setTermAndVotedForService(TermAndVotedForService termAndVotedForService) {
		this.termAndVotedForService = termAndVotedForService;
	}

	public Workder getHeatBeatWorkder() {
		return heatBeatWorkder;
	}

	public Workder getAppendLogWorkder() {
		return appendLogWorkder;
	}

	public Workder getRequestVoteWorker() {
		return requestVoteWorker;
	}

	public Workder getLoginWorkder() {
		return loginWorkder;
	}

	public Workder getClientHeartbeatWorker() {
		return clientHeartbeatWorker;
	}

	public Workder getClientActionWorkder() {
		return clientActionWorkder;
	}

	public Map<Long, Session> getSessionMap() {
		return sessionMap;
	}

	public void setSessionMap(Map<Long, Session> sessionMap) {
		this.sessionMap = sessionMap;
	}

	public synchronized boolean updateSession(long sessionId, long newLastReceiveTime,
			long newLastClientTransactionId) {
		boolean isUpdate = false;
		Session session = sessionMap.get(sessionId);
		if (session == null) {
			isUpdate = false;
		} else {
			isUpdate = true;
			session.setLastReceiveTime(newLastReceiveTime);
			if (newLastClientTransactionId != -1) {
				session.setLastClientTransactionId(newLastClientTransactionId);
			}
		}
		return isUpdate;
	}

	/**
	 * 非领导者使用，更新本地缓存
	 * 
	 * @param newSessionMap
	 */
	public void updateSession(Map<Long, Session> newSessionMap, boolean isUpdateTransaction) {
		sessionLock.lock();
		Iterator<Long> it = sessionMap.keySet().iterator();
		while (it.hasNext()) {
			Long oldSessionId = it.next();
			Session oldSession = sessionMap.get(oldSessionId);
			Session newSession = newSessionMap.get(oldSessionId);
			if (newSession == null) {
				it.remove();
			} else {
				oldSession.setLastReceiveTime(newSession.getLastReceiveTime());
				if (isUpdateTransaction) {
					oldSession.setLastClientTransactionId(newSession.getLastClientTransactionId());
				}
			}
		}
		for (Entry<Long, Session> entry : newSessionMap.entrySet()) {
			Long newSessionId = entry.getKey();
			Session newSession = entry.getValue();
			if (!sessionMap.containsKey(newSessionId)) {
				sessionMap.put(newSessionId, newSession);
			}
		}
		sessionLock.unlock();
	}

	public void addSession(long newSessionId, long newLastReceiveTime, long newLastClientTransactionId) {
		Session session = new Session(newSessionId, newLastReceiveTime, newLastClientTransactionId);
		sessionMap.put(newSessionId, session);
	}

	/**
	 * 无论何时，都应该接收并响应客户端消息
	 */
	public void openClientWorker() {
		loginWorkder.setEnable(true);
		clientHeartbeatWorker.setEnable(true);
		clientActionWorkder.setEnable(true);
	}

	public IStatement getiStatement() {
		return iStatement;
	}

	public ILogSnap getiLogSnap() {
		return iLogSnap;
	}

	public void updateSession(List<BaseLog> baseLogList) {
		sessionLock.lock();
		for (BaseLog baseLog : baseLogList) {
			long sessionId = baseLog.getClientSessionId();
			long clientTransactionId = baseLog.getClientTransactionId();
			Session session = sessionMap.get(sessionId);
			if (session != null) {
				session.setLastClientTransactionId(clientTransactionId);
			}
		}
		sessionLock.unlock();
	}

	public EnumAppendLogResult appendLogEntry(AppendLogEntryMsg appendLogEntryMsg) {
		// 先将之前未提交的日志提交
		logLock.lock();
		EnumAppendLogResult result = iLogSnap.appendLogEntry(appendLogEntryMsg);
		if (result == EnumAppendLogResult.LOG_APPEND_SUCCESS) {
			if (appendLogEntryMsg.getAppendType() == AppendLogEntryMsg.TYPE_APPEND_NULL) {
				updateSession(appendLogEntryMsg.getSessionMap(), true);
			} else if (appendLogEntryMsg.getAppendType() == AppendLogEntryMsg.TYPE_APPEND_ORDINARY) {
				updateSession(appendLogEntryMsg.getBaseLogList());
			}
		}
		if (iLogSnap.isNeedRebootStatemachine()) {
			iStatement.restart();
		} else {
			// 如果不需重启状态机，
			// （1）并且是同步类型的日志，则需要，将日志放到待提交的集合中，并且立刻提交
			// （2）如果只是普通追加日志，则只需放到队列中，不需立刻提交
			if (result == EnumAppendLogResult.LOG_APPEND_SUCCESS) {
				if (appendLogEntryMsg.getAppendType() == AppendLogEntryMsg.TYPE_APPEND_SYN) {
					iStatement.commit(appendLogEntryMsg.getBaseLogList());
				} else if (appendLogEntryMsg.getAppendType() == AppendLogEntryMsg.TYPE_APPEND_ORDINARY) {
					BaseLog baseLog = appendLogEntryMsg.getBaseLogList().get(0);
					if (baseLog.getLogType() != LogData.LOG_GET) {
						iStatement.putLogData(appendLogEntryMsg.getBaseLogList());
					}
				}
			}
		}
		logLock.unlock();
		return result;
	}

	public EnumAppendLogResult appendLogEntryLocally(AppendLogEntryMsg appendLogEntryMsg) {
		// 先将之前未提交的日志提交
		logLock.lock();
		EnumAppendLogResult result = iLogSnap.appendLogEntry(appendLogEntryMsg);
		if (result == EnumAppendLogResult.LOG_APPEND_SUCCESS) {
			iStatement.putLogData(appendLogEntryMsg.getBaseLogList());
		}
		logLock.unlock();
		return result;
	}

	public EnumAppendSnapshotResult appendSnapshot(AppendSnapshotMsg appendSnapshotMsg) {
		logLock.lock();
		EnumAppendSnapshotResult appendSnapshotResult = iLogSnap.appendSnapshot(appendSnapshotMsg);
		if (iLogSnap.isNeedRebootStatemachine()) {
			iStatement.restart();
		} else {
			List<BaseSnapshot> baseSnapshotList = appendSnapshotMsg.getBaseSnapshot();
			iStatement.commitSnapshot(baseSnapshotList);
		}
		logLock.unlock();
		return appendSnapshotResult;
	}

	public boolean commit(long leaderCommit) {
		logLock.lock();
		iStatement.commit(leaderCommit);
		logLock.unlock();
		return true;
	}

	/**
	 * 
	 * 领导者提交自己的日志
	 * 
	 * @return
	 */
	public boolean commit() {
		logLock.lock();
		iStatement.commit();
		logLock.unlock();
		return true;
	}

	public String getValue(String key) {
		return iStatement.getValue(key);
	}
}
