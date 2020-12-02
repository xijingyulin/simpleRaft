package com.sraft.core.schedule.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sraft.core.role.Leader;

public class HeartbeatThread implements Runnable {
	private static Logger LOG = LoggerFactory.getLogger(HeartbeatThread.class);
	private Leader leader;
	private int checkRange;

	public HeartbeatThread(Leader leader, int checkRange) {
		this.leader = leader;
		this.checkRange = checkRange;
	}

	/*
	 * 1.更新节点状态，是否过半存活
	 * 
	 * 2.发送心跳消息
	 * 
	 * 3.设置下一次心跳超时
	 */
	@Override
	public void run() {
		leader.checkNodeStatus(checkRange);
		leader.stopHeartbeat();
		//领导者还存活
		if (!leader.isChangedRole()) {
			leader.sendHeartbeat();
			leader.startHeartbeat();
		}
	}
}
