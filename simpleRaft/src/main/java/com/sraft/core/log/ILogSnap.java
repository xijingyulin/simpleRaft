package com.sraft.core.log;

import java.io.IOException;
import java.util.List;

import com.sraft.core.message.AppendLogEntryMsg;
import com.sraft.core.message.AppendSnapshotMsg;
import com.sraft.enums.EnumAppendLogResult;
import com.sraft.enums.EnumAppendSnapshotResult;

public interface ILogSnap {

	/**
	 * 读取现有所有日志
	 * 
	 * @return
	 * @throws IOException
	 */
	List<LogData> getAllLogData() throws IOException;

	/**
	 * 读取快照
	 * 
	 * @return
	 */
	List<Snapshot> getAllSnapshot();

	/**
	 * 
	 * 1.【空日志，非空日志读请求】，只需检查上一条索引；不需遍历整个文件
	 * 
	 * 2.【非空日志，非空日志读请求】，需要检查：上一条索引，遍历整个文件，快照的最后一条索引
	 * 
	 * 3.通过一致性检查，【空日志，非空日志读请求】直接返回成功
	 * 
	 * 4.通过一致性检查，【非空日志写请求】写文件
	 * 
	 * （1）上一条索引：直接再在末尾追加
	 * 
	 * （2）步骤（1）检查失败，遍历整个文件：在对应的偏移位置插入；需要重启状态机
	 * 
	 * （3）步骤（1）（2）检查失败，快照的最后一条索引，删除现有日志文件；需要重启状态机
	 * 
	 * （4）日志为空的话，都需要先新建文件
	 * 
	 * （5）如果追加日志是第一条日志，直接清空跟随者快照和日志；需要重启状态机
	 * 
	 * 5.没有通过一致性检查，还需要检查是否是空服务器，空服务器需要先发送快照，再发送日志
	 * 
	 * @param appendLogEntryMsg
	 * @return
	 * @throws IOException
	 */
	EnumAppendLogResult appendLogEntry(AppendLogEntryMsg appendLogEntryMsg);

	/**
	 * 领导者发送快照只有两种情况
	 * 
	 * 1.新服务器
	 * 
	 * 2.跟随者日志全都一致性检查失败
	 * 
	 * （1）发送第一条快照时（第一条日志，prevIndex和prevTerm均为-1），清空跟随者所有日志
	 * 
	 * （2）后续的快照只需一致性检查（跟追加日志的一致性检查一样，但只检查一次prevIndex和prevTerm），凡是检查失败，就从头重新发送
	 * 
	 * 追加成功返回 【成功】，否则【失败】，凡是【失败】领导者都从头发送快照
	 * 
	 * 返回值见 EnumReplyAppendSnapshot
	 * 
	 * @param appendSnapshotMsg
	 * @return
	 */
	EnumAppendSnapshotResult appendSnapshot(AppendSnapshotMsg appendSnapshotMsg);

	/**
	 * 添加生成快照的定时任务
	 * 
	 * @return
	 */
	boolean addSnapshotJob();

	/**
	 * 执行生成快照任务
	 * 
	 * @return
	 */
	boolean doGenSnapshot();

	long getLastLogIndex();

	long getLastLogTerm();

	LogData getLogDataByIndex(long logIndex);

	long getLastSnapIndex();

	long getLastSnapTerm();

	boolean isNeedRebootStatemachine();

	List<LogData> getLogDataByCount(long beginLogIndex, int logDataCount);

	List<Snapshot> getSnapshotList(long beginSnapshotIndex, int count);
}
