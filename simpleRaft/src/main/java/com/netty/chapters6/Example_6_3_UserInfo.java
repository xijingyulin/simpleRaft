package com.netty.chapters6;

import java.io.Serializable;
import java.nio.ByteBuffer;

public class Example_6_3_UserInfo implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String userName;
	private int userID;

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public int getUserID() {
		return userID;
	}

	public void setUserID(int userID) {
		this.userID = userID;
	}

	public byte[] encode(ByteBuffer buffer) {
		buffer.clear();
		byte[] value = userName.getBytes();
		buffer.putInt(value.length);
		buffer.put(value);
		buffer.putInt(userID);
		buffer.flip();
		value = null;
		byte[] result = new byte[buffer.remaining()];
		buffer.get(result);
		return result;
	}
}
