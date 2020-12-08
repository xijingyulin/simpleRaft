package com.sraft.core.log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sraft.Config;
import com.sraft.common.FileHelper;
import com.sraft.common.StringHelper;
import com.sraft.core.message.AppendLogEntryMsg;
import com.sraft.core.message.AppendSnapshotMsg;
import com.sraft.enums.EnumAppendLogResult;
import com.sraft.enums.EnumAppendSnapshotResult;

public class LogEntryManager extends ALogEntryImpl {

	private static Logger LOG = LoggerFactory.getLogger(LogEntryManager.class);

	public LogEntryManager(Config config) throws IOException {
		super(config);
		init();
	}

	@Override
	public List<LogData> getAllLogData() throws IOException {
		List<LogData> allLogDataList = new ArrayList<LogData>();
		if (StringHelper.checkIsNotNull(logDataPath)) {
			allLogDataList = iLogData.getAllLogData(logDataPath);
		}
		return allLogDataList;
	}

	@Override
	public List<Snapshot> getAllSnapshot() {
		List<Snapshot> allSnapshotList = new ArrayList<Snapshot>();
		if (StringHelper.checkIsNotNull(snapshotPath)) {
			allSnapshotList = iSnapshot.getAllSnapshot(snapshotPath);
		}
		return allSnapshotList;
	}

	/*
	 * 1.空日志，只需检查上一条索引；不需遍历整个文件
	 * 
	 * 2.非空日志，需要检查：上一条索引，遍历整个文件，快照的最后一条索引
	 * 
	 * 3.通过一致性检查，空日志直接返回成功
	 * 
	 * 4.通过一致性检查，非空日志写文件
	 * 
	 * （1）上一条索引：直接再在末尾追加
	 * 
	 * （2）步骤（1）检查失败，遍历整个文件：在对应的偏移位置插入；需要重启状态机
	 * 
	 * （3）步骤（1）（2）检查失败，快照的最后一条索引，删除现有日志文件；需要重启状态机
	 * 
	 * （4）日志为空的话，都需要先新建文件
	 * 
	 * 5.没有通过一致性检查，还需要检查是否是空服务器，空服务器需要先发送快照，再发送日志
	 */
	@Override
	public int appendLogEntry(AppendLogEntryMsg appendLogEntryMsg) {
		getLock();
		EnumAppendLogResult appendResultEnum = EnumAppendLogResult.LOG_APPEND_SUCCESS;
		LogData prevLogData = null;
		boolean isConsistency = false;

		int appendType = appendLogEntryMsg.getAppendType();
		// 空日志只需一致性检查
		if (appendType == AppendLogEntryMsg.TYPE_APPEND_NULL) {
			if (consistencyCheck(appendLogEntryMsg)) {
				appendResultEnum = EnumAppendLogResult.LOG_APPEND_SUCCESS;
			} else if (isNullServer()) {
				appendResultEnum = EnumAppendLogResult.LOG_NULL;
			} else {
				appendResultEnum = EnumAppendLogResult.LOG_CHECK_FALSE;
			}
			return appendResultEnum.getValue();
		}

		if (consistencyCheck(appendLogEntryMsg)) {
			isConsistency = true;
		} else if (consistencyCheckSnapshot(appendLogEntryMsg)) {
			isConsistency = true;
			if (StringHelper.checkIsNotNull(logDataPath)) {
				FileHelper.delFile(logDataPath);
				lastLogData = null;
				logDataPath = null;
				lastLogIndex = lastSnapIndex;
				lastLogTerm = lastSnapTerm;
				isChanged = true;
			}
		} else {
			prevLogData = consistencyCheckAllLog(appendLogEntryMsg);
			if (prevLogData != null) {
				isConsistency = true;
				isChanged = true;
			}
		}
		if (!isConsistency) {
			//空服务器
			if (lastLogData == null) {
				appendResultEnum = EnumAppendLogResult.LOG_NULL;
			} else {
				appendResultEnum = EnumAppendLogResult.LOG_CHECK_FALSE;
			}
		} else {
			// 没有日志，需要新建日志文件,日志名=目录+前缀+第一条日志sraft事务ID
			if (lastLogData == null) {
				logDataPath = logDataDir + File.separator + PREFIX_LOG_DATA
						+ appendLogEntryMsg.getBaseLogList().get(0).getSraftTransactionId() + ".log";
				try {
					FileHelper.createNewEmptyFile(logDataPath);
				} catch (IOException e) {
					e.printStackTrace();
					LOG.error("追加日志，创建日志文件失败:{}", logDataPath);
					LOG.error(e.getMessage(), e);
					appendResultEnum = EnumAppendLogResult.LOG_CHECK_FALSE;
				}
			}
			try {
				List<LogData> logDataList = transMsg2LogData(appendLogEntryMsg);
				boolean isAppendSuccess = false;
				if (prevLogData != null) {
					isAppendSuccess = iLogData.append(logDataPath, prevLogData.getOffset(), logDataList);
				} else {
					isAppendSuccess = iLogData.append(logDataPath, logDataList);
				}
				if (isAppendSuccess) {
					updateLastLogData(logDataList.get(logDataList.size() - 1));
					appendResultEnum = EnumAppendLogResult.LOG_APPEND_SUCCESS;
				} else {
					appendResultEnum = EnumAppendLogResult.LOG_CHECK_FALSE;
				}
			} catch (IOException e) {
				e.printStackTrace();
				LOG.error(e.getMessage(), e);
				appendResultEnum = EnumAppendLogResult.LOG_CHECK_FALSE;
			}
		}
		unLock();
		return appendResultEnum.getValue();
	}

