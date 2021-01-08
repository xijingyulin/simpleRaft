package com.test.simpleRaft.role;

import org.slf4j.LoggerFactory;

import com.sraft.core.Config;
import com.sraft.core.role.RoleController;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

public class TestRole1 {
	public static void main(String args[]) {
		initLog();
		TestRole1 testRole = new TestRole1();
		testRole.test();
	}

	public void test() {
		String confPath = "conf/1";
		Config config = new Config(confPath);
		try {
			config.readConf();
			RoleController roleController = new RoleController(config);
			roleController.play();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public static void initLog() {
		LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
		JoranConfigurator configurator = new JoranConfigurator();
		configurator.setContext(lc);
		lc.reset();
		try {
			configurator.doConfigure("conf/logback.xml");
		} catch (JoranException e) {
			e.printStackTrace();
		}
	}
}
