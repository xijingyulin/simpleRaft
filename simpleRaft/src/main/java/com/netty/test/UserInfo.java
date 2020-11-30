package com.netty.test;

import org.msgpack.annotation.Message;

@Message
public class UserInfo {

	private int age;
	private String name;

	public final int getAge() {
		return age;
	}

	public final void setAge(int age) {
		this.age = age;
	}

	public final String getName() {
		return name;
	}

	public final void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("age:");
		builder.append(age);
		builder.append(",name:");
		builder.append(name);
		return builder.toString();
	}

}
