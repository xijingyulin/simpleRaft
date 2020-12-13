package com.sraft.core.log;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sraft.common.FileHelper;
import com.sraft.common.StringHelper;
import com.sraft.core.Config;
import com.sraft.core.message.AppendLogEntryMsg;
import com.sraft.core.message.AppendSnapshotMsg;
import com.sraft.core.message.BaseLog;
import com.sraft.core.message.BaseSnapshot;

public abstract class ALogSnapImpl implements ILogSnap {
	private static Logger LOG = LoggerFactory.getLogger(ALogSnapImpl.class);

	protected ILogData iLogData = null;
	protected LogData lastLogData = null;
	protected long lastLogIndex = -1;
	protected long lastLogTerm = -1;

	protected ISnapshot iSnapshot = null;
	protected Snapshot lastSnapshot = null;
	protected long lastSnapIndex = -1;
	protected long lastSnapTerm = -1;

	protected String logDataDir;
	protected String snapshotPath;
	protected String logDataPath;
	protected static final String PREFIX_LOG_DATA = "log_";
	protected static final String PREFIX_SNAPSHOT = "snapshot_";
	protected ScheduledExecutorService schedularSnapshot = null;
	protected Lock lock = new ReentrantLock();

	/**
	 * 有些操作需要重启状态机；比如，
	 * 
	 * （1）遍历文件的一致性检查；在中间找到上一条日志
	 * 
	 * （2）日志不为空，但上一条索引却是快照最后一条日志，需要清空日志
	 * 
	 * （3）发送过来的是第一条日志，但快照和日志却不为空
	 * 
	 * （4）发送快照
	 * 
	 */
	protected volatile boolean isChanged = false;

	/**
	 * 每隔snapshotInterval小时，压缩一次日志，满足触发条件才能压缩
	 */
	protected int snapshotInterval = 10;
	/**
	 * 当日志行数达到compressTrigger
	 */
	protected int compressTrigger = 10000;
	/**
	 * 真正压缩行数
	 */
	protected int compressCount = 8000;

	public ALogSnapImpl(Config config) throws IOException {
		logDataDir = config.getLogDataDir();
		snapshotInterval = config.getSnapshotInterval();
		compressTrigger = config.getCompressTrigger();
		compressCount = config.getCompressCount();
		iLogData = new LogDataImpl();
		iSnapshot = new SnapshotImpl();
		schedularSnapshot = Executors.newSingleThreadScheduledExecutor();
	}

	public void init() throws IOException {
		File dataDir = new File(logDataDir);
		if (!dataDir.exists()) {
			return;
		}
		List<String> logDataFileList = new ArrayList<String>();
		List<String> snapshotFileList = new ArrayList<String>();
		File[] allFiles = dataDir.listFiles();
		for (File file : allFiles) {
			if (file.isFile()) {
				String fileName = file.getName();
				if (fileName.startsWith(PREFIX_LOG_DATA)) {
					logDataFileList.add(fileName);
				} else if (fileName.startsWith(PREFIX_SNAPSHOT)) {
					snapshotFileList.add(fileName);
				}
			}
		}
		if (logDataFileList.size() > 0) {
			Collections.sort(logDataFileList, new Comparator<String>() {
				@Override
				public int compare(String o1, String o2) {
					long sraftTransactionId1 = Long.parseLong(o1.substring(o1.lastIndexOf(PREFIX_LOG_DATA)));
					long sraftTransactionId2 = Long.parseLong(o2.substring(o2.lastIndexOf(PREFIX_LOG_DATA)));
					if (sraftTransactionId1 > sraftTransactionId2) {
						return -1;
					} else {
						return 1;
					}
				}
			});
			String lastLogDataFileName = logDataFileList.get(0);
			logDataPath = logDataDir + File.separator + lastLogDataFileName;
		}

		if (snapshotFileList.size() > 0) {
			Collections.sort(snapshotFileList, new Comparator<String>() {
				@Override
				public int compare(String o1, String o2) {
					long sraftTransactionId1 = Long.parseLong(o1.substring(o1.lastIndexOf(PREFIX_LOG_DATA)));
					long sraftTransactionId2 = Long.parseLong(o2.substring(o2.lastIndexOf(PREFIX_LOG_DATA)));
					if (sraftTransactionId1 > sraftTransactionId2) {
						return -1;
					} else {
						return 1;
					}
				}
			});
			String lastSnapshotFileName = snapshotFileList.get(0);
			snapshotPath = logDataDir + File.separator + lastSnapshotFileName;
		}

		if (StringHelper.checkIsNotNull(logDataPath)) {
			List<LogData> logDataList = iLogData.getAllLogData(logDataPath);
			lastLogData = logDataList.get(logDataList.size() - 1);
			lastLogIndex = lastLogData.getLogIndex();
			lastLogTerm = lastLogData.getLogTerm();
			logDataList.clear();
		}
		if (StringHelper.checkIsNotNull(snapshotPath)) {
			lastSnapshot = iSnapshot.getLastSnapshot(snapshotPath);
			lastSnapIndex = lastSnapshot.getLogIndex();
			lastSnapTerm = lastSnapshot.getLogTerm();
			// 只有快照，没有日志的情况下
			if (StringHelper.checkIsNotNull(logDataPath)) {
				lastLogIndex = lastSnapIndex;
				lastLogTerm = lastSnapTerm;
			}
		}
	}

