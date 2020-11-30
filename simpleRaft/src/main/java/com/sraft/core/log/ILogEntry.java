package com.sraft.core.log;

import java.io.IOException;
import java.util.List;

import com.sraft.core.message.AppendLogEntryMsg;
import com.sraft.core.message.AppendSnapshotMsg;

public interface ILogEntry {

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
	 * 追加日志有两种
	 * 
	 * 1.空日志，与非空日志的唯一区别就是，不写到日志里，也不执行到状态机
	 * 
	 * 2.非空日志
	 * 
	 * 返回的状态只有三种，具体看枚举EnumReplyAppendLog
	 * 
	 * （1）首先，一致性检查，先检查最新索引和任期，符合则返回【追加成功】；
	 * 
	 * （2）如果最新索引和任期不符合，则遍历日志，不遍历快照；都不符合再返回【一致性检查失败】
	 * 
	 * （3）如果为空服务器，且追加的不是第一条日志；则返回【空服务器】
	 * 
	 * 
	 * @param appendLogEntryMsg
	 * @return
	 */
	int appendLogEntry(AppendLogEntryMsg appendLogEntryMsg) throws IOException;

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
	int appendSnapshot(AppendSnapshotMsg appendSnapshotMsg);

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
}
