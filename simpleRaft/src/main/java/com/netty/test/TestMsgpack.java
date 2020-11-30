package com.netty.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.msgpack.MessagePack;
import org.msgpack.template.Templates;

import com.sraft.core.message.RequestVoteMsg;
import com.sraft.core.message.ServerMsg;

public class TestMsgpack {

	public static void main2() throws IOException {

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

	public static void main(String args[]) throws IOException {

		RequestVoteMsg msg = new RequestVoteMsg();
		msg.setTerm(1);
		msg.setLastLogIndex(-1);
		msg.setNodeId(11);
		msg.setMsgId(2);

		MessagePack messagePack = new MessagePack();
		messagePack.register(RequestVoteMsg.class);
		byte[] encode = messagePack.write(msg);

		Object msg2 = messagePack.read(encode);
		Object msg3 = messagePack.read(encode, ServerMsg.class);
		//RequestVoteMsg msg3 = (RequestVoteMsg) msg2;
		System.out.println(msg2.toString());
		System.out.println(msg3.toString());

	}
}
