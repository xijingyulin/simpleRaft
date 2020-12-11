package com.sraft.client;

public class ActionResult {

	private int status;
	private int actionType;
	private String value;
	private int errCode;

	public ActionResult() {

	}

	public ActionResult(int status, int actionType, String value, int errCode) {
		this.status = status;
		this.actionType = actionType;
		this.value = value;
		this.errCode = errCode;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public int getActionType() {
		return actionType;
	}

	public void setActionType(int actionType) {
		this.actionType = actionType;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public int getErrCode() {
		return errCode;
	}

	public void setErrCode(int errCode) {
		this.errCode = errCode;
	}

}
