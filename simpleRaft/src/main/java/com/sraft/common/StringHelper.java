package com.sraft.common;

public class StringHelper {
	/**
	 * 判断字符串不为空
	 * 
	 * @param str
	 * @return
	 */
	public static boolean checkIsNotNull(String str) {
		boolean bool = true;
		if (str == null || "".equals(str.trim()) || "null".equalsIgnoreCase(str.trim())) {
			bool = false;
		} else if (str.length() == 1 && str.charAt(0) == 12288) {
			bool = false;
		}
		return bool;
	}
}
