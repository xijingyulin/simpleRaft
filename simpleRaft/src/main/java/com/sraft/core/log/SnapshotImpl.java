package com.sraft.core.log;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.sraft.common.StringHelper;

public class SnapshotImpl implements ISnapshot {

	@Override
	public Snapshot getLastSnapshot(String snapshotPath) {
		Snapshot snapshot = null;

		RandomAccessFile raf = null;
		File file = new File(snapshotPath);
		if (!file.exists()) {
			return snapshot;
		}
		try {
			raf = new RandomAccessFile(file, "r");
			byte[] byteArr = null;
			while (raf.getFilePointer() != raf.length()) {
				snapshot = new Snapshot();
				snapshot.setLogIndex(raf.readLong());
				snapshot.setLogTerm(raf.readLong());
				snapshot.setLogLength(raf.readLong());

				int keyLength = raf.readInt();
				snapshot.setKeyLength(keyLength);
				byteArr = new byte[keyLength];
				raf.read(byteArr);
				snapshot.setbKey(byteArr);

				int valueLength = raf.readInt();
				snapshot.setValueLength(valueLength);
				byteArr = new byte[valueLength];
				raf.read(byteArr);
				snapshot.setbValue(byteArr);
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
		return snapshot;
	}

	@Override
	public List<Snapshot> getAllSnapshot(String snapshotPath) {
		List<Snapshot> snapshotList = new ArrayList<Snapshot>();

		RandomAccessFile raf = null;
		File file = new File(snapshotPath);
		if (!file.exists()) {
			return snapshotList;
		}
		try {
			raf = new RandomAccessFile(file, "r");
			byte[] byteArr = null;
			Snapshot snapshot = null;
			while (raf.getFilePointer() != raf.length()) {
				snapshot = new Snapshot();
				snapshot.setLogIndex(raf.readLong());
				snapshot.setLogTerm(raf.readLong());
				snapshot.setLogLength(raf.readLong());

				int keyLength = raf.readInt();
				snapshot.setKeyLength(keyLength);
				byteArr = new byte[keyLength];
				raf.read(byteArr);
				snapshot.setbKey(byteArr);

				int valueLength = raf.readInt();
				snapshot.setValueLength(valueLength);
				byteArr = new byte[valueLength];
				raf.read(byteArr);
				snapshot.setbValue(byteArr);

				snapshotList.add(snapshot);
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
		return snapshotList;
	}

	@Override
	public String[] genSnapshot(ILogData iLogData, String logDataDir, String oldSnapshotPath, String oldLogDataPath,
			int compressCount) throws IOException {
		String[] filePathArr = null;
		List<Snapshot> allSnapshot = new ArrayList<Snapshot>();
		List<LogData> compressLogDataList = new ArrayList<LogData>();
		// 读取旧快照
		if (StringHelper.checkIsNotNull(oldSnapshotPath)) {
			allSnapshot.addAll(getAllSnapshot(oldSnapshotPath));
		}
		// 读取需要被压缩的日志
		compressLogDataList.addAll(iLogData.getLogDataByCount(oldLogDataPath, compressCount));
		// 写入状态机，过滤垃圾数据
		Map<String, Snapshot> oldStatemachine = new LinkedHashMap<String, Snapshot>();
		// 快照最后一条数据，是用来标识快照最后一条日志的索引和任期
		Snapshot lastSnapshot = null;
		for (int i = 0; i < allSnapshot.size(); i++) {
			Snapshot snapshot = allSnapshot.get(i);
			if (i == allSnapshot.size() - 1) {
				lastSnapshot = snapshot;
			} else {
				String key = new String(snapshot.getbKey(), "UTF-8");
				oldStatemachine.put(key, snapshot);
			}
		}
		for (int i = 0; i < compressLogDataList.size(); i++) {
			LogData logData = compressLogDataList.get(i);
			String newKey = logData.getKey();
			int type = logData.getLogType();

			Snapshot snapshot = new Snapshot();
			snapshot.setLogIndex(logData.getLogIndex());
			snapshot.setLogTerm(logData.getLogTerm());
			snapshot.setKeyLength(logData.getKeyLength());
			snapshot.setbKey(logData.getbKey());
			snapshot.setValueLength(logData.getValueLength());
			snapshot.setbValue(logData.getbValue());
			long snapLogLen = Snapshot.FIXED_BYTE_LENGTH + logData.getKeyLength() + logData.getValueLength();
			snapshot.setLogLength(snapLogLen);

			if (type == LogData.LOG_PUT) {
				oldStatemachine.remove(newKey);
				oldStatemachine.put(newKey, snapshot);
			} else if (type == LogData.LOG_DEL) {
				oldStatemachine.remove(newKey);
			} else if (type == LogData.LOG_UPDATE) {
				oldStatemachine.remove(newKey);
				oldStatemachine.put(newKey, snapshot);
			}
			if (i == compressLogDataList.size() - 1) {
				// 记录最后一条日志的索引和任期
				lastSnapshot = new Snapshot();
				lastSnapshot.setLogIndex(logData.getLogIndex());
				lastSnapshot.setLogTerm(logData.getLogTerm());
				lastSnapshot.setbKey("".getBytes("UTF-8"));
				lastSnapshot.setKeyLength(lastSnapshot.getbKey().length);
				lastSnapshot.setbValue("".getBytes("UTF-8"));
				lastSnapshot.setValueLength(lastSnapshot.getbValue().length);
				long lastSnapshotLen = Snapshot.FIXED_BYTE_LENGTH + lastSnapshot.getKeyLength()
						+ lastSnapshot.getValueLength();
				lastSnapshot.setLogLength(lastSnapshotLen);
			}
		}
		if (oldStatemachine.size() > 0) {
			// 快照以第一条快照的索引ID作为文件名
			String newSnapshotPath = null;
			String temSnapshotPath = logDataDir + File.separator + "tem_snapshot.snapshot";
			// 日志以第一条日志的sraft事务ID作为文件名
			String newLogDataPath = null;
			String temLogDataPath = logDataDir + File.separator + "tem_logData.snapshot";
			RandomAccessFile raf = null;
			RandomAccessFile raf2 = null;
			RandomAccessFile raf3 = null;
			try {
				boolean isFirst = true;
				// 写新快照
				raf = new RandomAccessFile(temSnapshotPath, "rw");
				for (Entry<String, Snapshot> entry : oldStatemachine.entrySet()) {
					Snapshot snapshot = entry.getValue();
					long logIndex = snapshot.getLogIndex();
					if (isFirst) {
						newSnapshotPath = logDataDir + File.separator + LogSnapManager.PREFIX_SNAPSHOT + logIndex
								+ ".snapshot";
						isFirst = false;
					}
					raf.writeLong(snapshot.getLogIndex());
					raf.writeLong(snapshot.getLogTerm());
					raf.writeLong(snapshot.getLogLength());
					raf.writeInt(snapshot.getKeyLength());
					raf.write(snapshot.getbKey());
					raf.writeInt(snapshot.getValueLength());
					raf.write(snapshot.getbValue());
				}
				raf.writeLong(lastSnapshot.getLogIndex());
				raf.writeLong(lastSnapshot.getLogTerm());
				raf.writeLong(lastSnapshot.getLogLength());
				raf.writeInt(lastSnapshot.getKeyLength());
				raf.write(lastSnapshot.getbKey());
				raf.writeInt(lastSnapshot.getValueLength());
				raf.write(lastSnapshot.getbValue());

				oldStatemachine.clear();
				//删除被压缩的日志
				raf2 = new RandomAccessFile(oldLogDataPath, "r");
				raf3 = new RandomAccessFile(temLogDataPath, "rw");
				LogData lastLogData = compressLogDataList.get(compressLogDataList.size() - 1);
				long compressLogLen = lastLogData.getOffset();
				long remainLogLen = raf2.length() - compressLogLen;
				byte[] bytes = new byte[(int) remainLogLen];
				raf2.seek(compressLogLen);
				raf2.read(bytes);
				raf3.write(bytes);
				bytes = null;
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
				if (raf2 != null) {
					try {
						raf2.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				if (raf3 != null) {
					try {
						raf3.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				new File(oldLogDataPath).delete();
				new File(oldSnapshotPath).delete();
				new File(temSnapshotPath).renameTo(new File(newSnapshotPath));
				List<LogData> logDataList = iLogData.getLogDataByCount(temLogDataPath, 1);
				LogData firstLogData = logDataList.get(0);
				newLogDataPath = logDataDir + File.separator + LogSnapManager.PREFIX_LOG_DATA
						+ firstLogData.getSraftTransactionId() + ".log";
				new File(temLogDataPath).renameTo(new File(newLogDataPath));
				filePathArr = new String[2];
				filePathArr[0] = newSnapshotPath;
				filePathArr[1] = newLogDataPath;
			}
		}
		return filePathArr;
	}

	@Override
	public void transSnapshot2Store(Snapshot snapshot) {
		int keyLength = snapshot.getbKey().length;
		int valueLength = snapshot.getValueLength();
		int logLength = Snapshot.FIXED_BYTE_LENGTH + keyLength + valueLength;
		snapshot.setKeyLength(keyLength);
		snapshot.setValueLength(valueLength);
		snapshot.setLogLength(logLength);
	}

	@Override
	public boolean appendSnapshot(String snapshotPath, List<Snapshot> snapshotList) throws Exception {
		boolean isSuccess = true;
		RandomAccessFile raf = null;
		try {
			// 写新快照
			raf = new RandomAccessFile(snapshotPath, "rw");
			raf.seek(raf.length());
			for (Snapshot snapshot : snapshotList) {
				raf.writeLong(snapshot.getLogIndex());
				raf.writeLong(snapshot.getLogTerm());
				raf.writeLong(snapshot.getLogLength());
				raf.writeInt(snapshot.getKeyLength());
				raf.write(snapshot.getbKey());
				raf.writeInt(snapshot.getValueLength());
				raf.write(snapshot.getbValue());
			}
			isSuccess = true;
		} catch (Exception e) {
			e.printStackTrace();
			isSuccess = false;
			throw new Exception(e);
		} finally {
			if (raf != null) {
				raf.close();
			}
		}
		return isSuccess;
	}

	@Override
	public List<Snapshot> getSnapshotList(String snapshotPath, long beginSnapshotIndex, int count) throws Exception {
		List<Snapshot> snapshotList = new ArrayList<Snapshot>();

		RandomAccessFile raf = null;
		File file = new File(snapshotPath);
		if (!file.exists()) {
			return snapshotList;
		}
		try {
			raf = new RandomAccessFile(file, "r");
			byte[] byteArr = null;
			Snapshot snapshot = null;
			while (raf.getFilePointer() != raf.length()) {
				snapshot = new Snapshot();
				snapshot.setLogIndex(raf.readLong());
				snapshot.setLogTerm(raf.readLong());
				snapshot.setLogLength(raf.readLong());
				if (snapshot.getLogIndex() >= beginSnapshotIndex) {
					int keyLength = raf.readInt();
					snapshot.setKeyLength(keyLength);
					byteArr = new byte[keyLength];
					raf.read(byteArr);
					snapshot.setbKey(byteArr);
					int valueLength = raf.readInt();
					snapshot.setValueLength(valueLength);
					byteArr = new byte[valueLength];
					raf.read(byteArr);
					snapshot.setbValue(byteArr);

					snapshotList.add(snapshot);
					if (snapshotList.size() == count) {
						break;
					}
				} else {
					int remain = (int) snapshot.getLogLength() - 24;
					raf.skipBytes(remain);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new Exception(e);
		} finally {
			if (raf != null) {
				try {
					raf.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return snapshotList;
	}
}
