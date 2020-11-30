package com.netty.chapters4;

import java.io.UnsupportedEncodingException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

public class Example_4_3_TimeClientHandler extends ChannelHandlerAdapter {

	private int counter;
	private byte[] req;

	public Example_4_3_TimeClientHandler() {
		req = ("QUERY TIME ORDER" + System.getProperty("line.separator")).getBytes();
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
		ByteBuf message = null;
		for (int i = 0; i < 100; i++) {
			message = Unpooled.buffer(req.length);
			message.writeBytes(req);
			ctx.writeAndFlush(message);
		}

	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws UnsupportedEncodingException {
		String body = (String) msg;
		System.out.println("Now is :" + body + " ; the counter is :" + ++counter);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		//释放资源
		cause.printStackTrace();
		ctx.close();
	}
}
