package com.test.flow;

import java.util.Date;

import com.sraft.common.flow.FlowHeader;
import com.sraft.common.flow.NoFlowLineException;

public class TestFlowWoker {

	public static void main(String args[]) {
		TestFlowWoker test = new TestFlowWoker();
		test.test();
	}

	public void test() {
		String phoneWorkerCard = "1234";
		String foodWorkerCard = "12345";
		FlowHeader.employ(phoneWorkerCard, new MakePhone());
		FlowHeader.employ(foodWorkerCard, new MakeFood());
		for (int i = 0; i < 10; i++) {
			if (i % 2 == 0) {
				Phone phone = new Phone("iPhone " + i, "5999元");
				try {
					System.out.println(new Date() + ",传送货品:" + phone.toString());
					FlowHeader.putProducts(phoneWorkerCard, phone);
				} catch (NoFlowLineException e) {
					//e.printStackTrace();
				}
			} else {
				Food food = new Food("苹果" + i, "水果");
				try {
					System.out.println(new Date() + ",传送货品:" + food.toString());
					FlowHeader.putProducts(foodWorkerCard, food);
				} catch (NoFlowLineException e) {
					//e.printStackTrace();
				}
			}
		}
		FlowHeader.unEmploy(foodWorkerCard);
		FlowHeader.unEmploy(phoneWorkerCard);
	}
}
