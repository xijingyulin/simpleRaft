package com.sraft.core.role.worker;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sraft.common.flow.IFlowWorker;
import com.sraft.core.role.AbstractRoles;

public abstract class Workder implements IFlowWorker {
	private static Logger LOG = LoggerFactory.getLogger(Workder.class);

	protected BlockingQueue<Object> msgQueue = new LinkedBlockingQueue<Object>();

	/**
	 * 因为任期不一致，选举超时等消息，需要转换角色，避免在停止角色后继续处理消息，暂时将消息放到队列中
	 */
	private boolean enable = false;
	private String LOCK_WORK = new String("LOCK_WORK");

	protected AbstractRoles role = null;

	@Override
	public void deliver(Object object) {
		if (!isEnable()) {
			msgQueue.add(object);
			return;
		}
		while (!msgQueue.isEmpty() && isEnable()) {
			try {
				Object oldMsg = msgQueue.take();
				doWork(oldMsg);
			} catch (InterruptedException e) {
				e.printStackTrace();
				LOG.error(e.getMessage(), e);
			}
		}
		doWork(object);
	}

	public abstract void doWork(Object object);

	public BlockingQueue<Object> getMsgQueue() {
		return msgQueue;
	}

	public void setMsgQueue(BlockingQueue<Object> msgQueue) {
		this.msgQueue = msgQueue;
	}

	public AbstractRoles getRole() {
		return role;
	}

	public void setRole(AbstractRoles role) {
		this.role = role;
	}

	public boolean isEnable() {
		synchronized (LOCK_WORK) {
			return enable;
		}
	}

	public void setEnable(boolean enable) {
		synchronized (LOCK_WORK) {
			this.enable = enable;
		}
	}

	/**
	 * 比较任期
	 * 
	 * @param fromTerm
	 * @return
	 */
//	public boolean isLargeTerm(long fromTerm) {
//		if (fromTerm > role.getCurrentTerm()) {
//			return true;
//		} else {
//			return false;
//		}
//	}
}
