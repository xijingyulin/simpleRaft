package com.sraft.core.message.handler;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sraft.common.flow.FlowHeader;
import com.sraft.common.flow.NoFlowLineException;
import com.sraft.core.message.HeartbeatMsg;
import com.sraft.core.message.ReplyHeartbeatMsg;
import com.sraft.core.message.ReplyRequestVoteMsg;
import com.sraft.core.message.RequestVoteMsg;
import com.sraft.core.role.RoleController;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

public abstract class AbstractHandler extends ChannelHandlerAdapter {
	private static Logger LOG = LoggerFactory.getLogger(AbstractHandler.class);

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		List<Object> params = new ArrayList<Object>();
		params.add(ctx);
		params.add(msg);

		if (msg instanceof RequestVoteMsg) {
			LOG.info("接收到【请求投票】消息:{}", msg.toString());
			try {
				FlowHeader.putProducts(RoleController.REQUEST_VOTE_WORKER, params);
			} catch (NoFlowLineException e) {
				LOG.error(e.getMessage(), e);
			}
		} else if (msg instanceof ReplyRequestVoteMsg) {
			LOG.info("接收到【回复请求投票】消息:{}", msg.toString());
			try {
				FlowHeader.putProducts(RoleController.REQUEST_VOTE_WORKER, params);
			} catch (NoFlowLineException e) {
				e.printStackTrace();
			}
		} else if (msg instanceof HeartbeatMsg) {
			//LOG.info("接收到【心跳】消息:{}", msg.toString());
			try {
				FlowHeader.putProducts(RoleController.HEARTBEAT_WORKER, params);
			} catch (NoFlowLineException e) {
				e.printStackTrace();
			}
		} else if (msg instanceof ReplyHeartbeatMsg) {
			//LOG.info("接收到【回复心跳】消息:{}", msg.toString());
			try {
				FlowHeader.putProducts(RoleController.HEARTBEAT_WORKER, params);
			} catch (NoFlowLineException e) {
				e.printStackTrace();
			}
		} else {
			LOG.info("接收到【其它不明消息！！！！！！！！！！！！！！！】消息:{}", msg.toString());
		}
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}
}
