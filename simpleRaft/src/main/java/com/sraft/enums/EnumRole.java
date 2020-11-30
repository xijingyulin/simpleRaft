package com.sraft.enums;

/**
 * 服务器角色
 * 
 * @author 伍尚康-2020年11月23日
 *
 */
public enum EnumRole {

	FOLLOWER(1), CANDIDATE(2), LEADER(3), ERROR(-1);

	private final int initValue;

	EnumRole(int initValue) {
		this.initValue = initValue;
	}

	public int getValue() {
		return this.initValue;
	}
}