	@Override
	public int appendSnapshot(AppendSnapshotMsg appendSnapshotMsg) {
		getLock();
		EnumAppendSnapshotResult appendSnapshotResult = EnumAppendSnapshotResult.SNAPSHOT_APPEND_FALSE;
		long prevSnapIndex = appendSnapshotMsg.getPrevSnapIndex();
		long prevSnapTerm = appendSnapshotMsg.getPrevSnapTerm();
		// 第一个快照，需要清空所有日志信息
		if (prevSnapIndex == -1 && prevSnapTerm == -1) {
			clear();
			snapshotPath = logDataDir + File.separator + PREFIX_SNAPSHOT
					+ appendSnapshotMsg.getBaseSnapshot().get(0).getLogIndex() + ".snapshot";
			try {
				FileHelper.createNewEmptyFile(snapshotPath);
			} catch (IOException e) {
				e.printStackTrace();
				appendSnapshotResult = EnumAppendSnapshotResult.SNAPSHOT_APPEND_FALSE;
			}
		}
		boolean isConsistency = checkSnapConsistency(appendSnapshotMsg);
		if (isConsistency) {
			List<Snapshot> snapshotList = transMsg2Snapshot(appendSnapshotMsg);
			try {
				boolean isSuccess = iSnapshot.appendSnapshot(snapshotPath, snapshotList);
				if (isSuccess) {
					appendSnapshotResult = EnumAppendSnapshotResult.SNAPSHOT_APPEND_TRUE;
					updateLastSnapshot(snapshotList.get(snapshotList.size() - 1));
				} else {
					appendSnapshotResult = EnumAppendSnapshotResult.SNAPSHOT_APPEND_FALSE;
				}
			} catch (Exception e) {
				e.printStackTrace();
				appendSnapshotResult = EnumAppendSnapshotResult.SNAPSHOT_APPEND_FALSE;
				LOG.error(e.getMessage(), e);
				LOG.error("安装快照失败,snapshotPath:{}", snapshotPath);
			}
		} else {
			appendSnapshotResult = EnumAppendSnapshotResult.SNAPSHOT_APPEND_FALSE;
		}
		unLock();
		return appendSnapshotResult.getValue();
	}

	@Override
	public boolean addSnapshotJob() {
		schedularSnapshot.scheduleAtFixedRate(new CompressLogJob(this), snapshotInterval, snapshotInterval,
				TimeUnit.HOURS);
		return false;
	}

	@Override
	public boolean doGenSnapshot() {
		boolean isSuccess = false;
		//读取日志行数
		//判断是否满足压缩条件
		//进行压缩生成快照
		//更新状态
		try {
			getLock();
			int logDataCount = iLogData.getLogDataCount(logDataPath);
			if (logDataCount > compressTrigger) {
				String[] pathArr = iSnapshot.genSnapshot(iLogData, logDataDir, snapshotPath, logDataPath,
						compressCount);
				if (pathArr != null) {
					snapshotPath = pathArr[0];
					logDataPath = pathArr[1];
					lastSnapshot = iSnapshot.getLastSnapshot(snapshotPath);
					lastSnapIndex = lastSnapshot.getLogIndex();
					lastSnapTerm = lastSnapshot.getLogTerm();
					isSuccess = true;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			isSuccess = false;
			LOG.error(e.getMessage(), e);
			LOG.error("生成快照失败！");
		} finally {
			unLock();
		}
		return isSuccess;
	}

	@Override
	public long getLastLogIndex() {

		return lastLogIndex;
	}

	@Override
	public long getLastLogTerm() {
		return lastLogTerm;
	}

	@Override
	public LogData getLogDataByIndex(long logIndex) {
		LogData logData = null;
		try {
			logData = iLogData.getLogDataByIndex(logDataPath, logIndex);
			return logData;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public long getLastSnapIndex() {
		return lastSnapIndex;
	}

	@Override
	public long getLastSnapTerm() {
		return lastSnapTerm;
	}

	@Override
	public boolean isNeedRebootStatemachine() {
		if (isChanged == true) {
			isChanged = false;
			return true;
		} else {
			return false;
		}
	}

}
