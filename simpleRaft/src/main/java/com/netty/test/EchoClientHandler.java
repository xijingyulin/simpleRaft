package com.netty.test;

import java.io.UnsupportedEncodingException;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

public class EchoClientHandler extends ChannelHandlerAdapter {

	private final int sendNumber;

	public EchoClientHandler(int sendNumber) {
		this.sendNumber = sendNumber;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		UserInfo[] userInfos = UserInfo();
		for (UserInfo userInfo : userInfos) {
			ctx.write(userInfo);
		}
		ctx.flush();
	}

	private UserInfo[] UserInfo() {
		UserInfo[] userInfos = new UserInfo[this.sendNumber];
		UserInfo userInfo = null;
		for (int i = 0; i < this.sendNumber; i++) {
			userInfo = new UserInfo();
			userInfo.setAge(i);
			userInfo.setName("WSK->" + i);
			userInfos[i] = userInfo;
		}
		return userInfos;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws UnsupportedEncodingException {
		System.out.println("Receive server response :[" + msg + "]");
		//ctx.write(msg);
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		//释放资源
		System.out.println("exceptionCaught");
		cause.printStackTrace();
		ctx.close();
	}
}
