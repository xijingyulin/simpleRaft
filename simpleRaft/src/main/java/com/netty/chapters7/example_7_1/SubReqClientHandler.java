package com.netty.chapters7.example_7_1;

import java.io.UnsupportedEncodingException;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

public class SubReqClientHandler extends ChannelHandlerAdapter {

	public SubReqClientHandler() {
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
		for (int i = 0; i < 10; i++) {
			ctx.write(subReq(i));
		}
		ctx.flush();

	}

	private SubscribeReq subReq(int i) {
		SubscribeReq req = new SubscribeReq();
		req.setAddress("小新塘新园新村");
		req.setPhoneNumber("15119630287");
		req.setProductName("Netty 权威指南");
		req.setSubReqID(i);
		req.setUserName("WSK");
		return req;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws UnsupportedEncodingException {
		System.out.println("Receive server response :[" + msg + "]");
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
