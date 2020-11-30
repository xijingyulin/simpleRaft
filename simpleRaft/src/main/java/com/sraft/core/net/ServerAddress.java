package com.sraft.core.net;

public class ServerAddress {
	private int nodeId;
	private String host;
	private int port;

	public ServerAddress() {
	}

	public ServerAddress(int nodeId, String host, int port) {
		super();
		this.nodeId = nodeId;
		this.host = host;
		this.port = port;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getNodeId() {
		return nodeId;
	}

	public void setNodeId(int nodeId) {
		this.nodeId = nodeId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("nodeId:");
		builder.append(nodeId);
		builder.append(",host:");
		builder.append(host);
		builder.append(",port:");
		builder.append(port);
		return builder.toString();
	}
}
