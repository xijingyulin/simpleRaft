package com.sraft.common;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertiesHelper {
	private final static Logger LOG = LoggerFactory.getLogger(PropertiesHelper.class);

	public static Properties getProperties(String config_file_path) throws IOException {
		InputStream in;
		Properties props = new Properties();
		try {
			in = new FileInputStream(config_file_path);
			props.load(in);
		} catch (Exception e) {
			LOG.error("配置文件加载失败：" + e.getMessage(), e);
		}
		return props;
	}

	/**
	 * 设置读编码，解决中文乱码问题
	 * 
	 * @param configFilePath
	 * @param charSet
	 * @return
	 * @throws IOException
	 */
	public static Properties getProperties(String configFilePath, String charSet) throws IOException {
		Properties props = new Properties();
		try {
			InputStreamReader ir = null;
			if (charSet == null || charSet.equals("null") || charSet == "null") {
				ir = new InputStreamReader(new FileInputStream(configFilePath));
			} else {
				ir = new InputStreamReader(new FileInputStream(configFilePath), charSet);
			}
			props.load(ir);
		} catch (Exception e) {
			LOG.error("配置文件加载失败：" + e.getMessage(), e);
		}
		return props;
	}

	public static void main(String[] args) {
		Properties prop;
		try {
			prop = PropertiesHelper.getProperties("conf/conf_acquclean_provider.properties");
			LOG.info(prop.getProperty("ZKURL"));
		} catch (IOException e) {
			e.printStackTrace();
			LOG.error(e.getMessage(), e);
		}

	}
}