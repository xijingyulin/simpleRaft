package com.sraft.core.data;

public interface IStatement {

	long getLastCommitId();
	
	void restart();
	
	boolean commit(long leaderCommit);
}
