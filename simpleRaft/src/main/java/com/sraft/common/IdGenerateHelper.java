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

	private static long sessionId;

	/**
	 * 初始化会话ID
	 * 
	 * @param id
	 *            机器ID
	 */
	public static void initializeNextSession(long id) {
		long nextSid = 0;
		nextSid = (System.currentTimeMillis() << 24) >>> 8;
		nextSid = nextSid | (id << 56);
		sessionId = nextSid;
	}

	/**
	 * 获取最新事务ID
	 * 
	 * @param newSraftTransactionId
	 * @return
	 */
	public synchronized static long getNextSessionId() {
		sessionId++;
		return sessionId;
	}

}
