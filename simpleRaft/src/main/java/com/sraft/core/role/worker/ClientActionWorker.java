package com.sraft.core.role.worker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sraft.client.ClientConnManager;
import com.sraft.common.DateHelper;
import com.sraft.common.IdGenerateHelper;
import com.sraft.core.log.LogData;
import com.sraft.core.message.BaseLog;
import com.sraft.core.message.ClientActionMsg;
import com.sraft.core.message.Msg;
import com.sraft.core.message.Packet;
import com.sraft.core.message.ReplyClientActionMsg;
import com.sraft.core.role.AppendTask;
import com.sraft.core.role.Candidate;
import com.sraft.core.role.Follower;
import com.sraft.core.role.Leader;
import com.sraft.core.role.RoleController;
import com.sraft.enums.EnumLoginStatus;
import com.sraft.enums.EnumServiceStatus;

import io.netty.channel.ChannelHandlerContext;

public class ClientActionWorker extends Workder {
	private static Logger LOG = LoggerFactory.getLogger(ClientActionWorker.class);

	private ClientConnManager clientConnManager;
	private static final int BATCH_ACTION_SIZE = 1000;

	public ClientActionWorker(RoleController roleController) {
		super(roleController);
		new Thread(new DealClientActionThread()).start();
	}

	public ClientActionWorker(ClientConnManager clientConnManager) {
		super(null);
		this.clientConnManager = clientConnManager;
		setChangeRole(false);
	}

	@Override
	public void doWork(Object object) {
		List<Object> params = (List<Object>) object;
		ChannelHandlerContext ctx = (ChannelHandlerContext) params.get(0);
		Object msg = params.get(1);
		if (msg instanceof ClientActionMsg) {
			add2Queue(object);
		} else if (msg instanceof ReplyClientActionMsg) {
			ReplyClientActionMsg replyClientActionMsg = (ReplyClientActionMsg) params.get(1);
			try {
				dealReplyClientActionMsg(replyClientActionMsg);
			} catch (IOException e) {
				e.printStackTrace();
				LOG.error(e.getMessage());
			}
		}
	}

	private LinkedBlockingQueue<Object> actionQueue = new LinkedBlockingQueue<Object>(BATCH_ACTION_SIZE);

	public void add2Queue(Object object) {
		actionQueue.add(object);
	}

	class DealClientActionThread implements Runnable {

		@Override
		public void run() {
			List<Object> actionList = new ArrayList<Object>();
			while (true) {
				while (!actionQueue.isEmpty()) {
					try {
						actionList.add(actionQueue.take());
						if (actionList.size() > BATCH_ACTION_SIZE) {
							break;
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
						LOG.error(e.getMessage());
					}
				}
				if (!actionList.isEmpty()) {
					dealClientActionMsg(actionList);
					MSG_NOT_DEAL.addAndGet(-actionList.size());
					actionList.clear();
				} else {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
						LOG.error(e.getMessage());
					}
				}
			}
		}

