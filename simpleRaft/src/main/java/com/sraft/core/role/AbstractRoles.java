package com.sraft.core.role;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import com.sraft.core.message.HeartbeatMsg;
import com.sraft.enums.EnumRole;

public abstract class AbstractRoles extends Thread implements IRole {
	protected RoleController roleController = null;

	/**
	 * 本身id
	 */
	protected int selfId;

	//状态数据
	protected EnumRole playRole = EnumRole.FOLLOWER;
	protected AtomicLong currentTerm = new AtomicLong(0);

	/**
	 * 标记投票给的候选人的ID，作用是避免同个任期内给多个候选人投票，这种情况一般只会发生在初次选举；正常而言，votedFor总是不为-1
	 */
	protected volatile int votedFor = -1;
	/**
	 * 避免某种临界点场景，例如候选者在刚好超时，关闭所有操作的时候，恰好接收到过半的投票；那么超时这里会转换角色，过半投票那里也要转换角色
	 * 
	 * 领导者各种事务操作时，前提条件都是没有转换角色
	 */
	protected volatile boolean isChangedRole = false;

	/**
	 * 是否接收到心跳，首先接收到的心跳里面任期需要大于等于自己的，才是有效的心跳
	 */
	private volatile HeartbeatMsg heartbeatMsg = null;

	public AbstractRoles(EnumRole playRole, RoleController roleController) throws IOException {
		this.playRole = playRole;
		this.selfId = roleController.getConfig().getSelfId();
		this.roleController = roleController;
		// 恢复任期
		currentTerm.set(roleController.getTermAndVotedForService().retrieveTerm());
		votedFor = roleController.getTermAndVotedForService().retrieveVotedFor();
	}

	public boolean updateTerm(long fromTerm) {
		synchronized (currentTerm) {
			if (fromTerm > currentTerm.get()) {
				currentTerm.set(fromTerm);
				roleController.getTermAndVotedForService().writeTerm(currentTerm.get());
				return true;
			} else {
				return false;
			}
		}
	}

	public synchronized boolean updateVotedFor(int nodeId) {
		if (nodeId != votedFor) {
			votedFor = nodeId;
			roleController.getTermAndVotedForService().writeVotedFor(votedFor);
			return true;
		} else {
			return false;
		}
	}

	public abstract void nextRole(EnumRole newRole);

	public EnumRole getPlayRole() {
		return playRole;
	}

	public long getCurrentTerm() {
		return currentTerm.get();
	}

	public int getVotedFor() {
		return votedFor;
	}

	public RoleController getRoleController() {
		return roleController;
	}

	public void enableWorker(AbstractRoles role) {
		roleController.getLoginWorkder().setRole(role);
		roleController.getClientHeartbeatWorker().setRole(role);
		roleController.getClientActionWorkder().setRole(role);

		roleController.getHeatBeatWorkder().setRole(role);
		roleController.getHeatBeatWorkder().setEnable(true);
		roleController.getAppendLogWorkder().setRole(role);
		roleController.getAppendLogWorkder().setEnable(true);
		roleController.getRequestVoteWorker().setRole(role);
		roleController.getRequestVoteWorker().setEnable(true);

	}

	public void disableWorker() {
		roleController.getLoginWorkder().setRole(null);
		roleController.getClientHeartbeatWorker().setRole(null);
		roleController.getClientActionWorkder().setRole(null);

		roleController.getHeatBeatWorkder().setEnable(false);
		roleController.getAppendLogWorkder().setEnable(false);
		roleController.getRequestVoteWorker().setEnable(false);

	}

	public HeartbeatMsg getHeartbeatMsg() {
		return heartbeatMsg;
	}

	public void setHeartbeatMsg(HeartbeatMsg heartbeatMsg) {
		this.heartbeatMsg = heartbeatMsg;
	}

	public boolean isChangedRole() {
		return isChangedRole;
	}

	public int getSelfId() {
		return selfId;
	}
}
