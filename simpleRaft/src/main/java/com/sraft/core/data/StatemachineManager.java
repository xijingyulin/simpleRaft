package com.sraft.core.data;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sraft.core.log.ILogSnap;
import com.sraft.core.log.LogData;
import com.sraft.core.log.Snapshot;

public class StatemachineManager implements IStatement {
	private static Logger LOG = LoggerFactory.getLogger(StatemachineManager.class);
	private IAction iAction = null;
	private Statemachine statemachine = null;
	private ILogSnap iLogSnap = null;
	private volatile long lastApplied = -1;
	private volatile long lastCommitId = -1;
	// 客户端最后应用执行的操作

	public StatemachineManager(ILogSnap iLogSnap) {
		this.iLogSnap = iLogSnap;
		statemachine = new Statemachine();
		iAction = new ActionImpl();
		restart();
	}

	public void restart() {
		iAction.clear(statemachine);

		List<Snapshot> allSnapshotList = iLogSnap.getAllSnapshot();
		List<LogData> allLogDataList = null;
		try {
			allLogDataList = iLogSnap.getAllLogData();
		} catch (IOException e) {
			e.printStackTrace();
			LOG.error(e.getMessage(), e);
		}

		if (allSnapshotList != null && allSnapshotList.size() > 0) {
			for (int i = 0; i < allSnapshotList.size(); i++) {
				Snapshot snapshot = allSnapshotList.get(i);
				try {
					// 最后一条快照是记录最后的任期和索引
					if (i < allSnapshotList.size() - 1) {
						iAction.put(statemachine, new String(snapshot.getbKey(), "UTF-8"),
								new String(snapshot.getbValue(), "UTF-8"));
					}
					updateApply(snapshot.getLogIndex());
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
					LOG.error(e.getMessage(), e);
				}
			}
			allSnapshotList.clear();
		}
		if (allLogDataList != null && allLogDataList.size() > 0) {
			for (LogData logData : allLogDataList) {
				int logType = logData.getLogType();
				
				if (logType == LogData.LOG_PUT) {
					iAction.put(statemachine, logData.getKey(), logData.getValue());
				} else if (logType == LogData.LOG_DEL) {
					iAction.del(statemachine, logData.getKey());
				} else if (logType == LogData.LOG_UPDATE) {
					iAction.update(statemachine, logData.getKey(), logData.getValue());
				}
				updateApply(logData.getLogIndex());
			}
			allLogDataList.clear();
		}
	}

	private void updateApply(long logIndex) {
		this.lastApplied = logIndex;
		this.lastCommitId = logIndex;
	}

	@Override
	public long getLastCommitId() {
		return lastCommitId;
	}

	@Override
	public boolean commit(long leaderCommit) {
		return false;
	}

}
