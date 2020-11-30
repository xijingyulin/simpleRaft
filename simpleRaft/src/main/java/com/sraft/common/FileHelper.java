package com.sraft.common;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class FileHelper {

	/**
	 * 新建空文件
	 * 
	 * @param filePath
	 *            文件路径
	 * @throws IOException
	 */
	public static File createNewEmptyFile(String filePath) throws IOException {
		File file = new File(filePath);
		if (file.exists()) {
			file.delete();
		}
		file = createFile(filePath);
		return file;
	}

	/**
	 * 新建文件，不存在就新建
	 * 
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public static File createFile(String path) throws IOException {
		File file = new File(path);
		File parent = file.getParentFile();
		if (parent != null && !parent.exists()) {
			parent.mkdirs();
		}
		if (!file.exists()) {
			file.createNewFile();
		}
		return file;
	}

	public static boolean delFile(String filePath) {
		File file = new File(filePath);
		return file.delete();
	}

	/**
	 * @param filePath
	 * @param content
	 * @param append
	 *            是否追加
	 */
	public static void writeFile(String filePath, String content, boolean append) {
		PrintWriter pw = null;
		BufferedWriter bw = null;
		FileWriter fw = null;
		try {
			fw = new FileWriter(filePath, append);
			bw = new BufferedWriter(fw);
			pw = new PrintWriter(bw);
			pw.println(content);
			pw.flush();

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (fw != null) {
				try {
					fw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (bw != null) {
				try {
					bw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (pw != null) {
				pw.close();
			}
		}
	}

}
