package com.netty.test;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

public class EchoClient {

	public void connect(String host, int port) {
		final EventLoopGroup group = new NioEventLoopGroup();
		try {
			Bootstrap bootstrap = new Bootstrap();
			bootstrap.group(group).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true)
					.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
					.handler(new ChannelInitializer<SocketChannel>() {

						@Override
						protected void initChannel(SocketChannel ch) throws Exception {
							//顺序不能乱，同类的要有先后顺序
							// 第一个先继承ByteToMessageDecoder，第二个再继承MessageToMessageDecoder
							// 字节最大长度，消息大小字段位置偏移值，消息大小字段的长度，消息大小字段的长度（用来解码后保留消息大小字段），解码时跳过的长度（通常长度位于头部，解码后不需要长度字段，所以可以跳过）
							ch.pipeline().addLast("frameDecoder", new LengthFieldBasedFrameDecoder(65535, 0, 2, 0, 2));
							ch.pipeline().addLast("msgpack decoder", new MsgpackDecoder());
							// 第一个继承MessageToMessageEncoder，第二个继承MessageToByteEncoder
							//LengthFieldPrepender将消息长度加在消息头中，结合LengthFieldBasedFrameDecoder使用，可以解决半包问题
							//参数，长度字段大小
							ch.pipeline().addLast("frameEncoder", new LengthFieldPrepender(2));
							ch.pipeline().addLast("msgpack encoder", new MsgpackEncoder());

							ch.pipeline().addLast(new EchoClientHandler(10));
						}
					});
			ChannelFuture future = bootstrap.connect(host, port).sync();

			future.channel().closeFuture().addListeners(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					System.out.println("连接断开");
					group.shutdownGracefully();
				}
			});
			// 等待客户端链路关闭
//			future.channel().closeFuture().sync();
		} catch (Exception e) {
			//e.printStackTrace();
			System.out.println("error");
		} finally {
			//group.shutdownGracefully();
		}
		System.out.println("here");
	}

	public static void main(String args[]) {
		new EchoClient().connect("127.0.0.1", 8080);
	}
}
