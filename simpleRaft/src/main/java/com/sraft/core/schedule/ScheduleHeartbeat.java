package com.sraft.core.schedule;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.sraft.core.role.Leader;

public class ScheduleHeartbeat {
	private ScheduledExecutorService scheduler = null;
	private static ScheduleHeartbeat instance = null;

	public static ScheduleHeartbeat getInstance() {
		if (instance == null) {
			synchronized (ScheduleHeartbeat.class) {
				if (instance == null) {
					instance = new ScheduleHeartbeat();
				}
			}
		}
		return instance;
	}

	public void schedule(int tickTime, Leader role) {
		scheduler = Executors.newSingleThreadScheduledExecutor();
		int checkRange = tickTime * 3;
		scheduler.schedule(new HeartbeatThread(role, checkRange), tickTime, TimeUnit.MILLISECONDS);
	}

	public void stop() {
		scheduler.shutdown();
	}
}
