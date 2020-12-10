package com.sraft.core.log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

public interface ISnapshot {

	/**
	 * 计算字段长度，转为可存储的对象
	 * 
	 * @param snapshot
	 */
	void transSnapshot2Store(Snapshot snapshot);

	/**
	 * 读取最后一条快照
	 * 
	 * @param snapshotPath
	 * @return
	 */
	Snapshot getLastSnapshot(String snapshotPath);

	/**
	 * 读取所有快照
	 * 
	 * @param snapshotPath
	 * @return
	 */
	List<Snapshot> getAllSnapshot(String snapshotPath);

	/**
	 * 从指定位置开始读取指定数量的快照
	 * 
	 * @param snapshotPath
	 * @param logIndex
	 * @param count
	 * @return
	 * @throws Exception
	 */
	List<Snapshot> getSnapshotList(String snapshotPath, long beginSnapshotIndex, int count) throws Exception;

	/**
	 * 生成快照，最后一条快照是记录最后的任期和索引
	 * 
	 * @param iLogData
	 * @param logDataDir
	 * @param oldSnapshotPath
	 * @param oldLogDataPath
	 * @param compressCount
	 * @return
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 */
	String[] genSnapshot(ILogData iLogData, String logDataDir, String oldSnapshotPath, String oldLogDataPath,
			int compressCount) throws UnsupportedEncodingException, IOException;

	/**
	 * 追加快照
	 * 
	 * @param snapshotPath
	 * @param snapshotList
	 * @return
	 * @throws Exception
	 */
	boolean appendSnapshot(String snapshotPath, List<Snapshot> snapshotList) throws Exception;
}
