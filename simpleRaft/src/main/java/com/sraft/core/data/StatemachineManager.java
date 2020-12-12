package com.sraft.core.data;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sraft.common.StringHelper;
import com.sraft.core.log.ILogSnap;
import com.sraft.core.log.LogData;
import com.sraft.core.log.Snapshot;
import com.sraft.core.message.BaseLog;
import com.sraft.core.message.BaseSnapshot;

public class StatemachineManager implements IStatement {
	private static Logger LOG = LoggerFactory.getLogger(StatemachineManager.class);
	private IAction iAction = null;
	private Statemachine statemachine = null;
	private ILogSnap iLogSnap = null;
	private volatile long lastApplied = -1;
	private volatile long lastCommitId = -1;
	/**
	 * 待提交队列
	 */
	private Map<Long, BaseLog> commitMap = new ConcurrentSkipListMap<>();

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
					String key = new String(snapshot.getbKey(), "UTF-8");
					String value = new String(snapshot.getbValue(), "UTF-8");
					// 最后一条快照是记录最后的任期和索引;key和value都为空
					if (StringHelper.checkIsNotNull(key)) {
						iAction.put(statemachine, key, value);
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
				commit(logData);
			}
			allLogDataList.clear();
		}
	}

	private void commit(BaseLog baseLog) {
		int logType = baseLog.getLogType();
		if (logType == LogData.LOG_PUT) {
			iAction.put(statemachine, baseLog.getKey(), baseLog.getValue());
		} else if (logType == LogData.LOG_REMOVE) {
			iAction.remove(statemachine, baseLog.getKey());
		} else if (logType == LogData.LOG_UPDATE) {
			iAction.update(statemachine, baseLog.getKey(), baseLog.getValue());
		}
		updateApply(baseLog.getLogIndex());
	}

	private void commit(BaseSnapshot baseSnapshot) {
		try {
			String key = new String(baseSnapshot.getbKey(), "UTF-8");
			String value = new String(baseSnapshot.getbValue(), "UTF-8");
			if (StringHelper.checkIsNotNull(key)) {
				iAction.put(statemachine, key, value);
			}
			updateApply(baseSnapshot.getLogIndex());
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
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
		long commitId = leaderCommit;
		if (iLogSnap.getLastLogIndex() < leaderCommit) {
			commitId = iLogSnap.getLastLogIndex();
		}

		Iterator<Long> it = commitMap.keySet().iterator();
		while (it.hasNext()) {
			Long logIndex = it.next();
			if (logIndex <= commitId) {
				BaseLog committingBaseLog = commitMap.get(logIndex);
				commit(committingBaseLog);
				it.remove();
			}
		}
		return true;
	}

	@Override
	public synchronized void putLogData(List<BaseLog> baseLogList) {
		for (BaseLog baseLog : baseLogList) {
			commitMap.put(baseLog.getLogIndex(), baseLog);
		}
	}

	@Override
	public boolean commit(List<BaseLog> baseLogList) {
		for (BaseLog baseLog : baseLogList) {
			commit(baseLog);
		}
		return true;
	}

	@Override
	public boolean commitSnapshot(List<BaseSnapshot> baseSnapshotList) {
		for (BaseSnapshot snapshot : baseSnapshotList) {
			commit(snapshot);
		}
		return false;
	}

	@Override
	public boolean commit() {
		Iterator<Long> it = commitMap.keySet().iterator();
		while (it.hasNext()) {
			Long logIndex = it.next();
			BaseLog committingBaseLog = commitMap.get(logIndex);
			commit(committingBaseLog);
			it.remove();
		}
		return true;
	}

	@Override
	public String getValue(String key) {
		// TODO Auto-generated method stub
		return iAction.get(statemachine, key);
	}

	@Override
	public boolean isNeedCommit() {
		return (!commitMap.isEmpty());
	}

}
