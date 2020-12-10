package com.sraft.core.log;

public class CompressLogJob implements Runnable {

	private ILogSnap iLogEntry;

	public CompressLogJob(ILogSnap iLogEntry) {
		this.iLogEntry = iLogEntry;
	}

	@Override
	public void run() {
		iLogEntry.doGenSnapshot();
	}
}
