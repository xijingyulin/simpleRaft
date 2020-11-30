package com.sraft.common.flow;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 流水线主任
 * 
 * @author 伍尚康-2020年11月22日
 *
 */
public final class FlowHeader {

	/**
	 * key:工牌 value:流水线工人
	 */
	protected static ConcurrentHashMap<String, IFlowWorker> FLOW_WORKER_MAP = new ConcurrentHashMap<String, IFlowWorker>();
	/**
	 * key:工牌 value:每个工人所属流水线
	 */
	protected static ConcurrentHashMap<String, BlockingQueue<Object>> FLOW_LINE_MAP = new ConcurrentHashMap<String, BlockingQueue<Object>>();

	/**
	 * key:工牌 value:每个工人所属流水线的工作台
	 */
	protected static ConcurrentHashMap<String, Thread> FLOW_LINE_PLAT_MAP = new ConcurrentHashMap<String, Thread>();

	/**
	 * 响铃，货物到来
	 */
	protected static String BELLS = new String("BELLS");

	/**
	 * 对HR人事管理进行限制，避免同时雇佣又解雇同一个工人
	 */
	protected static String HR_LOCK = new String("HR");

	public static void employ(String workerCard, IFlowWorker flowWorkder) {
		synchronized (HR_LOCK) {
			if (!FLOW_WORKER_MAP.containsKey(workerCard)) {
				FLOW_WORKER_MAP.put(workerCard, flowWorkder);
				FLOW_LINE_MAP.put(workerCard, new LinkedBlockingQueue<Object>());
				Thread flowLinePlat = new Delivery(workerCard);
				flowLinePlat.start();
				FLOW_LINE_PLAT_MAP.put(workerCard, flowLinePlat);
			}
		}
	}

	public static void unEmploy(String workerCard) {
		synchronized (HR_LOCK) {
			if (FLOW_LINE_PLAT_MAP.containsKey(workerCard)) {
				FLOW_LINE_MAP.remove(workerCard);
				FLOW_WORKER_MAP.remove(workerCard);

				Thread flowLinePlat = FLOW_LINE_PLAT_MAP.get(workerCard);
				flowLinePlat.interrupt();
				FLOW_LINE_PLAT_MAP.remove(workerCard);
			}
		}
	}

	/**
	 * 推送待加工货物进来
	 * 
	 * @param workerCard
	 *            加工该货物的工人的工号
	 * @param product
	 *            待加工货物
	 * @throws NoFlowLineException
	 *             当在流水线车间时找不到该工人，说明工人被解雇了，抛出异常
	 */
	@Deprecated
	public static void putProducts_bak(String workerCard, Object product) throws NoFlowLineException {
		if (!FLOW_LINE_MAP.containsKey(workerCard)) {
			throw new NoFlowLineException("工人被解雇了,工号:" + workerCard);
		} else {
			FLOW_LINE_MAP.get(workerCard).add(product);
			synchronized (BELLS) {
				BELLS.notifyAll();
			}
		}
	}

	public static void putProducts(String workerCard, Object product) throws NoFlowLineException {
		BlockingQueue<Object> queue = FLOW_LINE_MAP.get(workerCard);
		if (queue == null) {
			throw new NoFlowLineException("工人被解雇了,工号:" + workerCard);
		} else {
			queue.add(product);
			synchronized (queue) {
				queue.notifyAll();
			}
		}
	}
}
