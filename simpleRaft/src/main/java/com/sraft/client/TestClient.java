package com.sraft.client;

public class TestClient {

	public static void main(String args[]) {
		String address = "127.0.0.1:8081,127.0.0.1:8082";
		SimpleRaftClient client;
		try {
			client = new SimpleRaftClient(address);
			client.put("abc", "def");

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
