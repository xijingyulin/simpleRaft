package com.netty.chapters6;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class Example_6_1_TestUserInfo {

	public static void main(String args[]) throws IOException {
		Example_6_1_UserInfo info = new Example_6_1_UserInfo();
		info.setUserID(100);
		info.setUserName("Welcome to Netty");
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream os = new ObjectOutputStream(bos);
		os.writeObject(info);
		os.flush();
		os.close();
		bos.close();
		byte[] b = bos.toByteArray();
		System.out.println("The jdk serializable length is :" + b.length);
		System.out.println("The byte array serializable is :" + info.encode().length);
	}
}