	protected void updateLastLogData(LogData logData) {
		lastLogData = logData;
		lastLogIndex = logData.getLogIndex();
		lastLogTerm = logData.getLogTerm();
	}

	protected List<LogData> transMsg2LogData(AppendLogEntryMsg appendLogEntryMsg) throws UnsupportedEncodingException {
		List<LogData> logDataList = new ArrayList<LogData>();
		List<BaseLog> baseLogList = appendLogEntryMsg.getBaseLogList();
		for (BaseLog baseLog : baseLogList) {
			LogData logData = new LogData();
			logData.setLogIndex(baseLog.getLogIndex());
			logData.setLogTerm(baseLog.getLogTerm());
			logData.setLogType(baseLog.getLogType());
			logData.setLeaderId(baseLog.getLeaderId());
			logData.setClientSessionId(baseLog.getClientSessionId());
			logData.setClientTransactionId(baseLog.getClientTransactionId());
			logData.setSraftTransactionId(baseLog.getSraftTransactionId());
			logData.setCreateTime(baseLog.getCreateTime());
			logData.setUpdateTime(baseLog.getUpdateTime());
			logData.setKey(baseLog.getKey());
			logData.setValue(baseLog.getValue());
			iLogData.tranLogData2Store(logData);
			logDataList.add(logData);
		}
		return logDataList;
	}

	/**
	 * 
	 * 遍历整个日志检查一致性
	 * 
	 * @param appendLogEntryMsg
	 * @return
	 */
	protected LogData consistencyCheckAllLog(AppendLogEntryMsg appendLogEntryMsg) {
		LogData logData = null;
		if (lastLogData == null) {
			return logData;
		}
		long prevLogIndex = appendLogEntryMsg.getPrevLogIndex();
		long prevLogTerm = appendLogEntryMsg.getPrevLogTerm();
		try {
			logData = iLogData.getLogDataByIndex(logDataPath, prevLogIndex);
			if (logData != null && logData.getLogTerm() != prevLogTerm) {
				logData = null;
			}
		} catch (IOException e) {
			e.printStackTrace();
			logData = null;
			LOG.error("一致性检查失败,prevLogIndex={},prevLogTerm={}", prevLogIndex, prevLogTerm);
		}
		return logData;
	}

	/**
	 * 一般一致性检查，只检查最新日志索引和任期
	 * 
	 * @param appendLogEntryMsg
	 * @return
	 */
	protected boolean consistencyCheck(AppendLogEntryMsg appendLogEntryMsg) {
		boolean isConsistent = false;
		long prevLogIndex = appendLogEntryMsg.getPrevLogIndex();
		long prevLogTerm = appendLogEntryMsg.getPrevLogTerm();
		if (prevLogIndex == lastLogIndex && prevLogTerm == lastLogTerm) {
			isConsistent = true;
		} else {
			return isConsistent;
		}
		return isConsistent;
	}

