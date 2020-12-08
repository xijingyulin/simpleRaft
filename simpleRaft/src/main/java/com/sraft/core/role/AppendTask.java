package com.sraft.core.role;

import java.util.concurrent.atomic.AtomicInteger;

import com.sraft.core.message.BaseLog;

public class AppendTask {

	private BaseLog baseLog;
	/**
	 * 总追加数
	 */
	private int allAppendNum;
	/**
	 * 追加日志成功数
	 */
	private AtomicInteger successNum = new AtomicInteger(0);
	/**
	 * 追加日志失败数
	 */
	private AtomicInteger failNum = new AtomicInteger(0);

	private Object result;

	public AppendTask(BaseLog baseLog) {
		this.baseLog = baseLog;
	}

	public BaseLog getBaseLog() {
		return baseLog;
	}

	public void setBaseLog(BaseLog baseLog) {
		this.baseLog = baseLog;
	}

	public void increSuccessNum() {
		this.successNum.incrementAndGet();
	}

	public void increFailNum() {
		this.failNum.incrementAndGet();
	}

	public void setAllAppendNum(int allAppendNum) {
		this.allAppendNum = allAppendNum;
	}

	public boolean isOverHalfSuccess() {
		if (successNum.get() > allAppendNum / 2) {
			return true;
		} else {
			return false;
		}
	}

	public boolean isOverHalfFail() {
		if (failNum.get() > allAppendNum / 2) {
			return true;
		} else {
			return false;
		}
	}

	public Object getResult() {
		return result;
	}

	public void setResult(Object result) {
		this.result = result;
	}
}
