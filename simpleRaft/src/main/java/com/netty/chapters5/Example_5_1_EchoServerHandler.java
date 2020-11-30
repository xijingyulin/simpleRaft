package com.netty.chapters5;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

public class Example_5_1_EchoServerHandler extends ChannelHandlerAdapter {

	int counter = 0;

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		String body = (String) msg;
		System.out.println("This is " + ++counter + " times receive client : [" + body + "]");
		body = body + "$_";
		ByteBuf buf = Unpooled.copiedBuffer(body.getBytes());

		ctx.writeAndFlush(buf);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		ctx.close();
	}
}
