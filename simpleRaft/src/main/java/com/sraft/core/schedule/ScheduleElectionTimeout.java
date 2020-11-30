package com.sraft.core.schedule;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.sraft.core.role.AbstractRoles;

public class ScheduleElectionTimeout {
	private ScheduledExecutorService scheduler = null;
	private static ScheduleElectionTimeout instance = null;

	public static ScheduleElectionTimeout getInstance() {
		if (instance == null) {
			synchronized (ScheduleElectionTimeout.class) {
				if (instance == null) {
					instance = new ScheduleElectionTimeout();
				}
			}
		}
		return instance;
	}

	public void schedule(int minTimeout, int maxTimeout, AbstractRoles role) {
		scheduler = Executors.newSingleThreadScheduledExecutor();
		int nextElectionTime = minTimeout + new Random().nextInt(maxTimeout - minTimeout);
		scheduler.schedule(new ElectionTimeoutThread(role, nextElectionTime), nextElectionTime, TimeUnit.MILLISECONDS);
	}

	public void stop() {
		scheduler.shutdown();
	}
}
