package com.test.client;

import com.sraft.core.net.ConnManager;

import io.netty.channel.Channel;

public class TestClientMain {

	public static void main(String args[]) {

	}

	public void test() {
		//String sraft = "127.0.0.1:9081,127.0.0.1:9082,127.0.0.1:9083";
		String sraft = "127.0.0.1:9081";
		String[] addressArr = sraft.split(",");
		Channel client = ConnManager.getInstance().connect(addressArr[0], Integer.parseInt(addressArr[1]));
		
	}
}
