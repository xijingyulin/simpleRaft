package com.sraft.core.message.handler;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sraft.common.flow.FlowHeader;
import com.sraft.common.flow.NoFlowLineException;
import com.sraft.core.message.AppendLogEntryMsg;
import com.sraft.core.message.AppendSnapshotMsg;
import com.sraft.core.message.ClientActionMsg;
import com.sraft.core.message.ClientHeartbeatMsg;
import com.sraft.core.message.HeartbeatMsg;
import com.sraft.core.message.LoginMsg;
import com.sraft.core.message.ReplyAppendLogEntryMsg;
import com.sraft.core.message.ReplyAppendSnapshotMsg;
import com.sraft.core.message.ReplyClientActionMsg;
import com.sraft.core.message.ReplyClientHeartbeatMsg;
import com.sraft.core.message.ReplyHeartbeatMsg;
import com.sraft.core.message.ReplyLoginMsg;
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
		try {
			if (msg instanceof RequestVoteMsg) {
				LOG.info("接收到【请求投票】消息:{}", msg.toString());
				FlowHeader.putProducts(RoleController.REQUEST_VOTE_WORKER, params);
			} else if (msg instanceof ReplyRequestVoteMsg) {
				LOG.info("接收到【回复请求投票】消息:{}", msg.toString());
				FlowHeader.putProducts(RoleController.REQUEST_VOTE_WORKER, params);
			} else if (msg instanceof HeartbeatMsg) {
				//LOG.info("接收到【心跳】消息:{}", msg.toString());
				FlowHeader.putProducts(RoleController.HEARTBEAT_WORKER, params);
			} else if (msg instanceof ReplyHeartbeatMsg) {
				//LOG.info("接收到【回复心跳】消息:{}", msg.toString());
				FlowHeader.putProducts(RoleController.HEARTBEAT_WORKER, params);
			} else if (msg instanceof LoginMsg) {
				LOG.info("接收到【登录】消息:{}", msg.toString());
				FlowHeader.putProducts(RoleController.LOGIN_WORKER, params);
			} else if (msg instanceof ReplyLoginMsg) {
				LOG.info("接收到【回复登录】消息:{}", msg.toString());
				FlowHeader.putProducts(RoleController.LOGIN_WORKER, params);
			} else if (msg instanceof ClientHeartbeatMsg) {
				//LOG.info("接收到【客户端心跳】消息:{}", msg.toString());
				FlowHeader.putProducts(RoleController.CLIENT_HEARTBEAT_WORKER, params);
			} else if (msg instanceof ReplyClientHeartbeatMsg) {
				//LOG.info("接收到【回复客户端心跳】消息:{}", msg.toString());
				FlowHeader.putProducts(RoleController.CLIENT_HEARTBEAT_WORKER, params);
			} else if (msg instanceof ClientActionMsg) {
				LOG.info("接收到【事务操作】消息:{}", msg.toString());
				FlowHeader.putProducts(RoleController.CLIENT_ACTION_WORKDER, params);
			} else if (msg instanceof ReplyClientActionMsg) {
				LOG.info("接收到【回复事务操作】消息:{}", msg.toString());
				FlowHeader.putProducts(RoleController.CLIENT_ACTION_WORKDER, params);
			} else if (msg instanceof AppendLogEntryMsg) {
				LOG.info("接收到【追加日志】消息:{}", msg.toString());
				FlowHeader.putProducts(RoleController.APPEND_LOG_WORKER, params);
			} else if (msg instanceof ReplyAppendLogEntryMsg) {
				LOG.info("接收到【回复追加日志】消息:{}", msg.toString());
				FlowHeader.putProducts(RoleController.APPEND_LOG_WORKER, params);
			} else if (msg instanceof AppendSnapshotMsg) {
				LOG.info("接收到【追加快照】消息:{}", msg.toString());
				FlowHeader.putProducts(RoleController.APPEND_LOG_WORKER, params);
			} else if (msg instanceof ReplyAppendSnapshotMsg) {
				LOG.info("接收到【回复追加快照】消息:{}", msg.toString());
				FlowHeader.putProducts(RoleController.APPEND_LOG_WORKER, params);
			} else {
				LOG.info("接收到【其它不明消息！！！！！！！！！！！！！！！】消息:{}", msg.toString());
			}

		} catch (NoFlowLineException e) {
			LOG.error(e.getMessage(), e);
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
