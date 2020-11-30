package com.sraft.core.log;

public class CompressLogJob implements Runnable {

	private ILogEntry iLogEntry;

	public CompressLogJob(ILogEntry iLogEntry) {
		this.iLogEntry = iLogEntry;
	}

	@Override
	public void run() {
		iLogEntry.doGenSnapshot();
	}
}
