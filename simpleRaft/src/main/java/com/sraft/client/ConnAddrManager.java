package com.sraft.client;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sraft.core.net.ServerAddress;

public class ConnAddrManager {
	private static Logger LOG = LoggerFactory.getLogger(ConnAddrManager.class);

	private static volatile ConnAddrManager instance;
	private List<ServerAddress> addrList = new ArrayList<ServerAddress>();

	public static ConnAddrManager getInstance() {
		if (instance == null) {
			synchronized (ConnAddrManager.class) {
				if (instance == null) {
					instance = new ConnAddrManager();
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
				int port = Integer.parseInt(hostAndPort[1].trim());
				ServerAddress serverAddress = new ServerAddress(i, host, port);
				addrList.add(serverAddress);
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
		nowIndex++;
		if (nowIndex >= addrList.size()) {
			nowIndex = 0;
		}
		return addrList.get(nowIndex);
	}

}
