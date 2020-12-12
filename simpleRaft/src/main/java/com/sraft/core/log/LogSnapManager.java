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
import com.sraft.core.message.BaseLog;
import com.sraft.enums.EnumAppendLogResult;
import com.sraft.enums.EnumAppendSnapshotResult;

public class LogSnapManager extends ALogSnapImpl {

	private static Logger LOG = LoggerFactory.getLogger(LogSnapManager.class);

	public LogSnapManager(Config config) throws IOException {
		super(config);
		init();
	}

	@Override
	public List<LogData> getAllLogData() throws IOException {
		getLock();
		List<LogData> allLogDataList = new ArrayList<LogData>();
		try {
			if (StringHelper.checkIsNotNull(logDataPath)) {
				allLogDataList = iLogData.getAllLogData(logDataPath);
			}
		} catch (IOException e) {
			throw new IOException(e);
		} finally {
			unLock();
		}
		return allLogDataList;
	}

	@Override
	public List<Snapshot> getAllSnapshot() {
		getLock();
		List<Snapshot> allSnapshotList = new ArrayList<Snapshot>();
		try {
			if (StringHelper.checkIsNotNull(snapshotPath)) {
				allSnapshotList = iSnapshot.getAllSnapshot(snapshotPath);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		} finally {
			unLock();
		}
		return allSnapshotList;
	}

	@Override
	public EnumAppendLogResult appendLogEntry(AppendLogEntryMsg appendLogEntryMsg) {
		getLock();
		EnumAppendLogResult appendResultEnum = EnumAppendLogResult.LOG_CHECK_FALSE;
		try {
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
				return appendResultEnum;
			} else {
				BaseLog baseLog = appendLogEntryMsg.getBaseLogList().get(0);
				//如果是读请求，同样只需一致性检查
				if (baseLog.getLogType() == LogData.LOG_GET) {
					if (consistencyCheck(appendLogEntryMsg)) {
						appendResultEnum = EnumAppendLogResult.LOG_APPEND_SUCCESS;
					} else if (isNullServer()) {
						appendResultEnum = EnumAppendLogResult.LOG_NULL;
					} else {
						appendResultEnum = EnumAppendLogResult.LOG_CHECK_FALSE;
					}
					return appendResultEnum;
				}
			}

			if (isFirstLog(appendLogEntryMsg)) {
				// 追加的是第一条日志，但本地却已经有其它日志了，所以需要清空
				if (StringHelper.checkIsNotNull(logDataPath) || StringHelper.checkIsNotNull(snapshotPath)) {
					isChanged = true;
				}
				clear();
				isConsistency = true;
			} else if (consistencyCheck(appendLogEntryMsg)) {
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
						isChanged = true;
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
		} catch (Throwable e) {
			e.printStackTrace();
			LOG.error(e.getMessage(), e);
		} finally {
			unLock();
		}
		return appendResultEnum;
	}

	@Override
	public EnumAppendSnapshotResult appendSnapshot(AppendSnapshotMsg appendSnapshotMsg) {
		getLock();
		EnumAppendSnapshotResult appendSnapshotResult = EnumAppendSnapshotResult.SNAPSHOT_APPEND_FALSE;
		try {
			long prevSnapIndex = appendSnapshotMsg.getPrevSnapIndex();
			long prevSnapTerm = appendSnapshotMsg.getPrevSnapTerm();
			// 第一个快照，需要清空所有日志信息
			if (prevSnapIndex == -1 && prevSnapTerm == -1) {
				isChanged = true;
				clear();
				snapshotPath = logDataDir + File.separator + PREFIX_SNAPSHOT
						+ appendSnapshotMsg.getBaseSnapshot().get(0).getLogIndex() + ".snapshot";
				try {
					FileHelper.createNewEmptyFile(snapshotPath);
				} catch (IOException e) {
					e.printStackTrace();
					appendSnapshotResult = EnumAppendSnapshotResult.SNAPSHOT_APPEND_FALSE;
				}
			} else {
				if (!StringHelper.checkIsNotNull(snapshotPath)) {
					LOG.error("这不是首条快照,但快照路径却为空");
				}
			}
			boolean isConsistency = checkSnapConsistency(appendSnapshotMsg);
			if (isConsistency) {
				List<Snapshot> snapshotList = transMsg2Snapshot(appendSnapshotMsg);
				try {
					boolean isSuccess = iSnapshot.appendSnapshot(snapshotPath, snapshotList);
					if (isSuccess) {
						isChanged = true;
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
		} catch (Throwable e) {
			e.printStackTrace();
			LOG.error(e.getMessage(), e);
		} finally {
			unLock();
		}
		return appendSnapshotResult;
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
		getLock();
		//读取日志行数
		//判断是否满足压缩条件
		//进行压缩生成快照
		//更新状态
		try {
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
		getLock();
		LogData logData = null;
		try {
			logData = iLogData.getLogDataByIndex(logDataPath, logIndex);
		} catch (IOException e) {
			e.printStackTrace();
			LOG.error(e.getMessage(), e);
		} finally {
			unLock();
		}
		return logData;
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
		getLock();
		boolean result = false;
		try {
			if (isChanged == true) {
				isChanged = false;
				result = true;
			}
		} finally {
			unLock();
		}
		return result;
	}

	@Override
	public List<LogData> getLogDataByCount(long beginLogIndex, int logDataCount) {
		getLock();
		List<LogData> logDataList = new ArrayList<LogData>();
		try {
			logDataList = iLogData.getLogDataByCount(logDataPath, beginLogIndex, logDataCount);
		} catch (IOException e) {
			e.printStackTrace();
			LOG.error(e.getMessage(), e);
		} finally {
			unLock();
		}
		return logDataList;
	}

	@Override
	public List<Snapshot> getSnapshotList(long beginSnapshotIndex, int count) {
		getLock();
		List<Snapshot> snapshotList = new ArrayList<Snapshot>();
		try {
			if (StringHelper.checkIsNotNull(snapshotPath)) {
				snapshotList = iSnapshot.getSnapshotList(snapshotPath, beginSnapshotIndex, count);
			}
		} catch (Exception e) {
			e.printStackTrace();
			LOG.error(e.getMessage(), e);
		} finally {
			unLock();
		}
		return snapshotList;
	}

}
