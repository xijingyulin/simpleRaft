package com.sraft.core.data;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public final class Statemachine {

	private ConcurrentHashMap<String, String> statemachine = new ConcurrentHashMap<String, String>();

	public ConcurrentHashMap<String, String> getStatemachine() {
		return statemachine;
	}

	public void setStatemachine(ConcurrentHashMap<String, String> statemachine) {
		this.statemachine = statemachine;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (Entry<String, String> entry : statemachine.entrySet()) {
			builder.append("key:").append(entry.getKey());
			builder.append(",").append("value:").append(entry.getValue()).append("\n");
		}
		return builder.toString();
	}

}
