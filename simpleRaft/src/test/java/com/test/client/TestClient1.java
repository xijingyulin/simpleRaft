package com.test.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sraft.client.ActionResult;
import com.sraft.client.SimpleRaftClient;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

public class TestClient1 {
	private static Logger LOG = LoggerFactory.getLogger(TestClient1.class);

	public static void main(String args[]) {
		initLog();
		TestClient1 test1 = new TestClient1();
		test1.test1();
	}

	public void testThread() {
		for (int i = 0; i < 100; i++) {
			new Thread(new TestThread(i)).start();
		}
	}

	class TestThread implements Runnable {
		private int no;

		public TestThread(int no) {
			this.no = no;
		}

		@Override
		public void run() {
			String address = "127.0.0.1:9081,127.0.0.1:9082,127.0.0.1:9083";
			//String address = "127.0.0.1:8081,127.0.0.1:8082";
			for (int i = 0; i < 1000; i++) {

			}
		}

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
		SimpleRaftClient client;
		try {
			client = new SimpleRaftClient(address);
			//for (int i = 1; i < 100; i++) {
			//client.put("key_" + i, "value_" + i);
			long begin = System.currentTimeMillis();
			System.out.println(client.remove("key_" + 1));
			//}
			long end = System.currentTimeMillis();
			long time = (end - begin) / 1000;
			System.out.println("耗时:" + time);
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
