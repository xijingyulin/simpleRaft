package com.sraft.client;

public interface IClientTransaction {

	void put(String key, String value);

	void update(String key, String value);

	void remove(String key);

	String get(String key);
}
