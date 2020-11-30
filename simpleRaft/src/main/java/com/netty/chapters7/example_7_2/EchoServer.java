package com.netty.chapters7.example_7_2;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class EchoServer {

	public void bind(int port) {
		//两个NioEventLoopGroup。一个负责客户端连接，一个负责读写事件
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try {
			//启动辅助类
			ServerBootstrap bootstrap = new ServerBootstrap();
			bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
					.option(ChannelOption.SO_BACKLOG, 100).handler(new LoggingHandler(LogLevel.INFO))
					.childHandler(new ChannelInitializer<SocketChannel>() {
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
							ch.pipeline().addLast(new EchoServerHandler());
						}
					});
			ChannelFuture future = bootstrap.bind(port).sync();
			//等待服务端监听端口关闭
			future.channel().closeFuture().sync();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}

	public static void main(String args[]) {
		new EchoServer().bind(8080);
	}
}
