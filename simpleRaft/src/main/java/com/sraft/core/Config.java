package com.sraft.core;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import com.sraft.common.PropertiesHelper;
import com.sraft.common.StringHelper;
import com.sraft.core.net.ServerAddress;

public class Config {
	public static final String TERM_FILE = "termInfo";
	public static final String VOTED_FOR_FILE = "votedFor";
	public static final String CONF_PREFIX = "sraft";
	public static final String CONF_SUFFIX = ".cfg";
	public static String DEFAULT_CONF_PATH = "conf" + File.separator;
	public static String CONF_FILE_PATH;
	private int timeout = 2000;
	private int tickTime = 200;;
	private int minTimeout;
	private int maxTimeout;

	private int clientPort;
	private String logDataDir;
	/**
	 * 每隔snapshotInterval小时，压缩一次日志，满足触发条件才能压缩
	 */
	protected int snapshotInterval = 10;
	/**
	 * 当日志行数达到compressTrigger
	 */
	protected int compressTrigger = 10000;
	/**
	 * 真正压缩行数
	 */
	protected int compressCount = 8000;
	/**
	 * 本机ID
	 */
	private int selfId;
	/**
	 * 所有节点地址列表 key:节点ID value:节点地址
	 */
	private Map<Integer, ServerAddress> serverAddressMap = new HashMap<Integer, ServerAddress>();

	/**
	 * 除了自己外，其它节点地址列表
	 */
	private List<ServerAddress> connAddressList = new ArrayList<ServerAddress>();

	public static boolean isStandby = true;

	public Config() {
	}

	public Config(String configPath) {
		DEFAULT_CONF_PATH = configPath;
	}

	public void readConf() throws Throwable {
		try {
			File confDir = new File(DEFAULT_CONF_PATH);
			File[] files = confDir.listFiles();
			for (File file : files) {
				String fileName = file.getName();
				if (file.isFile()) {
					if (fileName.startsWith(CONF_PREFIX) && fileName.endsWith(CONF_SUFFIX)) {
						CONF_FILE_PATH = file.getAbsolutePath();
						break;
					}
				}
			}

			if (StringHelper.checkIsNotNull(CONF_FILE_PATH)) {
				Properties prop = PropertiesHelper.getProperties(CONF_FILE_PATH);
				Set<Object> keySet = prop.keySet();
				for (Object object : keySet) {
					String key = (String) object;
					String value = prop.getProperty(key).trim();
					if (key.equalsIgnoreCase("timeout")) {
						this.timeout = Integer.parseInt(value);
						this.minTimeout = timeout / 2;
						this.maxTimeout = timeout * 2;
					}
					if (key.equalsIgnoreCase("tickTime")) {
						this.tickTime = Integer.parseInt(value);
					}
					if (key.equalsIgnoreCase("clientPort")) {
						this.clientPort = Integer.parseInt(value);
					}
					if (key.equalsIgnoreCase("logDataDir")) {
						if (value.endsWith("/")) {
							value = value.substring(0, value.lastIndexOf("/"));
						} else if (value.endsWith("\\")) {
							value = value.substring(0, value.lastIndexOf("\\"));
						}
						this.logDataDir = value;
					}
					if (key.equalsIgnoreCase("snapshotInterval")) {
						this.snapshotInterval = Integer.valueOf(value);
					}
					if (key.equalsIgnoreCase("compressTrigger")) {
						this.compressTrigger = Integer.valueOf(value);
					}
					if (key.equalsIgnoreCase("compressCount")) {
						this.compressCount = Integer.valueOf(value);
					}
					if (key.equalsIgnoreCase("id")) {
						if (StringHelper.checkIsNotNull(value)) {
							this.selfId = Integer.parseInt(value);
						}
					}
					if (key.startsWith("server")) {
						int nodeId = Integer.parseInt(key.split("[.]")[1].trim());
						String[] valueArr = value.split(":");
						String host = valueArr[0].trim();
						String port = valueArr[1].trim();
						ServerAddress serverAddress = new ServerAddress(nodeId, host, Integer.parseInt(port));
						isStandby = false;
						this.serverAddressMap.put(nodeId, serverAddress);
					}
				}
				for (Entry<Integer, ServerAddress> entry : serverAddressMap.entrySet()) {
					int serverId = entry.getKey();
					if (serverId != selfId) {
						connAddressList.add(entry.getValue());
					}
				}
			} else {
				throw new Exception("没有找到配置文件,配置目录:" + CONF_FILE_PATH);
			}
		} catch (Throwable e) {
			e.printStackTrace();
			throw new Throwable("配置出错:" + e.getMessage());
		}
	}


	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public int getClientPort() {
		return clientPort;
	}

	public void setClientPort(int clientPort) {
		this.clientPort = clientPort;
	}

	public String getLogDataDir() {
		return logDataDir;
	}

	public void setLogDataDir(String logDataDir) {
		this.logDataDir = logDataDir;
	}

	public int getSelfId() {
		return selfId;
	}

	public void setSelfId(int selfId) {
		this.selfId = selfId;
	}

	public Map<Integer, ServerAddress> getServerAddressMap() {
		return serverAddressMap;
	}

	public void setServerAddressMap(Map<Integer, ServerAddress> serverAddressMap) {
		this.serverAddressMap = serverAddressMap;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("timeout:");
		builder.append(timeout);
		builder.append(",clientPort:");
		builder.append(clientPort);
		builder.append(",logDataDir:");
		builder.append(logDataDir);
		builder.append(",snapshotInterval:");
		builder.append(snapshotInterval);
		builder.append(",compressTrigger:");
		builder.append(compressTrigger);
		builder.append(",compressCount:");
		builder.append(compressCount);
		builder.append(",selfId:");
		builder.append(selfId);
		builder.append(",serverAddressMap:");
		builder.append(serverAddressMap);
		return builder.toString();
	}

	public int getSnapshotInterval() {
		return snapshotInterval;
	}

	public void setSnapshotInterval(int snapshotInterval) {
		this.snapshotInterval = snapshotInterval;
	}

	public int getCompressCount() {
		return compressCount;
	}

	public void setCompressCount(int compressCount) {
		this.compressCount = compressCount;
	}

	public int getCompressTrigger() {
		return compressTrigger;
	}

	public void setCompressTrigger(int compressTrigger) {
		this.compressTrigger = compressTrigger;
	}

	public int getTickTime() {
		return tickTime;
	}

	public void setTickTime(int tickTime) {
		this.tickTime = tickTime;
	}

	public int getMinTimeout() {
		return minTimeout;
	}

	public void setMinTimeout(int minTimeout) {
		this.minTimeout = minTimeout;
	}

	public int getMaxTimeout() {
		return maxTimeout;
	}

	public void setMaxTimeout(int maxTimeout) {
		this.maxTimeout = maxTimeout;
	}

	public static boolean isStandby() {
		return isStandby;
	}

	public static void setStandby(boolean isStandby) {
		Config.isStandby = isStandby;
	}

	public List<ServerAddress> getConnAddressList() {
		return connAddressList;
	}

	public void setConnAddressList(List<ServerAddress> connAddressList) {
		this.connAddressList = connAddressList;
	}

}
