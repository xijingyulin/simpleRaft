package com.sraft.client.message;

import org.msgpack.annotation.Message;

import com.sraft.core.message.Msg;

@Message
public class ClientReply extends Msg {
	/**
	 * 是否成功
	 */
	protected int result;
	/**
	 * 错误码，1:跟随者 2:候选者 3:领导者暂停服务
	 */
	protected int errCode;
	/**
	 * 当errCode == 1，remark不为空，那么remark就是领导者地址，格式ip:port
	 */
	protected String remark;

	public ClientReply() {

	}

	public int getResult() {
		return result;
	}

	public void setResult(int result) {
		this.result = result;
	}

	public int getErrCode() {
		return errCode;
	}

	public void setErrCode(int errCode) {
		this.errCode = errCode;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("result:");
		builder.append(result);
		builder.append(",errCode:");
		builder.append(errCode);
		builder.append(",remark:");
		builder.append(remark);
		builder.append(",msgType:");
		builder.append(msgType);
		builder.append(",msgId:");
		builder.append(msgId);
		builder.append(",sendTime:");
		builder.append(sendTime);
		builder.append(",receviceTime:");
		builder.append(receviceTime);
		return builder.toString();
	}

}
