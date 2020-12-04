package com.sraft.core.role.sender;

import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sraft.common.flow.IFlowWorker;
import com.sraft.core.message.AppendLogEntryMsg;
import com.sraft.core.message.Packet;
import com.sraft.core.message.ReplyAppendLogEntryMsg;
import com.sraft.core.net.ConnManager;
import com.sraft.core.net.ServerAddress;
import com.sraft.core.role.Leader;
import com.sraft.enums.EnumNodeStatus;

public class SendAppendLogWorker implements IFlowWorker {
	private static Logger LOG = LoggerFactory.getLogger(SendAppendLogWorker.class);
	private ServerAddress serverAddress;
	private Leader leader;

	public SendAppendLogWorker(ServerAddress serverAddress, Leader leader) {
		this.serverAddress = serverAddress;
		this.leader = leader;
	}

	private AppendLogEntryMsg appendLogMsg;
	private int nodeId;
	private BlockingQueue<Packet> pendingQueue;

	@Override
	public void deliver(Object object) {
		appendLogMsg = (AppendLogEntryMsg) object;
		nodeId = serverAddress.getNodeId();
		pendingQueue = leader.getPendingQueueMap().get(nodeId);
		int appendType = appendLogMsg.getAppendType();

		if (appendType == AppendLogEntryMsg.TYPE_APPEND_NULL) {
			sendEmptyMsg();
		} else if (appendType == AppendLogEntryMsg.TYPE_APPEND_SYN) {

		} else if (appendType == AppendLogEntryMsg.TYPE_APPEND_ORDINARY) {

		}
	}

	public void sendEmptyMsg() {
		Packet packet = new Packet();
		packet.setSendMsg(appendLogMsg);
		boolean isSuccess = false;
		synchronized (pendingQueue) {
			isSuccess = ConnManager.getInstance().sendMsg(serverAddress, appendLogMsg);
			if (isSuccess) {
				pendingQueue.add(packet);
			} else {
				LOG.info("追加日志失败");
				leader.updateNodeStatus(nodeId, EnumNodeStatus.NODE_LOG_UNSYN);
			}
		}
		try {
			if (isSuccess) {
				synchronized (packet) {
					packet.wait();
				}
				LOG.info("节点:{},追加日志处理完成:{}", nodeId, packet.toString());
				if (packet.getReplyMsg() == null) {
					LOG.info("节点:{},没有响应", nodeId, packet.toString());
				} else {
					ReplyAppendLogEntryMsg replyAppendLogEntryMsg = (ReplyAppendLogEntryMsg) packet.getReplyMsg();
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			LOG.error(e.getMessage(), e);
		}
	}
}
