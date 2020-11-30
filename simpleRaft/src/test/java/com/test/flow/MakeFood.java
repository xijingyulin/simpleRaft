package com.test.flow;

import java.util.Date;

import com.sraft.common.flow.IFlowWorker;

public class MakeFood implements IFlowWorker {

	@Override
	public void deliver(Object object) {
		Food food = (Food) object;
		System.out.println(new Date() + "开始对食品进行再加工:" + food.toString());
		System.out.println(new Date() + "开始对食品进行加工完毕" + food.toString());
	}

}
