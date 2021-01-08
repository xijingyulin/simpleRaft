package com.sraft.test.config;

import com.sraft.core.Config;

public class TestConfig {

	public static void main(String args[]) {
		Config config = new Config();
		try {
			config.readConf();
			System.out.println(config.toString());
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}
