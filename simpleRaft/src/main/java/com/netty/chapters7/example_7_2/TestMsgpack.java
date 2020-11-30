package com.netty.chapters7.example_7_2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.msgpack.MessagePack;
import org.msgpack.template.Template;
import org.msgpack.template.Templates;

public class TestMsgpack {

	public static void main(String args[]) throws IOException {

		List<String> strList = new ArrayList<String>();
		strList.add("张三");
		strList.add("李四");
		MessagePack messagePack = new MessagePack();
		byte[] encode = messagePack.write(strList);
		List<String> decodeList = messagePack.read(encode, Templates.tList(Templates.TString));
		for (String str : decodeList) {
			System.out.println(str);
		}
	}
}
