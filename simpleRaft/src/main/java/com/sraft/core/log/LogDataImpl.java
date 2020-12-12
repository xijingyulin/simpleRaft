package com.sraft.core.log;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sraft.common.FileHelper;
import com.sraft.common.StringHelper;

public class LogDataImpl implements ILogData {
	private static Logger LOG = LoggerFactory.getLogger(LogDataImpl.class);

	@Override
	public boolean append(String logDataPath, List<LogData> logDataList) throws IOException {
		boolean isSuccess = true;
		RandomAccessFile raf = null;
		try {
			File file = FileHelper.createFile(logDataPath);
			raf = new RandomAccessFile(file, "rw");
			raf.seek(raf.length());
			for (LogData logData : logDataList) {
				raf.writeLong(logData.getLogIndex());
				raf.writeLong(logData.getLogTerm());
				raf.writeInt(logData.getLogType());

				raf.writeLong(logData.getLogLength());

				raf.writeInt(logData.getLeaderId());
				raf.writeLong(logData.getClientSessionId());
				raf.writeLong(logData.getClientTransactionId());
				raf.writeLong(logData.getSraftTransactionId());
				raf.writeLong(logData.getCreateTime());
				raf.writeLong(logData.getUpdateTime());
				raf.writeInt(logData.getKeyLength());
				raf.write(logData.getbKey());
				raf.writeInt(logData.getValueLength());
				raf.write(logData.getbValue());
				logData.setOffset(raf.getFilePointer());
			}
			isSuccess = true;
		} catch (IOException e) {
			e.printStackTrace();
			isSuccess = false;
			throw new IOException(e);
		} finally {
			if (raf != null) {
				try {
					raf.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return isSuccess;
	}

	@Override
	public LogData getLogDataByIndex(String logDataPath, long logIndex) throws IOException {

		LogData logData = null;
		RandomAccessFile raf = null;
		File file = new File(logDataPath);
		if (!file.exists()) {
			return logData;
		}
		try {
			raf = new RandomAccessFile(file, "r");
			byte[] byteArr = null;
			while (raf.getFilePointer() != raf.length()) {
				long temLogIndex = raf.readLong();
				long temLogTerm = raf.readLong();
				int temLogType = raf.readInt();
				long logLength = raf.readLong();
				if (logIndex == temLogIndex) {
					logData = new LogData();
					logData.setLogIndex(temLogIndex);
					logData.setLogTerm(temLogTerm);
					logData.setLogType(temLogType);
					logData.setLogLength(logLength);
					logData.setLeaderId(raf.readInt());
					logData.setClientSessionId(raf.readLong());
					logData.setClientTransactionId(raf.readLong());
					logData.setSraftTransactionId(raf.readLong());
					logData.setCreateTime(raf.readLong());
					logData.setUpdateTime(raf.readLong());

					int keyLength = raf.readInt();
					logData.setKeyLength(keyLength);

					byteArr = new byte[keyLength];
					raf.read(byteArr);
					String key = new String(byteArr, "UTF-8");
					logData.setbKey(byteArr);
					logData.setKey(key);

					int valueLength = raf.readInt();
					logData.setValueLength(valueLength);

					byteArr = new byte[valueLength];
					raf.read(byteArr);
					String value = new String(byteArr, "UTF-8");
					logData.setbValue(byteArr);
					logData.setValue(value);

					long offset = raf.getFilePointer();
					logData.setOffset(offset);
					byteArr = null;
					break;
				} else {
					int remainType = (int) logLength - 28;
					raf.skipBytes(remainType);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			logData = null;
			throw new IOException(e);
		} finally {
			if (raf != null) {
				try {
					raf.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return logData;
	}

	@Override
	public boolean append(String logDataPath, long offset, List<LogData> logDataList) throws IOException {
		boolean isSuccess = true;
		File file = new File(logDataPath);
		String originalName = file.getName();
		String temName = "temp_" + originalName;
		String temFilePath = file.getParentFile().getAbsolutePath() + File.separator + temName;
		File temFile = new File(temFilePath);
		RandomAccessFile raf = null;
		RandomAccessFile temRaf = null;
		boolean isMatch = false;
		try {
			FileHelper.createNewEmptyFile(temFilePath);
			raf = new RandomAccessFile(file, "r");
			temRaf = new RandomAccessFile(temFilePath, "rw");
			byte[] bytes = new byte[(int) offset];
			if (offset <= raf.length()) {
				raf.read(bytes);
				temRaf.write(bytes);
				isMatch = true;
			}
		} catch (IOException e) {
			e.printStackTrace();
			isSuccess = false;
			isMatch = false;
			throw new IOException(e);
		} finally {
			if (raf != null) {
				try {
					raf.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (temRaf != null) {
				try {
					temRaf.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		if (isMatch) {
			//插入成功
			try {
				if (append(temFilePath, logDataList)) {
					file.delete();
					temFile.renameTo(new File(logDataPath));
				} else {
					isSuccess = false;
					temFile.delete();
				}
			} catch (IOException e) {
				e.printStackTrace();
				isSuccess = false;
				temFile.delete();
			}
		}
		return isSuccess;
	}

	@Override
	public void tranLogData2Store(LogData logData) throws UnsupportedEncodingException {
		logData.setbKey(logData.getKey().getBytes("UTF-8"));
		logData.setKeyLength(logData.getbKey().length);
		if (StringHelper.checkIsNotNull(logData.getValue())) {
			logData.setbValue(logData.getValue().getBytes("UTF-8"));
		} else {
			logData.setbValue("".getBytes("UTF-8"));
		}
		logData.setValueLength(logData.getbValue().length);
		logData.setLogLength(LogData.FIXED_BYTE_LENGTH + logData.getbKey().length + logData.getbValue().length);
	}

	@Override
	public List<LogData> getAllLogData(String logDataPath) throws IOException {
		List<LogData> logDataList = new ArrayList<LogData>();
		RandomAccessFile raf = null;
		File file = new File(logDataPath);
		if (!file.exists()) {
			return logDataList;
		}
		try {
			raf = new RandomAccessFile(file, "r");
			byte[] byteArr = null;
			LogData logData = null;
			while (raf.getFilePointer() != raf.length()) {
				logData = new LogData();
				logData.setLogIndex(raf.readLong());
				logData.setLogTerm(raf.readLong());
				logData.setLogType(raf.readInt());
				logData.setLogLength(raf.readLong());
				logData.setLeaderId(raf.readInt());
				logData.setClientSessionId(raf.readLong());
				logData.setClientTransactionId(raf.readLong());
				logData.setSraftTransactionId(raf.readLong());
				logData.setCreateTime(raf.readLong());
				logData.setUpdateTime(raf.readLong());

				int keyLength = raf.readInt();
				logData.setKeyLength(keyLength);

				byteArr = new byte[keyLength];
				raf.read(byteArr);
				String key = new String(byteArr, "UTF-8");
				logData.setbKey(byteArr);
				logData.setKey(key);

				int valueLength = raf.readInt();
				logData.setValueLength(valueLength);

				byteArr = new byte[valueLength];
				raf.read(byteArr);
				String value = new String(byteArr, "UTF-8");
				logData.setbValue(byteArr);
				logData.setValue(value);

				long offset = raf.getFilePointer();
				logData.setOffset(offset);
				logDataList.add(logData);
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new IOException(e);
		} finally {
			if (raf != null) {
				try {
					raf.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return logDataList;
	}

	@Override
	public LogData getLastLogData(String logDataPath) {
		LogData logData = null;
		RandomAccessFile raf = null;
		File file = new File(logDataPath);
		if (!file.exists()) {
			return logData;
		}
		try {
			raf = new RandomAccessFile(file, "r");
			byte[] byteArr = null;
			while (raf.getFilePointer() != raf.length()) {
				logData = new LogData();
				logData.setLogIndex(raf.readLong());
				logData.setLogTerm(raf.readLong());
				logData.setLogType(raf.readInt());
				logData.setLogLength(raf.readLong());
				logData.setLeaderId(raf.readInt());
				logData.setClientSessionId(raf.readLong());
				logData.setClientTransactionId(raf.readLong());
				logData.setSraftTransactionId(raf.readLong());
				logData.setCreateTime(raf.readLong());
				logData.setUpdateTime(raf.readLong());

				int keyLength = raf.readInt();
				logData.setKeyLength(keyLength);

				byteArr = new byte[keyLength];
				raf.read(byteArr);
				String key = new String(byteArr, "UTF-8");
				logData.setbKey(byteArr);
				logData.setKey(key);

				int valueLength = raf.readInt();
				logData.setValueLength(valueLength);

				byteArr = new byte[valueLength];
				raf.read(byteArr);
				String value = new String(byteArr, "UTF-8");
				logData.setbValue(byteArr);
				logData.setValue(value);

				long offset = raf.getFilePointer();
				logData.setOffset(offset);
				byteArr = null;
			}
		} catch (IOException e) {
			e.printStackTrace();
			logData = null;
		} finally {
			if (raf != null) {
				try {
					raf.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return logData;
	}

	@Override
	public List<LogData> getLogDataByCount(String logDataPath, int logDataCount) {
		List<LogData> logDataList = new ArrayList<LogData>();
		RandomAccessFile raf = null;
		File file = new File(logDataPath);
		if (!file.exists()) {
			return logDataList;
		}
		try {
			int count = 0;
			raf = new RandomAccessFile(file, "r");
			byte[] byteArr = null;
			LogData logData = null;
			while (raf.getFilePointer() != raf.length()) {
				logData = new LogData();
				logData.setLogIndex(raf.readLong());
				logData.setLogTerm(raf.readLong());
				logData.setLogType(raf.readInt());
				logData.setLogLength(raf.readLong());
				logData.setLeaderId(raf.readInt());
				logData.setClientSessionId(raf.readLong());
				logData.setClientTransactionId(raf.readLong());
				logData.setSraftTransactionId(raf.readLong());
				logData.setCreateTime(raf.readLong());
				logData.setUpdateTime(raf.readLong());

				int keyLength = raf.readInt();
				logData.setKeyLength(keyLength);

				byteArr = new byte[keyLength];
				raf.read(byteArr);
				String key = new String(byteArr, "UTF-8");
				logData.setbKey(byteArr);
				logData.setKey(key);

				int valueLength = raf.readInt();
				logData.setValueLength(valueLength);

				byteArr = new byte[valueLength];
				raf.read(byteArr);
				String value = new String(byteArr, "UTF-8");
				logData.setbValue(byteArr);
				logData.setValue(value);

				long offset = raf.getFilePointer();
				logData.setOffset(offset);
				logDataList.add(logData);
				count++;
				if (count == logDataCount) {
					break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (raf != null) {
				try {
					raf.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return logDataList;
	}

	@Override
	public int getLogDataCount(String logDataPath) throws IOException {
		int count = 0;
		RandomAccessFile raf = null;
		File file = new File(logDataPath);
		if (!file.exists()) {
			return count;
		}
		try {
			raf = new RandomAccessFile(file, "r");
			while (raf.getFilePointer() != raf.length()) {
				// 前面有3个字段，共20个字节
				raf.skipBytes(20);
				//长度字节8个字节
				long logLength = raf.readLong();
				int remainLength = (int) (logLength - 28);
				raf.skipBytes(remainLength);
				count++;
			}
		} catch (IOException e) {
			e.printStackTrace();
			count = 0;
			throw new IOException(e);
		} finally {
			if (raf != null) {
				try {
					raf.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return count;
	}

	@Override
	public List<LogData> getLogDataByCount(String logDataPath, long beginLogIndex, int logDataCount)
			throws IOException {
		List<LogData> logDataList = new ArrayList<LogData>();
		RandomAccessFile raf = null;
		File file = new File(logDataPath);
		if (!file.exists()) {
			return logDataList;
		}
		try {
			LogData logData = null;
			raf = new RandomAccessFile(file, "r");
			byte[] byteArr = null;
			while (raf.getFilePointer() != raf.length()) {
				long temLogIndex = raf.readLong();
				long temLogTerm = raf.readLong();
				int temLogType = raf.readInt();
				long logLength = raf.readLong();
				if (temLogIndex >= beginLogIndex) {
					logData = new LogData();
					logData.setLogIndex(temLogIndex);
					logData.setLogTerm(temLogTerm);
					logData.setLogType(temLogType);
					logData.setLogLength(logLength);
					logData.setLeaderId(raf.readInt());
					logData.setClientSessionId(raf.readLong());
					logData.setClientTransactionId(raf.readLong());
					logData.setSraftTransactionId(raf.readLong());
					logData.setCreateTime(raf.readLong());
					logData.setUpdateTime(raf.readLong());

					int keyLength = raf.readInt();
					logData.setKeyLength(keyLength);

					byteArr = new byte[keyLength];
					raf.read(byteArr);
					String key = new String(byteArr, "UTF-8");
					logData.setbKey(byteArr);
					logData.setKey(key);

					int valueLength = raf.readInt();
					logData.setValueLength(valueLength);

					byteArr = new byte[valueLength];
					raf.read(byteArr);
					String value = new String(byteArr, "UTF-8");
					logData.setbValue(byteArr);
					logData.setValue(value);

					long offset = raf.getFilePointer();
					logData.setOffset(offset);
					byteArr = null;

					logDataList.add(logData);
					if (logDataList.size() >= logDataCount) {
						break;
					}
				} else {
					int remainType = (int) logLength - 28;
					raf.skipBytes(remainType);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new IOException(e);
		} finally {
			if (raf != null) {
				try {
					raf.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return logDataList;
	}
}
