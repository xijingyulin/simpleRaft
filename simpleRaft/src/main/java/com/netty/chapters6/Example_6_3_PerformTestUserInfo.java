package com.netty.chapters6;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;

public class Example_6_3_PerformTestUserInfo {

	public static void main(String args[]) throws IOException {
		Example_6_3_UserInfo info = new Example_6_3_UserInfo();
		info.setUserID(100);
		info.setUserName("Welcome to Netty");
		ByteArrayOutputStream bos = null;
		ObjectOutputStream os = null;
		long startTime = System.currentTimeMillis();
		for (int i = 0; i < 1000000; i++) {
			bos = new ByteArrayOutputStream();
			os = new ObjectOutputStream(bos);
			os.writeObject(info);
			os.flush();
			os.close();
			byte[] b = bos.toByteArray();
			bos.close();
		}
		long endTime = System.currentTimeMillis();
		System.out.println("The jdk serializable cost time is :" + (endTime - startTime) + "ms");

		ByteBuffer buffer = ByteBuffer.allocate(1024);
		startTime = System.currentTimeMillis();
		for (int i = 0; i < 1000000; i++) {
			byte[] b = info.encode(buffer);
		}
		System.out.println("The byte serializable cost time is :" + (endTime - startTime) + "ms");

	}
}
