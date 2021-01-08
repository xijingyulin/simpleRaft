package com.sraft.test.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sraft.client.ActionResult;
import com.sraft.client.SimpleRaftClient;
import com.sraft.core.message.Msg;

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
			for (int i = 0; i < 10; i++) {
				String key = "test:" + i;
				String value = "value:" + i;
				LOG.info("新增数据,key={},value={}", key, value);
				ActionResult result = client.put(key, value);
				LOG.info("执行结果:{}", result.getStatus() == Msg.RETURN_STATUS_OK);
			}
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
