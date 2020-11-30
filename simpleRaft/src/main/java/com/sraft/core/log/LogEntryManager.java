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
import com.sraft.enums.EnumReplyAppendLog;
import com.sraft.enums.EnumReplyAppendSnapshot;

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

	@Override
	public int appendLogEntry(AppendLogEntryMsg appendLogEntryMsg) {
		EnumReplyAppendLog appendResultEnum = EnumReplyAppendLog.LOG_APPEND_SUCCESS;
		LogData prevLogData = null;
		boolean isConsistency = false;
		if (consistencyCheck(appendLogEntryMsg)) {
			isConsistency = true;
		} else {
			prevLogData = consistencyCheckAllLog(appendLogEntryMsg);
			if (prevLogData != null) {
				isConsistency = true;
			}
		}
		if (!isConsistency) {
			//空服务器
			if (lastLogData == null) {
				appendResultEnum = EnumReplyAppendLog.LOG_NULL;
			} else {
				appendResultEnum = EnumReplyAppendLog.LOG_CHECK_FALSE;
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
					appendResultEnum = EnumReplyAppendLog.LOG_CHECK_FALSE;
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
					appendResultEnum = EnumReplyAppendLog.LOG_APPEND_SUCCESS;
				} else {
					appendResultEnum = EnumReplyAppendLog.LOG_CHECK_FALSE;
				}
			} catch (IOException e) {
				e.printStackTrace();
				LOG.error(e.getMessage(), e);
				appendResultEnum = EnumReplyAppendLog.LOG_CHECK_FALSE;
			}
		}
		return appendResultEnum.getValue();
	}

	@Override
	public int appendSnapshot(AppendSnapshotMsg appendSnapshotMsg) {
		EnumReplyAppendSnapshot appendSnapshotResult = EnumReplyAppendSnapshot.SNAPSHOT_APPEND_FALSE;
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
				appendSnapshotResult = EnumReplyAppendSnapshot.SNAPSHOT_APPEND_FALSE;
			}
		}
		boolean isConsistency = checkSnapConsistency(appendSnapshotMsg);
		if (isConsistency) {
			List<Snapshot> snapshotList = transMsg2Snapshot(appendSnapshotMsg);
			try {
				boolean isSuccess = iSnapshot.appendSnapshot(snapshotPath, snapshotList);
				if (isSuccess) {
					appendSnapshotResult = EnumReplyAppendSnapshot.SNAPSHOT_APPEND_TRUE;
					updateLastSnapshot(snapshotList.get(snapshotList.size() - 1));
				} else {
					appendSnapshotResult = EnumReplyAppendSnapshot.SNAPSHOT_APPEND_FALSE;
				}
			} catch (Exception e) {
				e.printStackTrace();
				appendSnapshotResult = EnumReplyAppendSnapshot.SNAPSHOT_APPEND_FALSE;
				LOG.error(e.getMessage(), e);
				LOG.error("安装快照失败,snapshotPath:{}", snapshotPath);
			}
		} else {
			appendSnapshotResult = EnumReplyAppendSnapshot.SNAPSHOT_APPEND_FALSE;
		}
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

}
