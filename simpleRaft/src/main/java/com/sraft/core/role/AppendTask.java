package com.sraft.core.role;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.sraft.core.message.BaseLog;

public class AppendTask {

	/**
	 * 任务ID
	 */
	private long taskId;

	private List<BaseLog> baseLogList;
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

	public AppendTask(List<BaseLog> baseLogList) {
		this.baseLogList = baseLogList;
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

	public long getTaskId() {
		return taskId;
	}

	public void setTaskId(long taskId) {
		this.taskId = taskId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("taskId:");
		builder.append(taskId);
		builder.append(",baseLogList:");
		builder.append(baseLogList);
		builder.append(",allAppendNum:");
		builder.append(allAppendNum);
		builder.append(",successNum:");
		builder.append(successNum);
		builder.append(",failNum:");
		builder.append(failNum);
		return builder.toString();
	}

	public List<BaseLog> getBaseLogList() {
		return baseLogList;
	}

	public void setBaseLogList(List<BaseLog> baseLogList) {
		this.baseLogList = baseLogList;
	}
}
