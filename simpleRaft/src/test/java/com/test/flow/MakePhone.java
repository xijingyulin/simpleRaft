package com.test.flow;

import java.util.Date;

import com.sraft.common.flow.IFlowWorker;

public class MakePhone implements IFlowWorker {

	@Override
	public void deliver(Object object) {
		Phone phone = (Phone) object;
		System.out.println(new Date() + "开始对手机进行再加工:" + phone.toString());
		System.out.println(new Date() + "开始对手机进行加工完毕" + phone.toString());
	}

}
