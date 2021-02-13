package com.sraft.client;

import com.sraft.core.message.ClientActionMsg;
import com.sraft.core.message.ReplyClientActionMsg;

public interface IDataCallBack {

	public void call(ClientActionMsg request, ReplyClientActionMsg response);
}
