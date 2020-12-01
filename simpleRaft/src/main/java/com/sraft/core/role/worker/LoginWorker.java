package com.sraft.core.role.worker;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sraft.client.SimpleRaftClient;
import com.sraft.client.message.LoginMsg;
import com.sraft.client.message.ReplyLoginMsg;

import io.netty.channel.ChannelHandlerContext;

public class LoginWorker extends Workder {
	private static Logger LOG = LoggerFactory.getLogger(LoginWorker.class);

	private SimpleRaftClient client;

	public LoginWorker() {

	}

	public LoginWorker(SimpleRaftClient client) {
		this.client = client;
		setEnable(true);
	}

	@Override
	public void doWork(Object object) {
		List<Object> params = (List<Object>) object;
		ChannelHandlerContext ctx = (ChannelHandlerContext) params.get(0);
		Object msg = params.get(1);
		if (msg instanceof LoginMsg) {
			LoginMsg loginMsg = (LoginMsg) params.get(1);
			//	dealLoginMsg(ctx, loginMsg);
		} else if (msg instanceof ReplyLoginMsg) {
			ReplyLoginMsg replyLoginMsg = (ReplyLoginMsg) params.get(1);
			//dealReplyLoginMsg(replyLoginMsg);
		}
	}

}
