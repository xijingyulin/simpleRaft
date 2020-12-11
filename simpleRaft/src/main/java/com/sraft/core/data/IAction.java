package com.sraft.core.data;

public interface IAction {
	boolean put(Statemachine statemachine, String key, String value);

	boolean update(Statemachine statemachine, String key, String value);

	boolean remove(Statemachine statemachine, String key);

	String get(Statemachine statemachine, String key);
	
	void clear(Statemachine statemachine);
}
