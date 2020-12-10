package com.sraft.core.data;

public interface IAction {
	boolean put(Statemachine statemachine, String key, String value);

	boolean update(Statemachine statemachine, String key, String value);

	boolean del(Statemachine statemachine, String key);

	String get(Statemachine statemachine, String key);
	
	void clear(Statemachine statemachine);
}
