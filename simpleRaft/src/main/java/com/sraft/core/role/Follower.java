package com.sraft.core.role;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sraft.core.schedule.ScheduleElectionTimeout;
import com.sraft.enums.EnumRole;

public class Follower extends AbstractRoles {
	private static Logger LOG = LoggerFactory.getLogger(Follower.class);
	/**
	 * 跟随者需要知道领导者ID，主要给客户端重定向
	 */
	private int leaderId = -1;
	private int leaderPort;

	public Follower(RoleController roleController) throws IOException {
		super(EnumRole.FOLLOWER, roleController);
	}

	@Override
	public void run() {
		try {
			LOG.info("激活消息接收通道");
			enableWorker(this);
			LOG.info("设置选举超时");
			startElectionTimeout();
		} catch (Throwable e) {
			e.printStackTrace();
			LOG.error(e.getMessage(), e);
			LOG.error("启动跟随者异常，退出程序");
			System.exit(0);
		}
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
		roleController.changeRole(newRole);
	}

	public int getLeaderId() {
		return leaderId;
	}

	public void setLeaderId(int receiveLeader) {
		if (this.leaderId != receiveLeader) {
			this.leaderId = receiveLeader;
		}
	}

	public int getLeaderPort() {
		return leaderPort;
	}

	public void setLeaderPort(int leaderPort) {
		if (this.leaderPort != leaderPort) {
			this.leaderPort = leaderPort;
		}
	}

}
