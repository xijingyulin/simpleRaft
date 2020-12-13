package com.test.log;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.sraft.core.log.ILogData;
import com.sraft.core.log.LogData;
import com.sraft.core.log.LogDataImpl;

public class TestLogDataMatch {
	private static Map<String, String> logDataMD5Map = new HashMap<String, String>();
	private static Map<String, Set<String>> logDataMap = new HashMap<String, Set<String>>();
	private static List<List<String>> allLogDataStrList = new ArrayList<List<String>>();

	public static void main(String args[]) throws IOException {
		printAllLogData("data");
		for (Entry<String, String> entry : logDataMD5Map.entrySet()) {
			System.out.println(entry.getKey());
			System.out.println(entry.getValue());
		}
		int firstFileSize = allLogDataStrList.get(0).size();
		for (int i = 0; i < firstFileSize; i++) {
			String firstStr = "-1";
			for (int j = 0; j < allLogDataStrList.size(); j++) {
				if (j == 0) {
					firstStr = allLogDataStrList.get(j).get(i);
				} else {
					String otherStr = allLogDataStrList.get(j).get(i);
					if (!firstStr.equals(otherStr)) {
						System.out.println("firstStr:" + firstStr + ",otherStr=" + otherStr);
					}
				}
			}
		}
	}

	public static void printAllLogData(String path) throws IOException {
		File dir = new File(path);
		File[] files = dir.listFiles();
		for (File file : files) {
			String filePath = file.getAbsolutePath();
			String fileName = file.getName();
			if (file.isDirectory()) {
				printAllLogData(filePath);
			} else if (file.isFile() && fileName.startsWith("log_")) {
				ILogData iLogData = new LogDataImpl();
				String absolutePath = file.getAbsolutePath();
				List<LogData> logDataList = iLogData.getAllLogData(filePath);
				Set<String> set = new HashSet<String>();
				StringBuffer sb = new StringBuffer();
				List<String> logDataStrList = new ArrayList<String>();
				for (LogData logData : logDataList) {
					sb.append(logData.toString());
					set.add(logData.toString());
					logDataStrList.add(logData.toString());
				}
				allLogDataStrList.add(logDataStrList);
				logDataMD5Map.put(absolutePath, getMd5(sb.toString()));
				logDataMap.put(absolutePath, set);
			}
		}
	}

	public static String getMd5(String str) {
		MessageDigest md;
		StringBuffer sb = new StringBuffer();
		try {
			md = MessageDigest.getInstance("MD5");
			md.update(str.getBytes("GBK"));
			byte[] bs = md.digest();
			int tem = 0;
			for (int i = 0; i < bs.length; i++) {
				tem = bs[i];
				if (tem < 0) {
					tem = tem + 256;
				}
				if (tem < 16) {
					sb.append(0);
				}
				sb.append(Integer.toHexString(tem));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sb.toString().toUpperCase();
	}
}
