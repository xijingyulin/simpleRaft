package com.sraft.core.schedule.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sraft.client.ClientConnManager;

public class ClientHeartbeatThread implements Runnable {
	private static Logger LOG = LoggerFactory.getLogger(ClientHeartbeatThread.class);
	private ClientConnManager client;
	private int checkRange;

	public ClientHeartbeatThread(ClientConnManager client, int checkRange) {
		this.client = client;
		this.checkRange = checkRange;
	}

	/*
	 * 1.检查心跳是否超时
	 * 
	 * 2.超时重新登录
	 * 
	 */
	@Override
	public void run() {
		try {
			client.checkClientHeartbeat(checkRange);
			if (client.isLogin()) {
				client.sendClientHeartbeat();
			} else {
				LOG.error("登录失败,重新登录");
				client.reConnected();
			}
			client.stopClientHeartbeat();
			client.startClientHeartbeat();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
