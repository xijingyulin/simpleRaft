package com.test.role;

import org.slf4j.LoggerFactory;

import com.sraft.Config;
import com.sraft.core.role.RoleController;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

public class TestRole {
	public static void main(String args[]) {
		initLog();
		TestRole testRole = new TestRole();
		testRole.test();
	}

	public void test() {
		Config config = new Config();
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
