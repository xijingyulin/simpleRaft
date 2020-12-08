package com.sraft.core.role;

import java.util.concurrent.BlockingQueue;

import com.sraft.core.message.Msg;
import com.sraft.core.message.Packet;
import com.sraft.enums.EnumNodeStatus;

/**
 * 
 * 领导者监控跟随者状态的状态对象
 * 
 * @author 伍尚康-2020年12月7日
 *
 */
public class FollowStatus {

	/**
	 * 节点ID
	 */
	private int nodeId;
	/**
	 * 最后接收到的跟随者的消息，心跳判断用
	 */
	private Msg lastReceviceMsg;

	/**
	 * 节点状态
	 */
	private EnumNodeStatus status;

	/**
	 * 发送事务操作消息时，已发送的消息队列；接收响应时，需要从中移除，并添加响应到packet中
	 */
	private BlockingQueue<Packet> pendingQueue;

	/**
	 * 已复制给跟随者的索引，当matchIndex == leader.lastLogIndex时，才是匹配完成
	 */
	private long matchIndex;

	public FollowStatus(int nodeId) {
		this.nodeId = nodeId;
	}

	public Msg getLastReceviceMsg() {
		return lastReceviceMsg;
	}

	public synchronized void setLastReceviceMsg(Msg lastReceviceMsg) {
		this.lastReceviceMsg = lastReceviceMsg;
	}

	public EnumNodeStatus getStatus() {
		return status;
	}

	public synchronized void setStatus(EnumNodeStatus status) {
		this.status = status;
	}

	public BlockingQueue<Packet> getPendingQueue() {
		return pendingQueue;
	}

	public void setPendingQueue(BlockingQueue<Packet> pendingQueue) {
		this.pendingQueue = pendingQueue;
	}

	public long getMatchIndex() {
		return matchIndex;
	}

	public void appendLogEntryMsg(long matchIndex) {
		this.matchIndex = matchIndex;
	}

	public int getNodeId() {
		return nodeId;
	}

	public void setMatchIndex(long matchIndex) {
		this.matchIndex = matchIndex;
	}

}
