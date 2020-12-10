package com.sraft.core.net;

import java.util.Date;
import java.util.List;

import org.msgpack.MessagePack;

import com.sraft.common.DateHelper;
import com.sraft.core.message.AppendLogEntryMsg;
import com.sraft.core.message.AppendSnapshotMsg;
import com.sraft.core.message.ClientHeartbeatMsg;
import com.sraft.core.message.HeartbeatMsg;
import com.sraft.core.message.LoginMsg;
import com.sraft.core.message.Msg;
import com.sraft.core.message.ReplyAppendLogEntryMsg;
import com.sraft.core.message.ReplyAppendSnapshotMsg;
import com.sraft.core.message.ReplyClientHeartbeatMsg;
import com.sraft.core.message.ReplyHeartbeatMsg;
import com.sraft.core.message.ReplyLoginMsg;
import com.sraft.core.message.ReplyRequestVoteMsg;
import com.sraft.core.message.RequestVoteMsg;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

public class MsgpackDecoder extends MessageToMessageDecoder<ByteBuf> {
	MessagePack messagePack = new MessagePack();

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
		final byte[] array;
		final int length = msg.readableBytes();
		array = new byte[length];
		msg.getBytes(msg.readerIndex(), array, 0, length);

		Msg msgObject = messagePack.read(array, Msg.class);
		Msg outObject = null;
		switch (msgObject.getMsgType()) {
		case Msg.TYPE_REQUEST_VOTE:
			outObject = messagePack.read(array, RequestVoteMsg.class);
			break;
		case Msg.TYPE_REPLY_REQUEST_VOTE:
			outObject = messagePack.read(array, ReplyRequestVoteMsg.class);
			break;
		case Msg.TYPE_HEARTBEAT:
			outObject = messagePack.read(array, HeartbeatMsg.class);
			break;
		case Msg.TYPE_REPLY_HEARTBEAT:
			outObject = messagePack.read(array, ReplyHeartbeatMsg.class);
			break;
		case Msg.TYPE_CLIENT_LOGIN:
			outObject = messagePack.read(array, LoginMsg.class);
			break;
		case Msg.TYPE_REPLY_CLIENT_LOGIN:
			outObject = messagePack.read(array, ReplyLoginMsg.class);
			break;
		case Msg.TYPE_CLIENT_HEARTBEAT:
			outObject = messagePack.read(array, ClientHeartbeatMsg.class);
			break;
		case Msg.TYPE_REPLY_CLIENT_HEARTBEAT:
			outObject = messagePack.read(array, ReplyClientHeartbeatMsg.class);
			break;
		case Msg.TYPE_APPEND_LOG:
			outObject = messagePack.read(array, AppendLogEntryMsg.class);
			break;
		case Msg.TYPE_REPLY_APPEND_LOG:
			outObject = messagePack.read(array, ReplyAppendLogEntryMsg.class);
			break;
		case Msg.TYPE_APPEND_SNAPSHOT:
			outObject = messagePack.read(array, AppendSnapshotMsg.class);
			break;
		case Msg.TYPE_REPLY_APPEND_SNAPSHOT:
			outObject = messagePack.read(array, ReplyAppendSnapshotMsg.class);
			break;
		default:
			outObject = msgObject;
			break;
		}
		outObject.setReceviceTime(DateHelper.formatDate2Long(new Date(), DateHelper.YYYYMMDDHHMMSSsss));
		out.add(outObject);
	}

}
