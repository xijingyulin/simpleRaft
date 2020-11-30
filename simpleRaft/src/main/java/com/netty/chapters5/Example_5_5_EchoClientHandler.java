package com.netty.chapters5;

import java.io.UnsupportedEncodingException;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

public class Example_5_5_EchoClientHandler extends ChannelHandlerAdapter {

	private int counter;
	static final String ECHO_REQ = "01234567890123456789";

	public Example_5_5_EchoClientHandler() {
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
		for (int i = 0; i < 100; i++) {
			ctx.writeAndFlush(Unpooled.copiedBuffer(ECHO_REQ.getBytes()));
		}

	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws UnsupportedEncodingException {
		String body = (String) msg;
		System.out.println("This is " + ++counter + " times receive server : [" + msg + "]");
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		//释放资源
		cause.printStackTrace();
		ctx.close();
	}
}
