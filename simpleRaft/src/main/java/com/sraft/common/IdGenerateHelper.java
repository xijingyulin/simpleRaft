package com.sraft.common;

import java.util.Date;

/**
 * 
 * ID生成器，生成会话ID，sraft事务ID，客户端事务ID
 * 
 * @author 伍尚康-2020年11月19日
 *
 */
public class IdGenerateHelper {

	private static long msgId = 0;

	public synchronized static long getMsgId() {
		long curr = Long.parseLong(DateHelper.formatDate2Str(new Date(), DateHelper.YYYYMMDDHHMMSS) + "000");
		if (msgId != curr) {
			if (curr < msgId) {
				msgId = msgId + 1;
			} else {
				msgId = curr;
			}
		} else {
			msgId = msgId + 1;
		}
		return msgId;
	}

	/**
	 * 
	 * 获取最新会话ID，传入目前最新会话ID
	 * 
	 * @param newSessionId
	 * @return
	 */
	public synchronized static long getSessionID(long lastSessionId) {
		lastSessionId++;
		return lastSessionId;
	}

	/**
	 * 获取最新sraft事务ID，传入目前最新sraftTransactionId事务ID
	 * 
	 * @param newSraftTransactionId
	 * @return
	 */
	public synchronized static long getSraftTransactionId(long lastSraftTransactionId) {
		lastSraftTransactionId++;
		return lastSraftTransactionId;
	}

	/**
	 * 获取客户端最新事务ID，仅在一个客户端的进程期间唯一
	 * 
	 * @param lastClientTransactionId
	 * @return
	 */
	public synchronized static long getClientTransactionId(long lastClientTransactionId) {
		lastClientTransactionId++;
		return lastClientTransactionId;
	}
}
