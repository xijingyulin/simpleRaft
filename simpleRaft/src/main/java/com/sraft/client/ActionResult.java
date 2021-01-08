package com.sraft.client;

public class ActionResult {

	/**
	 * 具体看:
	 * 
	 * Msg.RETURN_STATUS_OK
	 * 
	 * Msg.RETURN_STATUS_FALSE
	 */
	private int status;
	/**
	 * 
	 * 具体看:
	 * 
	 * LogData.LOG_PUT
	 * 
	 * LogData.LOG_UPDATE
	 * 
	 * LogData.LOG_REMOVE
	 * 
	 * LogData.LOG_GET
	 * 
	 */
	private int actionType;
	/**
	 * get方法才会有返回值
	 */
	private String value;
	/**
	 * 具体看:
	 * 
	 * Msg.ERR_CODE_LOG_*
	 */
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

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("status:");
		builder.append(status);
		builder.append(",actionType:");
		builder.append(actionType);
		builder.append(",value:");
		builder.append(value);
		builder.append(",errCode:");
		builder.append(errCode);
		return builder.toString();
	}

}