		private void dealClientActionMsg(List<Object> actionList) {
			List<ChannelHandlerContext> clientChannelList = new ArrayList<ChannelHandlerContext>();
			List<ReplyClientActionMsg> replyClientActionMsgList = new ArrayList<ReplyClientActionMsg>();
			List<BaseLog> baseLogList = new ArrayList<BaseLog>();
			for (Object object : actionList) {
				List<Object> params = (List<Object>) object;
				ChannelHandlerContext ctx = (ChannelHandlerContext) params.get(0);
				ClientActionMsg clientActionMsg = (ClientActionMsg) params.get(1);

				long sessionId = clientActionMsg.getSessionId();
				long receviceTime = clientActionMsg.getReceviceTime();
				boolean isUpdate = roleController.updateSession(sessionId, receviceTime);

				ReplyClientActionMsg replyClientActionMsg = new ReplyClientActionMsg();
				replyClientActionMsg.setActionType(clientActionMsg.getActionType());
				replyClientActionMsg.setMsgId(IdGenerateHelper.getMsgId());
				replyClientActionMsg.setMsgType(Msg.TYPE_REPLY_CLIENT_ACTION);
				replyClientActionMsg.setSessionId(sessionId);
				replyClientActionMsg.setTransactionId(clientActionMsg.getTransactionId());
				if (role instanceof Follower) {
					replyClientActionMsg.setResult(Msg.RETURN_STATUS_FALSE);
					replyClientActionMsg.setErrCode(Msg.ERR_CODE_LOGIN_FOLLOWER);
				} else if (role instanceof Candidate) {
					replyClientActionMsg.setResult(Msg.RETURN_STATUS_FALSE);
					replyClientActionMsg.setErrCode(Msg.ERR_CODE_LOGIN_CANDIDATE);
				} else if (role instanceof Leader) {
					if (!isUpdate) {
						replyClientActionMsg.setResult(Msg.RETURN_STATUS_FALSE);
						replyClientActionMsg.setErrCode(Msg.ERR_CODE_SESSION_TIMEOUT);
					} else {
						Leader leader = (Leader) role;
						if (!leader.isAliveOverHalf()) {
							replyClientActionMsg.setResult(Msg.RETURN_STATUS_FALSE);
							replyClientActionMsg.setErrCode(Msg.ERR_CODE_LOGIN_LEADER_NO_MAJOR);
						} else {
							LOG.info("接收到事务操作消息:{}", clientActionMsg.toString());

							if (leader.isRepeatTransaction(clientActionMsg)
									|| clientActionMsg.getActionType() == LogData.LOG_GET) {
								LOG.info("已执行过这个操作,直接返回结果");
								if (clientActionMsg.getActionType() == LogData.LOG_GET) {
									replyClientActionMsg
											.setValue(leader.getRoleController().getValue(clientActionMsg.getKey()));
								}
								replyClientActionMsg.setResult(Msg.RETURN_STATUS_OK);
							} else {
								baseLogList.add(leader.getBaseLog(clientActionMsg));
								clientChannelList.add(ctx);
								replyClientActionMsgList.add(replyClientActionMsg);
								continue;
							}
						}
					}
				}
				replyClientActionMsg.setSendTime(DateHelper.formatDate2Long(new Date(), DateHelper.YYYYMMDDHHMMSSsss));
				ctx.writeAndFlush(replyClientActionMsg);
			}

			if (!baseLogList.isEmpty()) {
				Leader leader = (Leader) role;
				AppendTask appendTask = new AppendTask(baseLogList);
				synchronized (appendTask) {
					LOG.info("提交任务:{}", appendTask.toString());
					leader.submitAppendTask(appendTask);
					try {
						appendTask.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				if (appendTask.isOverHalfSuccess()) {
					LOG.info("提交任务成功:{}", appendTask.toString());
					// 提交状态机
					leader.getRoleController().commit();
					reply(clientChannelList, replyClientActionMsgList, true);
				} else {
					reply(clientChannelList, replyClientActionMsgList, false);
					LOG.info("提交任务失败:{}", appendTask.toString());
				}
			}
		}
	}

	public void reply(List<ChannelHandlerContext> clientChannelList,
			List<ReplyClientActionMsg> replyClientActionMsgList, boolean isSuccess) {
		for (int i = 0; i < replyClientActionMsgList.size(); i++) {

			ChannelHandlerContext ctx = clientChannelList.get(i);
			ReplyClientActionMsg replyClientActionMsg = replyClientActionMsgList.get(i);
			replyClientActionMsg.setSendTime(DateHelper.formatDate2Long(new Date(), DateHelper.YYYYMMDDHHMMSSsss));
			if (isSuccess) {
				replyClientActionMsg.setResult(Msg.RETURN_STATUS_OK);
			} else {
				replyClientActionMsg.setResult(Msg.RETURN_STATUS_FALSE);
				replyClientActionMsg.setErrCode(Msg.ERR_CODE_LOG_APPEND_FALSE);
			}
			ctx.writeAndFlush(replyClientActionMsg);
		}
	}

	//	private void dealClientActionMsg(ChannelHandlerContext ctx, ClientActionMsg clientActionMsg) {
	//		long sessionId = clientActionMsg.getSessionId();
	//		long receviceTime = clientActionMsg.getReceviceTime();
	//		boolean isUpdate = roleController.updateSession(sessionId, receviceTime);
	//		ReplyClientActionMsg replyClientActionMsg = new ReplyClientActionMsg();
	//		replyClientActionMsg.setActionType(clientActionMsg.getActionType());
	//		replyClientActionMsg.setMsgId(IdGenerateHelper.getMsgId());
	//		replyClientActionMsg.setMsgType(Msg.TYPE_REPLY_CLIENT_ACTION);
	//		replyClientActionMsg.setSessionId(sessionId);
	//
	//		if (role instanceof Follower) {
	//			replyClientActionMsg.setResult(Msg.RETURN_STATUS_FALSE);
	//			replyClientActionMsg.setErrCode(Msg.ERR_CODE_LOGIN_FOLLOWER);
	//		} else if (role instanceof Candidate) {
	//			replyClientActionMsg.setResult(Msg.RETURN_STATUS_FALSE);
	//			replyClientActionMsg.setErrCode(Msg.ERR_CODE_LOGIN_CANDIDATE);
	//		} else if (role instanceof Leader) {
	//			if (!isUpdate) {
	//				replyClientActionMsg.setResult(Msg.RETURN_STATUS_FALSE);
	//				replyClientActionMsg.setErrCode(Msg.ERR_CODE_SESSION_TIMEOUT);
	//			} else {
	//				Leader leader = (Leader) role;
	//				if (!leader.isAliveOverHalf()) {
	//					replyClientActionMsg.setResult(Msg.RETURN_STATUS_FALSE);
	//					replyClientActionMsg.setErrCode(Msg.ERR_CODE_LOGIN_LEADER_NO_MAJOR);
	//				} else {
	//					LOG.info("接收到事务操作消息:{}", clientActionMsg.toString());
	//
	//					if (leader.isRepeatTransaction(clientActionMsg)) {
	//						LOG.info("已执行过这个操作,直接返回结果");
	//						if (clientActionMsg.getActionType() == LogData.LOG_GET) {
	//							replyClientActionMsg
	//									.setValue(leader.getRoleController().getValue(clientActionMsg.getKey()));
	//						}
	//						replyClientActionMsg.setResult(Msg.RETURN_STATUS_OK);
	//					} else {
	//						// 注意读操作，不需累计索引
	//						BaseLog baseLog = leader.getBaseLog(clientActionMsg);
	//						AppendTask appendTask = new AppendTask(baseLog);
	//						synchronized (appendTask) {
	//							LOG.info("提交任务:{}", appendTask.toString());
	//							leader.submitAppendTask(appendTask);
	//							try {
	//								appendTask.wait();
	//							} catch (InterruptedException e) {
	//								e.printStackTrace();
	//							}
	//						}
	//						if (appendTask.isOverHalfSuccess()) {
	//							LOG.info("提交任务成功:{}", appendTask.toString());
	//							// 提交状态机
	//							leader.getRoleController().commit();
	//							if (baseLog.getLogType() == LogData.LOG_GET) {
	//								replyClientActionMsg.setValue(leader.getRoleController().getValue(baseLog.getKey()));
	//							}
	//							replyClientActionMsg.setResult(Msg.RETURN_STATUS_OK);
	//						} else {
	//							LOG.info("提交任务失败:{}", appendTask.toString());
	//							replyClientActionMsg.setResult(Msg.RETURN_STATUS_FALSE);
	//							replyClientActionMsg.setErrCode(Msg.ERR_CODE_LOG_APPEND_FALSE);
	//						}
	//					}
	//				}
	//			}
	//		}
	//		replyClientActionMsg.setSendTime(DateHelper.formatDate2Long(new Date(), DateHelper.YYYYMMDDHHMMSSsss));
	//		ctx.writeAndFlush(replyClientActionMsg);
	//	}

	private void dealReplyClientActionMsg(ReplyClientActionMsg replyClientActionMsg) throws IOException {
		int result = replyClientActionMsg.getResult();
		clientConnManager.updateLastReceiveMsg(replyClientActionMsg);

		BlockingQueue<Packet> pendingQueue = clientConnManager.getPendingQueue();
		synchronized (pendingQueue) {
			if (!pendingQueue.isEmpty()) {
				Packet packet = pendingQueue.remove();
				synchronized (packet) {
					if (((ClientActionMsg) packet.getSendMsg()).getTransactionId() != replyClientActionMsg
							.getTransactionId()) {
						throw new IOException("接收到的消息的事务ID与从队列中取出的消息的事务ID不一致");
					}
					packet.setReplyMsg(replyClientActionMsg);
					packet.notify();
					if (packet.getCall() != null) {
						clientConnManager.getCallBackQueue().add(packet);
						synchronized (clientConnManager.getCallBackQueue()) {
							clientConnManager.getCallBackQueue().notify();
						}
					}
				}
			}
		}

		if (result == Msg.RETURN_STATUS_OK) {
			clientConnManager.updateServiceStatus(EnumServiceStatus.USEFULL);
		} else {
			int errCode = replyClientActionMsg.getErrCode();
			switch (errCode) {
			case Msg.ERR_CODE_LOGIN_FOLLOWER:
				clientConnManager.updateServiceStatus(EnumServiceStatus.UN_USEFULL);
				clientConnManager.updateLoginStatus(EnumLoginStatus.FALSE);
				LOG.error("连接到跟随者,需要重新登录");
				break;
			case Msg.ERR_CODE_LOGIN_CANDIDATE:
				clientConnManager.updateServiceStatus(EnumServiceStatus.UN_USEFULL);
				clientConnManager.updateLoginStatus(EnumLoginStatus.FALSE);
				LOG.error("连接到候选者,需要重新登录");
				break;
			case Msg.ERR_CODE_LOGIN_LEADER_NO_MAJOR:
				clientConnManager.updateServiceStatus(EnumServiceStatus.UN_USEFULL);
				clientConnManager.updateLastReceiveMsg(replyClientActionMsg);
				LOG.error("由于没有过半存活机器，领导者暂停服务");
				break;
			case Msg.ERR_CODE_SESSION_TIMEOUT:
				clientConnManager.updateServiceStatus(EnumServiceStatus.UN_USEFULL);
				clientConnManager.updateLoginStatus(EnumLoginStatus.FALSE);
				LOG.error("会话超时,需要重新登录");
				break;
			case Msg.ERR_CODE_ROLE_CHANGED:
				clientConnManager.updateServiceStatus(EnumServiceStatus.UN_USEFULL);
				clientConnManager.updateLoginStatus(EnumLoginStatus.FALSE);
				LOG.error("角色已改变,需要重新登录");
				break;
			case Msg.ERR_CODE_LOG_APPEND_FALSE:
				clientConnManager.updateLoginStatus(EnumLoginStatus.FALSE);
				LOG.error("追加失败");
				break;
			default:
				clientConnManager.updateServiceStatus(EnumServiceStatus.UN_USEFULL);
				clientConnManager.updateLoginStatus(EnumLoginStatus.FALSE);
				LOG.error("其它原因,需要重新登录");
				break;
			}
		}
	}
}
