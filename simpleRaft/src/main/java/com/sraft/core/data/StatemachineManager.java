package com.sraft.core.data;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sraft.core.log.ILogEntry;
import com.sraft.core.log.LogData;
import com.sraft.core.log.Snapshot;

public class StatemachineManager implements IStatement {
	private static Logger LOG = LoggerFactory.getLogger(StatemachineManager.class);
	private IAction iAction = null;
	private Statemachine statemachine = null;
	private ILogEntry iLogEntry = null;
	private long lastApplied = -1;
	private long lastCommitId = -1;
	// 客户端最后应用执行的操作

	public StatemachineManager(ILogEntry iLogEntry) {
		this.iLogEntry = iLogEntry;
		statemachine = new Statemachine();
		iAction = new ActionImpl();
		init();
	}

	private void init() {
		List<Snapshot> allSnapshotList = iLogEntry.getAllSnapshot();
		List<LogData> allLogDataList = null;
		try {
			allLogDataList = iLogEntry.getAllLogData();
		} catch (IOException e) {
			e.printStackTrace();
			LOG.error(e.getMessage(), e);
		}

		if (allSnapshotList != null && allSnapshotList.size() > 0) {
			for (Snapshot snapshot : allSnapshotList) {
				try {
					iAction.add(statemachine, new String(snapshot.getbKey(), "UTF-8"),
							new String(snapshot.getbValue(), "UTF-8"));
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
				iAction.add(statemachine, logData.getKey(), logData.getValue());
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

}
