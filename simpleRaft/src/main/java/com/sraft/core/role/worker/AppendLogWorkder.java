package com.sraft.core.role.worker;

import java.util.List;

import com.sraft.core.message.AppendLogEntryMsg;
import com.sraft.core.message.ReplyAppendLogEntryMsg;

import io.netty.channel.ChannelHandlerContext;

public class AppendLogWorkder extends Workder {

	@Override
	public void doWork(Object object) {
		List<Object> params = (List<Object>) object;
		ChannelHandlerContext ctx = (ChannelHandlerContext) params.get(0);
		Object msg = params.get(1);
		if (msg instanceof AppendLogEntryMsg) {
			AppendLogEntryMsg appendLogEntryMsg = (AppendLogEntryMsg) params.get(1);
			dealAppendLogEntryMsg(ctx, appendLogEntryMsg);
		} else if (msg instanceof ReplyAppendLogEntryMsg) {
			ReplyAppendLogEntryMsg replyAppendLogEntryMsg = (ReplyAppendLogEntryMsg) params.get(1);
			dealReplyAppendLogEntryMsg(replyAppendLogEntryMsg);
		}
	}

	public void dealAppendLogEntryMsg(ChannelHandlerContext ctx, AppendLogEntryMsg appendLogEntryMsg) {
		long fromTerm = appendLogEntryMsg.getTerm();
		boolean isPassTerm = checkTerm(fromTerm);
		ReplyAppendLogEntryMsg replyAppendLogEntryMsg = new ReplyAppendLogEntryMsg();
		//replyAppendLogEntryMsg.setAppendType(appendType);
	}

	public void dealReplyAppendLogEntryMsg(ReplyAppendLogEntryMsg replyAppendLogEntryMsg) {

	}
	
	public boolean checkTerm(long fromTerm) {
		if (fromTerm >= role.getCurrentTerm()) {
			return true;
		} else {
			return false;
		}
	}
}
