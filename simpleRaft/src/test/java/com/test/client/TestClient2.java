package com.test.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sraft.client.ActionResult;
import com.sraft.client.SimpleRaftClient;
import com.sraft.client.exception.KeyNullException;
import com.sraft.client.exception.UnavailableException;
import com.sraft.client.exception.ValueNullException;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

public class TestClient2 {
	private static Logger LOG = LoggerFactory.getLogger(TestClient2.class);

	public static void main(String args[]) {
		initLog();
		TestClient2 test1 = new TestClient2();
		test1.test1();
	}

	public void test3() {
		String address = "127.0.0.1:9081,127.0.0.1:9082,127.0.0.1:9083";
		SimpleRaftClient client;
		try {
			client = new SimpleRaftClient(address);
			ActionResult result = client.update("key_1", "value_1");
			System.out.println(result);
			//client.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void test2() {
		String address = "127.0.0.1:9081,127.0.0.1:9082,127.0.0.1:9083";
		SimpleRaftClient client;
		try {
			client = new SimpleRaftClient(address);
			ActionResult result = client.remove("key_4");
			System.out.println(result);
			//client.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void test1() {
		String address = "127.0.0.1:9081,127.0.0.1:9082,127.0.0.1:9083";
		//String address = "127.0.0.1:8081,127.0.0.1:8082";
		SimpleRaftClient client = null;
		try {
			client = new SimpleRaftClient(address);
		} catch (Exception e) {
			e.printStackTrace();
		}
		long begin = System.currentTimeMillis();
		for (int i = 0; i < 20000; i++) {

			try {
				client.get("key_999");
			} catch (UnavailableException | KeyNullException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//			try {
			//				System.out.println(client.put("key_" + i, "value_" + i));
			//			} catch (UnavailableException | KeyNullException | ValueNullException e) {
			//				e.printStackTrace();
			//				LOG.error(e.getMessage(), e);
			//			}
		}
		long end = System.currentTimeMillis();
		long time = (end - begin) / 1000;
		System.out.println("耗时:" + time);
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
