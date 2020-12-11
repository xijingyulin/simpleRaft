package com.sraft.core.data;

public class ActionImpl implements IAction {

	@Override
	public boolean put(Statemachine statemachine, String key, String value) {
		statemachine.getStatemachine().put(key, value);
		return true;
	}

	@Override
	public boolean update(Statemachine statemachine, String key, String value) {
		statemachine.getStatemachine().put(key, value);
		return false;
	}

	@Override
	public boolean remove(Statemachine statemachine, String key) {
		statemachine.getStatemachine().remove(key);
		return false;
	}

	@Override
	public String get(Statemachine statemachine, String key) {
		return statemachine.getStatemachine().get(key);
	}

	@Override
	public void clear(Statemachine statemachine) {
		statemachine.getStatemachine().clear();
	}

}
