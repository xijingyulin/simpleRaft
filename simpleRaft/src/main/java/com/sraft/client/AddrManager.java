package com.sraft.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sraft.core.net.ServerAddress;

import io.netty.channel.Channel;

public class AddrManager {
	private static Logger LOG = LoggerFactory.getLogger(AddrManager.class);

	private static volatile AddrManager instance;
	private List<ServerAddress> addrList = new ArrayList<ServerAddress>();
	/**
	 * 记住已有的地址
	 */
	private Set<String> addrSet = new HashSet<String>();

	public static AddrManager getInstance() {
		if (instance == null) {
			synchronized (AddrManager.class) {
				if (instance == null) {
					instance = new AddrManager();
				}
			}
		}
		return instance;
	}

	public void explainAddr(String address) throws Exception {
		try {
			String[] addressArr = address.split(",");

			for (int i = 0; i < addressArr.length; i++) {
				String addr = addressArr[i];
				String[] hostAndPort = addr.split(":");
				String host = hostAndPort[0].trim();
				String portStr = hostAndPort[1].trim();
				int port = Integer.parseInt(portStr);
				ServerAddress serverAddress = new ServerAddress(i, host, port);
				addrList.add(serverAddress);
				addrSet.add(host + ":" + portStr);
			}
			if (addrList.size() == 0) {
				throw new Exception("地址为空");
			}
		} catch (Exception e) {
			e.printStackTrace();
			LOG.error(e.getMessage(), e);
			throw new Exception("解析地址出错");
		}
	}

	private int nowIndex = -1;

	public ServerAddress nextAddr() {
		synchronized (addrList) {
			nowIndex++;
			if (nowIndex >= addrList.size()) {
				nowIndex = 0;
			}
			return addrList.get(nowIndex);
		}
	}

	/**
	 * 成功增加地址后，重定位地址索引到领导者地址
	 * 
	 * @param host
	 * @param port
	 */
	public synchronized void addAddr(String host, int port) {
		synchronized (addrList) {
			String key = host + ":" + port;
			if (!addrSet.contains(key)) {
				ServerAddress serverAddress = new ServerAddress(addrList.size(), host, port);
				addrList.add(serverAddress);
				nowIndex = addrList.size() - 2;
				addrSet.add(key);
			}
		}
	}

	public ServerAddress getLeaderConn() {
		synchronized (addrList) {
			return addrList.get(nowIndex);
		}
	}
}
