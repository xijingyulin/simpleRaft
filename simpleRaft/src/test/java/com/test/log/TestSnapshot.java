package com.test.log;

import java.io.IOException;
import java.util.List;

import com.sraft.common.FileHelper;
import com.sraft.core.log.ILogData;
import com.sraft.core.log.ISnapshot;
import com.sraft.core.log.LogDataImpl;
import com.sraft.core.log.Snapshot;
import com.sraft.core.log.SnapshotImpl;

public class TestSnapshot {
	public static void main(String args[]) throws IOException {
		//testGenSnapshot();
		testGetAllSnapshot();
		testGetLastSnapshot();
	}

	public static void testGetLastSnapshot() {
		String snapshotPath = "file/snap_new.log";
		ISnapshot iSnapshot = new SnapshotImpl();
		Snapshot snapshot = iSnapshot.getLastSnapshot(snapshotPath);
		System.out.println(snapshot.toString());
	}

	public static void testGetAllSnapshot() {
		String snapshotPath = "file/snap_new.log";
		ISnapshot iSnapshot = new SnapshotImpl();
		List<Snapshot> snapshotList = iSnapshot.getAllSnapshot(snapshotPath);
		for (Snapshot snapshot : snapshotList) {
			System.out.println(snapshot.toString());
		}
	}

	public static void testGenSnapshot() throws IOException {
		String logDataDir = "file";
		String oldSnapshotPath = "file/snap_old.log";
		String oldLogDataPath = "file/log_old.log";
		ILogData iLogData = new LogDataImpl();
		ISnapshot iSnapshot = new SnapshotImpl();
		int compressCount = 5;
		iSnapshot.genSnapshot(iLogData, logDataDir, oldSnapshotPath, oldLogDataPath, compressCount);
	}

}
