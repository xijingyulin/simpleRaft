package com.sraft.core.log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

public interface ILogData {

	/**
	 * 将logData转成可存入文件的数据
	 * 
	 * @param logData
	 * @throws UnsupportedEncodingException
	 */
	void tranLogData2Store(LogData logData) throws UnsupportedEncodingException;

	/**
	 * 在末尾追加
	 * 
	 * @throws IOException
	 * 
	 */
	boolean append(String logDataPath, List<LogData> logDataList) throws IOException;

	/**
	 * 根据索引查找日志
	 * 
	 * @throws IOException
	 * 
	 */
	LogData getLogDataByIndex(String logDataPath, long logIndex) throws IOException;

	/**
	 * 
	 * 在指定位置offset后面追加日志，清除后面的所有内容
	 * 
	 * 新建临时文件，将指定位置的前的数据，写入到临时文件，然后在临时文件后面追加数据；最后重命名覆盖原文件
	 * 
	 * @param logDataPath
	 * @param offset
	 * @param logData
	 * @return
	 * @throws IOException
	 */
	boolean append(String logDataPath, long offset, List<LogData> logDataList) throws IOException;

	/**
	 * 读取所有内容
	 * 
	 * @param logDataPath
	 * @return
	 * @throws IOException
	 */
	List<LogData> getAllLogData(String logDataPath) throws IOException;

	/**
	 * 读取最后一条日志
	 * 
	 * @param logDataPath
	 * @return
	 */
	LogData getLastLogData(String logDataPath);

	/**
	 * 读取指定数量的日志
	 * 
	 * @param logDataPath
	 * @return
	 */
	List<LogData> getLogDataByCount(String logDataPath, int logDataCount);

	/**
	 * 计算日志总条数
	 * 
	 * @param logDataPath
	 * @return
	 * @throws IOException
	 */
	int getLogDataCount(String logDataPath) throws IOException;

}
