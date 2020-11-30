package com.sraft.client;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sraft.client.message.LoginMsg;
import com.sraft.common.DateHelper;
import com.sraft.common.IdGenerateHelper;
import com.sraft.core.message.Msg;
import com.sraft.core.net.ConnManager;

public class SraftClient {
	private static Logger LOG = LoggerFactory.getLogger(SraftClient.class);

	public SraftClient(String address) {
		try {
			// 解析地址
			ConnAddrManager.getInstance().explainAddr(address);
			login();
		} catch (Exception e) {
			e.printStackTrace();
			LOG.error(e.getMessage());
			System.exit(0);
		}
	}

	private long sessionId = -1;

	private void login() {
		//ConnManager.getInstance().connect(host, port)

	}

	private LoginMsg getLoginMsg() {
		LoginMsg msg = new LoginMsg();
		msg.setMsgId(IdGenerateHelper.getMsgId());
		msg.setMsgType(Msg.TYPE_CLIENT_LOGIN);
		msg.setSendTime(DateHelper.formatDate2Long(new Date(), DateHelper.YYYYMMDDHHMMSSsss));
		msg.setSessionId(sessionId);

		return msg;
	}
}
