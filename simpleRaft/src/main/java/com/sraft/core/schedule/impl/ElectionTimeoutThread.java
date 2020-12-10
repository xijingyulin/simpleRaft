package com.sraft.core.schedule.impl;

import java.text.ParseException;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sraft.common.DateHelper;
import com.sraft.core.message.HeartbeatMsg;
import com.sraft.core.message.ServerMsg;
import com.sraft.core.role.AbstractRoles;
import com.sraft.core.role.Candidate;
import com.sraft.core.role.Follower;
import com.sraft.enums.EnumRole;

public class ElectionTimeoutThread implements Runnable {
	private static Logger LOG = LoggerFactory.getLogger(ElectionTimeoutThread.class);
	private AbstractRoles role;
	private int interval;

	public ElectionTimeoutThread(AbstractRoles role, int interval) {
		this.role = role;
		this.interval = interval;
	}

	@Override
	public void run() {
		boolean isDead = isHeartbeat();
		if (role instanceof Follower) {
			Follower follower = (Follower) role;
			if (isDead) {
				LOG.info("本次选举超时:{}ms", interval);
				follower.nextRole(EnumRole.CANDIDATE);
			} else {
				follower.closeElectionTimeout();
				follower.startElectionTimeout();
			}
		} else {
			Candidate candidate = (Candidate) role;
			if (candidate.isChangedRole()) {
				
			} else if (candidate.isVotedOverHalf()) {
				candidate.nextRole(EnumRole.LEADER);
			} else if (!isDead) {
				LOG.info("接收到心跳，转换成跟随者");
				candidate.nextRole(EnumRole.FOLLOWER);
			} else {
				LOG.info("本次选举超时:{}ms", interval);
				candidate.resetStatus();
				LOG.info("自增任期");
				candidate.addTerm();
				LOG.info("发起投票");
				candidate.initVote();
				LOG.info("设置选举超时");
				candidate.closeElectionTimeout();
				candidate.startElectionTimeout();
			}
		}
	}

	/**
	 * 在超时内是否接收到心跳
	 * 
	 * @return
	 */
	public boolean isHeartbeat() {
		boolean isDead = false;
		long minHeartTime;
		try {
			minHeartTime = DateHelper.addMillSecond(new Date(), -interval);
		} catch (ParseException e) {
			e.printStackTrace();
			return true;
		}
		ServerMsg heartbeatMsg = role.getHeartbeatMsg();
		if (heartbeatMsg == null) {
			isDead = true;
		} else {
			long receiveTime = heartbeatMsg.getReceviceTime();
			if (receiveTime >= minHeartTime) {
				isDead = false;
			} else {
				isDead = true;
			}
		}
		return isDead;
	}
}
