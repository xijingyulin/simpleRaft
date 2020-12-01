package com.sraft.core.schedule;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.sraft.core.role.Leader;

public class ScheduleSession {
	private ScheduledExecutorService scheduler = null;
	private static ScheduleSession instance = null;

	public static ScheduleSession getInstance() {
		if (instance == null) {
			synchronized (ScheduleSession.class) {
				if (instance == null) {
					instance = new ScheduleSession();
				}
			}
		}
		return instance;
	}

	public void schedule(int clientHeartbeatInterval, Leader role) {
		scheduler = Executors.newSingleThreadScheduledExecutor();
		int checkTime = clientHeartbeatInterval * 2;
		// 考虑到领导者重新选举所花费的时间，已经客户端找到新领导者的时间，设为10倍较为合适
		int checkRange = clientHeartbeatInterval * 10;
		scheduler.schedule(new SessionThread(role, checkRange), checkTime, TimeUnit.MILLISECONDS);
	}

	public void stop() {
		scheduler.shutdown();
	}
}
