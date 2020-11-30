package com.sraft.common;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateHelper {

	/**
	 * 常用格式
	 */
	public static final String YYYYMMDD = "yyyyMMdd";
	public static final String YYYYMMDD2 = "yyyy-MM-dd";
	public static final String YYYYMMDDHHMMSS = "yyyyMMddHHmmss";
	public static final String YYYYMMDDHHMMSS2 = "yyyy-MM-dd HH:mm:ss";
	public static final String YYYYMMDDHHMMSSsss = "yyyyMMddHHmmssSSS";

	public static String timeStr2TimeStr(String SrcTimeStr, String srcFormat, String dstFormat) throws ParseException {
		Timestamp d = formatStr2Date(SrcTimeStr, srcFormat);
		return formatDate2Str(d, dstFormat);
	}

	public static Timestamp formatStr2Date(String timeStr, String format) throws ParseException {
		DateFormat df = new SimpleDateFormat(format);
		Date date = df.parse(timeStr);
		Timestamp timestamp = new Timestamp(date.getTime());
		return timestamp;
	}

	public static String formatDate2Str(Date date, String format) {
		DateFormat df = new SimpleDateFormat(format);
		return df.format(date);
	}

	public static long formatDate2Long(Date date, String format) {
		DateFormat df = new SimpleDateFormat(format);
		return Long.parseLong(df.format(date));
	}

	public static Date addYear(String timeStr, String format, int interval) throws ParseException {
		Timestamp timestamp = formatStr2Date(timeStr, format);
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(timestamp);
		calendar.add(Calendar.YEAR, interval);
		Timestamp timestamp2 = new Timestamp(calendar.getTime().getTime());
		return timestamp2;
	}

	public static long addMillSecond(Date date, int interval) throws ParseException {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.MILLISECOND, interval);
		Timestamp timestamp = new Timestamp(calendar.getTime().getTime());
		return formatDate2Long(timestamp, YYYYMMDDHHMMSSsss);
	}

	public static void main(String args[]) throws ParseException {
		Date date = new Date();
		System.out.println(formatDate2Long(date, YYYYMMDDHHMMSSsss));
		System.out.println(addMillSecond(date, -1000));
	}
}
