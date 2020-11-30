package com.netty.chapters3;

import java.io.UnsupportedEncodingException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

public class Example_3_1_TimeClientHandler extends ChannelHandlerAdapter {

	private final ByteBuf firstMessage;

	public Example_3_1_TimeClientHandler() {
		byte[] req = "QUERY TIME ORDER".getBytes();
		firstMessage = Unpooled.buffer(req.length);
		firstMessage.writeBytes(req);
	}

	/*
	 * 当客户端和服务端建立链路后，Netty的NIO线程会调用channelActive方法，通过writeAndFlush发送数据给对端
	 * 
	 * (non-Javadoc)
	 * 
	 * @see io.netty.channel.ChannelHandlerAdapter#channelActive(io.netty.channel.
	 * ChannelHandlerContext)
	 */
	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		ctx.writeAndFlush(firstMessage);

	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws UnsupportedEncodingException {
		ByteBuf buf = (ByteBuf) msg;
		byte[] req = new byte[buf.readableBytes()];
		buf.readBytes(req);
		String body = new String(req, "utf-8");
		System.out.println("Now is :" + body);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		//释放资源
		cause.printStackTrace();
		ctx.close();
	}
}
