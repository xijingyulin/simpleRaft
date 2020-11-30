package com.sraft.common.flow;

import java.util.concurrent.BlockingQueue;

/**
 * 将货物传送给加工流水线
 * 
 * @author 伍尚康-2020年11月24日
 *
 */
public class Delivery extends Thread {
	private String workerCard;

	public Delivery(String workerCard) {
		this.workerCard = workerCard;
	}

	//@Override
	@Deprecated
	public void run_bak() {
		IFlowWorker flowWorker = FlowHeader.FLOW_WORKER_MAP.get(workerCard);
		BlockingQueue<Object> flowLineQueue = FlowHeader.FLOW_LINE_MAP.get(workerCard);
		boolean isUnEmploied = false;
		while (true) {
			if ((isInterrupted() || isUnEmploied) && flowLineQueue.isEmpty()) {
				break;
			}
			synchronized (FlowHeader.BELLS) {
				try {
					if (flowLineQueue.isEmpty()) {
						FlowHeader.BELLS.wait();
					}
				} catch (InterruptedException e) {
					isUnEmploied = true;
				}
			}
			while (!flowLineQueue.isEmpty()) {
				try {
					flowWorker.deliver(flowLineQueue.take());
				} catch (InterruptedException e) {
					isUnEmploied = true;
				}
			}
		}
	}

	@Override
	public void run() {
		IFlowWorker flowWorker = FlowHeader.FLOW_WORKER_MAP.get(workerCard);
		BlockingQueue<Object> flowLineQueue = FlowHeader.FLOW_LINE_MAP.get(workerCard);
		boolean isUnEmploied = false;
		while (true) {
			if ((isInterrupted() || isUnEmploied) && flowLineQueue.isEmpty()) {
				break;
			}
			synchronized (flowLineQueue) {
				try {
					if (flowLineQueue.isEmpty()) {
						flowLineQueue.wait();
					}
				} catch (InterruptedException e) {
					isUnEmploied = true;
				}
			}
			while (!flowLineQueue.isEmpty()) {
				try {
					flowWorker.deliver(flowLineQueue.take());
				} catch (InterruptedException e) {
					isUnEmploied = true;
				}
			}
		}
	}
}
