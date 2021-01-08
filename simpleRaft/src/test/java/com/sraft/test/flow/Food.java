package com.sraft.test.flow;

public class Food {

	private String name;
	private String type;

	public Food(String name, String type) {
		super();
		this.name = name;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("name:");
		builder.append(name);
		builder.append(",type:");
		builder.append(type);
		return builder.toString();
	}

}