	/**
	 * 一致性检查，是否和快照最后一条日志相同
	 * 
	 * 这种情况日志一般不为空，一致性检查成功的话，需要删除日志
	 * 
	 * @param appendLogEntryMsg
	 * @return
	 */
	protected boolean consistencyCheckSnapshot(AppendLogEntryMsg appendLogEntryMsg) {
		boolean isConsistent = false;
		long prevLogIndex = appendLogEntryMsg.getPrevLogIndex();
		long prevLogTerm = appendLogEntryMsg.getPrevLogTerm();
		if (prevLogIndex == lastSnapIndex && prevLogTerm == lastSnapTerm) {
			isConsistent = true;
		} else {
			return isConsistent;
		}
		return isConsistent;
	}

	protected void updateLastSnapshot(Snapshot snapshot) {
		lastSnapshot = snapshot;
		lastSnapIndex = snapshot.getLogIndex();
		lastSnapTerm = snapshot.getLogTerm();
	}

	/**
	 * 检查快照一致性
	 * 
	 * @param appendSnapshotMsg
	 * @return
	 */
	protected boolean checkSnapConsistency(AppendSnapshotMsg appendSnapshotMsg) {
		long prevSnapIndex = appendSnapshotMsg.getPrevSnapIndex();
		long prevSnapTerm = appendSnapshotMsg.getPrevSnapTerm();
		if (prevSnapIndex == lastSnapIndex && prevSnapTerm == lastSnapTerm) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 第一条日志，需要先清空跟随者快照和日志
	 * 
	 * @param appendLogEntryMsg
	 * @return
	 */
	protected boolean isFirstLog(AppendLogEntryMsg appendLogEntryMsg) {
		long prevLogIndex = appendLogEntryMsg.getPrevLogIndex();
		long prevLogTerm = appendLogEntryMsg.getPrevLogTerm();
		if (prevLogIndex == -1 && prevLogTerm == -1) {
			return true;
		} else {
			return false;
		}
	}

	protected List<Snapshot> transMsg2Snapshot(AppendSnapshotMsg appendSnapshotMsg) {
		List<Snapshot> snapshotList = new ArrayList<Snapshot>();
		List<BaseSnapshot> baseSnapshotList = appendSnapshotMsg.getBaseSnapshot();
		for (BaseSnapshot baseSnapshot : baseSnapshotList) {
			Snapshot snapshot = new Snapshot();
			snapshot.setLogIndex(baseSnapshot.getLogIndex());
			snapshot.setLogTerm(baseSnapshot.getLogTerm());
			snapshot.setbKey(baseSnapshot.getbKey());
			snapshot.setbValue(baseSnapshot.getbValue());
			iSnapshot.transSnapshot2Store(snapshot);
			snapshotList.add(snapshot);
		}
		return snapshotList;
	}

	protected void clear() {
		if (StringHelper.checkIsNotNull(logDataPath)) {
			FileHelper.delFile(logDataPath);
		}
		if (StringHelper.checkIsNotNull(snapshotPath)) {
			FileHelper.delFile(snapshotPath);
		}
		lastLogData = null;
		lastLogIndex = -1;
		lastLogTerm = -1;
		logDataPath = null;
		lastSnapshot = null;
		lastSnapIndex = -1;
		lastSnapTerm = -1;
		snapshotPath = null;
	}

	public void getLock() {
		lock.lock();
	}

	public void unLock() {
		lock.unlock();
	}

	public ILogData getiLogData() {
		return iLogData;
	}

	public void setiLogData(ILogData iLogData) {
		this.iLogData = iLogData;
	}

	public boolean isNullServer() {
		if (lastLogData == null && lastSnapshot == null) {
			return true;
		} else {
			return false;
		}
	}
}
