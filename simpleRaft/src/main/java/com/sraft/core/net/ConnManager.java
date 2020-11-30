package com.sraft.core.net;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sraft.core.message.Msg;
import com.sraft.core.message.handler.ToServerHandler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
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

public class ConnManager {
	private static Logger LOG = LoggerFactory.getLogger(ConnManager.class);

	private static volatile ConnManager instance = null;
	private Map<Integer, Channel> channelMap = new ConcurrentHashMap<Integer, Channel>();
	private Map<Integer, ServerConn> serverMap = new ConcurrentHashMap<Integer, ServerConn>();

	public static ConnManager getInstance() {
		if (instance == null) {
			synchronized (ConnManager.class) {
				if (instance == null) {
					instance = new ConnManager();
				}
			}
		}
		return instance;
	}

	public synchronized void openService(int port) throws InterruptedException {
		ServerConn serverConn = serverMap.get(port);
		if (serverConn == null) {
			serverConn = new ServerConn();
			serverConn.bind(port);
			serverMap.put(port, serverConn);
		}
	}

	public Channel connect(final ServerAddress serverAddress) {
		Channel channel = null;
		int nodeId = serverAddress.getNodeId();
		if (channelMap.containsKey(nodeId)) {
			channel = channelMap.get(nodeId);
		} else {
			final EventLoopGroup group = new NioEventLoopGroup();
			Bootstrap bootstrap = new Bootstrap();
			bootstrap.group(group).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true)
					.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
					.handler(new ChannelInitializer<SocketChannel>() {
						@Override
						protected void initChannel(SocketChannel ch) throws Exception {
							// 顺序不能乱，同类的要有先后顺序
							// 第一个先继承ByteToMessageDecoder，第二个再继承MessageToMessageDecoder
							// 字节最大长度，消息大小字段位置偏移值，消息大小字段的长度，消息大小字段的长度（用来解码后保留消息大小字段），解码时跳过的长度（通常长度位于头部，解码后不需要长度字段，所以可以跳过）
							ch.pipeline().addLast("frameDecoder", new LengthFieldBasedFrameDecoder(65535, 0, 2, 0, 2));
							ch.pipeline().addLast("msgpack decoder", new MsgpackDecoder());
							// 第一个继承MessageToMessageEncoder，第二个继承MessageToByteEncoder
							//LengthFieldPrepender将消息长度加在消息头中，结合LengthFieldBasedFrameDecoder使用，可以解决半包问题
							//参数，长度字段大小
							ch.pipeline().addLast("frameEncoder", new LengthFieldPrepender(2));
							ch.pipeline().addLast("msgpack encoder", new MsgpackEncoder());

							ch.pipeline().addLast(new ToServerHandler());
						}
					});
			try {
				ChannelFuture future = bootstrap.connect(serverAddress.getHost(), serverAddress.getPort()).sync();
				if (future.isSuccess()) {
					channel = future.channel();
					channelMap.put(serverAddress.getNodeId(), channel);
					channel.closeFuture().addListeners(new ChannelFutureListener() {
						@Override
						public void operationComplete(ChannelFuture future) throws Exception {
							group.shutdownGracefully();
							channelMap.remove(serverAddress.getNodeId());
						}
					});
				}
			} catch (Throwable e) {
				channel = null;
				group.shutdownGracefully();
				//LOG.error("连接服务器失败,地址:{}", serverAddress);
			}
		}
		return channel;
	}

	public Channel connect(String host, int port) {
		Channel channel = null;

		final EventLoopGroup group = new NioEventLoopGroup();
		Bootstrap bootstrap = new Bootstrap();
		bootstrap.group(group).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true)
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000).handler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						// 顺序不能乱，同类的要有先后顺序
						// 第一个先继承ByteToMessageDecoder，第二个再继承MessageToMessageDecoder
						// 字节最大长度，消息大小字段位置偏移值，消息大小字段的长度，消息大小字段的长度（用来解码后保留消息大小字段），解码时跳过的长度（通常长度位于头部，解码后不需要长度字段，所以可以跳过）
						ch.pipeline().addLast("frameDecoder", new LengthFieldBasedFrameDecoder(65535, 0, 2, 0, 2));
						ch.pipeline().addLast("msgpack decoder", new MsgpackDecoder());
						// 第一个继承MessageToMessageEncoder，第二个继承MessageToByteEncoder
						//LengthFieldPrepender将消息长度加在消息头中，结合LengthFieldBasedFrameDecoder使用，可以解决半包问题
						//参数，长度字段大小
						ch.pipeline().addLast("frameEncoder", new LengthFieldPrepender(2));
						ch.pipeline().addLast("msgpack encoder", new MsgpackEncoder());

						ch.pipeline().addLast(new ToServerHandler());
					}
				});
		try {
			ChannelFuture future = bootstrap.connect(host, port).sync();
			if (future.isSuccess()) {
				channel = future.channel();
				channel.closeFuture().addListeners(new ChannelFutureListener() {
					@Override
					public void operationComplete(ChannelFuture future) throws Exception {
						group.shutdownGracefully();
					}
				});
			}
		} catch (Throwable e) {
			channel = null;
			group.shutdownGracefully();
		}
		return channel;
	}

	public void close(int nodeId) {
		Channel channel = channelMap.remove(nodeId);
		channel.close();
	}

	public void closeAll() {
		Iterator<Integer> it = channelMap.keySet().iterator();
		while (it.hasNext()) {
			int nodeId = it.next();
			Channel channel = channelMap.get(nodeId);
			channel.close();
			it.remove();
		}
	}

	public boolean sendMsg(ServerAddress serverAddress, Msg msg) {
		boolean isSuccess = false;
		Channel channel = connect(serverAddress);
		if (channel != null) {
			try {
				channel.writeAndFlush(msg).sync();
				isSuccess = true;
			} catch (InterruptedException e) {
				//e.printStackTrace();
				//LOG.error(e.getMessage(), e);
				isSuccess = false;
			}
		}
		//		if (!isSuccess) {
		//			LOG.error("发送消息失败,地址:{},内容:{}", serverAddress, msg);
		//		}
		return isSuccess;
	}

}
