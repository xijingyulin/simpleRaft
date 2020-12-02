package com.sraft.core.role;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sraft.common.DateHelper;
import com.sraft.common.IdGenerateHelper;
import com.sraft.core.message.Msg;
import com.sraft.core.message.RequestVoteMsg;
import com.sraft.core.net.ConnManager;
import com.sraft.core.net.ServerAddress;
import com.sraft.core.role.sender.SendRequestVoteThread;
import com.sraft.core.schedule.ScheduleElectionTimeout;
import com.sraft.enums.EnumRole;

public class Candidate extends AbstractRoles {
	private static Logger LOG = LoggerFactory.getLogger(Candidate.class);
	/**
	 * 投票过半
	 */
	private boolean isVotedOverHalf = false;
	/**
	 * 候选者首先投票给自己，所以初始化为1
	 */
	private AtomicInteger countVote = new AtomicInteger(1);
	private List<ServerAddress> connAddressList;

	public Candidate(RoleController roleController) throws IOException {
		super(EnumRole.CANDIDATE, roleController);
		connAddressList = roleController.getConfig().getConnAddressList();
	}

	@Override
	public void run() {
		try {
			updateVotedFor(roleController.getConfig().getSelfId());
			LOG.info("激活消息接收通道");
			enableWorker(this);
			LOG.info("连接其它服务器");
			connOtherServer();
			LOG.info("设置选举超时");
			startElectionTimeout();
		} catch (Throwable e) {
			e.printStackTrace();
			LOG.error(e.getMessage(), e);
			LOG.error("启动候选者异常,退出程序");
		}
	}

	/**
	 * 连接所有其它节点
	 */
	public void connOtherServer() {
		for (ServerAddress address : connAddressList) {
			try {
				ConnManager.getInstance().connect(address);
			} catch (Exception e) {
				e.printStackTrace();
				LOG.error(e.getMessage(), e);
				LOG.error("连接服务器失败:{}", address.toString());
			}
		}
	}

	public void initVote() {
		RequestVoteMsg requestVoteMsg = getRequestVoteMsg();
		for (ServerAddress serverAddress : connAddressList) {
			new Thread(new SendRequestVoteThread(requestVoteMsg, serverAddress)).start();
		}
	}

	public RequestVoteMsg getRequestVoteMsg() {
		RequestVoteMsg requestVoteMsg = new RequestVoteMsg();
		requestVoteMsg.setMsgType(Msg.TYPE_REQUEST_VOTE);
		requestVoteMsg.setMsgId(IdGenerateHelper.getMsgId());
		requestVoteMsg.setNodeId(roleController.getConfig().getSelfId());
		requestVoteMsg.setTerm(currentTerm.get());
		requestVoteMsg.setSendTime(DateHelper.formatDate2Long(new Date(), DateHelper.YYYYMMDDHHMMSSsss));
		requestVoteMsg.setLastLogIndex(roleController.getiLogEntry().getLastLogIndex());
		requestVoteMsg.setLastLogTerm(roleController.getiLogEntry().getLastLogTerm());
		return requestVoteMsg;
	}

	/**
	 * 设置选举超时定时器
	 */
	public void startElectionTimeout() {
		ScheduleElectionTimeout.getInstance().schedule(roleController.getConfig().getMinTimeout(),
				roleController.getConfig().getMaxTimeout(), this);
	}

	/**
	 * 停止选举超时
	 */
	public void closeElectionTimeout() {
		ScheduleElectionTimeout.getInstance().stop();
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
		LOG.info("转换角色,停止选举超时...");
		closeElectionTimeout();
		LOG.info("转换角色,禁用消息接收通道...");
		disableWorker();
		if (newRole == EnumRole.FOLLOWER) {
			LOG.info("转换角色,关闭连接其它服务器...");
			ConnManager.getInstance().closeAll();
		}
		roleController.changeRole(newRole);
	}

	public void resetStatus() {
		isChangedRole = false;
		countVote.set(1);
		isVotedOverHalf = false;
	}

	public boolean isVotedOverHalf() {
		return isVotedOverHalf;
	}

	public void addVoted() {
		int count = countVote.incrementAndGet();
		int half = roleController.getConfig().getServerAddressMap().size() / 2;
		if (count > half) {
			isVotedOverHalf = true;
		}
	}

	public void addTerm() {
		synchronized (currentTerm) {
			roleController.getTermAndVotedForService().writeTerm(currentTerm.incrementAndGet());
			LOG.info("新任期:{}", currentTerm);
		}
	}

}
