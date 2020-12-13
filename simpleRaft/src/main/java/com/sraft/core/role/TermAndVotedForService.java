package com.sraft.core.role;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import com.sraft.common.FileHelper;
import com.sraft.common.StringHelper;
import com.sraft.core.Config;

public class TermAndVotedForService {

	private Config config;

	public TermAndVotedForService(Config config) {
		this.config = config;
	}

	/**
	 * 恢复，当前任期号，和投票投给了谁
	 */
	public long retrieveTerm() {
		long currentTerm = 0;
		String filePath = config.getLogDataDir() + File.separator + Config.TERM_FILE;
		File file = new File(filePath);
		BufferedReader br = null;
		try {
			if (!file.exists()) {
				try {
					FileHelper.createNewEmptyFile(filePath);
				} catch (IOException e) {
					e.printStackTrace();
				}
				return currentTerm;
			}
			br = new BufferedReader(new FileReader(new File(filePath)));
			String content = br.readLine();
			if (StringHelper.checkIsNotNull(content)) {
				currentTerm = Long.parseLong(content);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return currentTerm;
	}

	/**
	 * 恢复，当前任期号，和投票投给了谁
	 */
	public int retrieveVotedFor() {
		int votedFor = -1;
		String filePath = config.getLogDataDir() + File.separator + Config.VOTED_FOR_FILE;
		File file = new File(filePath);
		BufferedReader br = null;
		try {
			if (!file.exists()) {
				try {
					FileHelper.createNewEmptyFile(filePath);
				} catch (IOException e) {
					e.printStackTrace();
				}
				return votedFor;
			}
			br = new BufferedReader(new FileReader(new File(filePath)));
			String content = br.readLine();
			if (StringHelper.checkIsNotNull(content)) {
				votedFor = Integer.parseInt(content);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return votedFor;
	}

	/**
	 * 当任期号改变时，修改
	 */
	public synchronized void writeTerm(long currentTerm) {
		String filePath = config.getLogDataDir() + File.separator + Config.TERM_FILE;
		FileHelper.writeFile(filePath, currentTerm + "", false);
	}

	/**
	 * 当任期号改变时，修改
	 */
	public synchronized void writeVotedFor(int votedFor) {
		String filePath = config.getLogDataDir() + File.separator + Config.VOTED_FOR_FILE;
		FileHelper.writeFile(filePath, votedFor + "", false);
	}

}
