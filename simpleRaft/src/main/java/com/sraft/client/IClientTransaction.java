package com.sraft.client;

public interface IClientTransaction {

	public static final int TYPE_ACTION_ADD = 1;
	public static final int TYPE_ACTION_UPDATE = 2;
	public static final int TYPE_ACTION_REMOVE = 3;
	public static final int TYPE_ACTION_GET = 4;

	void add(String key, String value);

	void update(String key, String value);

	void remove(String key);

	String get(String key);
}
