package com.sraft.core.data;

import java.util.List;

import com.sraft.core.message.BaseLog;
import com.sraft.core.message.BaseSnapshot;

public interface IStatement {

	long getLastCommitId();

	void restart();

	/**
	 * 领导者专用，领导者提交日志，不需要根据leaderCommit，可以直接将所有未提交的日志进行提交
	 * 
	 * @return
	 */
	boolean commit();

	boolean commit(long leaderCommit);

	boolean commit(List<BaseLog> baseLogList);

	boolean commitSnapshot(List<BaseSnapshot> baseSnapshotList);

	boolean isNeedCommit();

	/**
	 * 将待提交的日志放到队列中
	 * 
	 * GET日志不用提交
	 * 
	 */
	void putLogData(List<BaseLog> baseLogList);

	String getValue(String key);
}
