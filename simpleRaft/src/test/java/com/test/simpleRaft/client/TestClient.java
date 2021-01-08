package com.test.simpleRaft.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sraft.client.SimpleRaftClient;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

public class TestClient {
	private static Logger LOG = LoggerFactory.getLogger(TestClient.class);

	public static void main(String args[]) {
		initLog();
		TestClient testClient = new TestClient();
		testClient.test();
	}

	public void test() {
		String address = "127.0.0.1:9081,127.0.0.1:9082,127.0.0.1:9083";
		SimpleRaftClient client;
		try {
			client = new SimpleRaftClient(address);
			System.out.println(client.put("test", "testSimpleRaft").getStatus());
		} catch (Exception e) {
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
