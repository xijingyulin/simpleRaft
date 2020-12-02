package com.sraft.core.schedule;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.sraft.client.SimpleRaftClient;
import com.sraft.core.schedule.impl.ClientHeartbeatThread;

public class ScheduleClientHeartbeat {
	private ScheduledExecutorService scheduler = null;
	private static ScheduleClientHeartbeat instance = null;

	public static ScheduleClientHeartbeat getInstance() {
		if (instance == null) {
			synchronized (ScheduleClientHeartbeat.class) {
				if (instance == null) {
					instance = new ScheduleClientHeartbeat();
				}
			}
		}
		return instance;
	}

	public void schedule(int tickTime, SimpleRaftClient client) {
		scheduler = Executors.newSingleThreadScheduledExecutor();
		int checkRange = tickTime * 3;
		scheduler.schedule(new ClientHeartbeatThread(client, checkRange), tickTime, TimeUnit.MILLISECONDS);
	}

	public void stop() {
		scheduler.shutdown();
	}
}
