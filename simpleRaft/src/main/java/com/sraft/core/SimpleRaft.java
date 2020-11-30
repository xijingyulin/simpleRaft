package com.sraft.core;

import com.sraft.Config;
import com.sraft.core.role.RoleController;

public class SimpleRaft {

	public static void main(String args[]) {
		Config config = new Config();
		try {
			config.readConf();
			RoleController roleController = new RoleController(config);
			roleController.play();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}
