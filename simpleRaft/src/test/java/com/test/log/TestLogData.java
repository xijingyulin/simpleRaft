package com.test.log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.sraft.core.log.ILogData;
import com.sraft.core.log.LogData;
import com.sraft.core.log.LogDataImpl;

public class TestLogData {
	static String logDataPath = "file/log_old.log";

	public static void main(String args[]) throws IOException {
		//testAppendLogData();
		//testAppendLogDataByOffset();
		//testGetAllLogData();
		//testGetLogDataByIndex();
		//testGetLastLogData();
		printAllLogData("data/data2");
	}

	public static void testGetLastLogData() {
		ILogData iLogData = new LogDataImpl();
		LogData logData = iLogData.getLastLogData(logDataPath);
		System.out.println(logData.toString());
	}

	public static void testGetLogDataByIndex() throws IOException {
		ILogData iLogData = new LogDataImpl();
		LogData logData = iLogData.getLogDataByIndex(logDataPath, 1);
		System.out.println(logData.toString());
	}

	public static void testAppendLogDataByOffset() throws IOException {
		LogData logData = new LogData();
		logData.setLogIndex(9);
		logData.setClientSessionId(0);
		logData.setKey("abcdfg");
		logData.setValue("伍尚康");

		ILogData iLogData = new LogDataImpl();
		iLogData.tranLogData2Store(logData);
		List<LogData> logDataList = new ArrayList<LogData>();
		logDataList.add(logData);
		boolean isSuccess = iLogData.append(logDataPath, 880, logDataList);
		System.out.println("append result : " + isSuccess);
	}

	public static void testGetAllLogData() throws IOException {
		ILogData iLogData = new LogDataImpl();
		List<LogData> logDataList = iLogData.getAllLogData(logDataPath);
		for (int i = 0; i < logDataList.size(); i++) {
			System.out.println(i + ":" + logDataList.get(i).toString());
		}
	}

	public static void testAppendLogData() throws IOException {
		ILogData iLogData = new LogDataImpl();
		List<LogData> logDataList = new ArrayList<LogData>();
		for (int i = 40; i < 50; i++) {
			LogData logData = new LogData();
			logData.setLogIndex(i);
			logData.setLogTerm(i);
			logData.setKey("abcd");
			logData.setValue("伍尚康");
			iLogData.tranLogData2Store(logData);
			logDataList.add(logData);
		}
		iLogData.append(logDataPath, logDataList);
		System.out.println("finish!");
	}

	public static void printAllLogData(String path) throws IOException {
		//String path = "data";
		File dir = new File(path);
		File[] files = dir.listFiles();
		for (File file : files) {
			String filePath = file.getAbsolutePath();
			String fileName = file.getName();
			if (file.isDirectory()) {
				printAllLogData(filePath);
			} else if (file.isFile() && fileName.startsWith("log_")) {
				ILogData iLogData = new LogDataImpl();
				System.out.println("fileName:" + file.getName());
				List<LogData> logDataList = iLogData.getAllLogData(filePath);
				for (int i = 0; i < logDataList.size(); i++) {
					System.out.println(i + ":" + logDataList.get(i).toString());
				}
			}
		}

	}
}
