package com.bi.queryer.sys.common;

import java.io.Serializable;
import java.util.List;

import net.sf.json.JSONObject;

/**
 * 通用返回信息类，用与存储某部操作的结果
 */
public class ReturnMsg<T> implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public static final String Status_Success ="success";
	
	public static final String Status_Error ="error";
	
	
	private boolean hasError = false; // 操作结果，true为成功，false
	private String errorCode;
	private String msg; // 包含一些有需要的信息，比如操作对象的id，操作记录条数等
	private String errorMsg; // 出现异常时，中文提示信息
	private List<String> msgList;// 逐条提示信息，用于导入类型的功能，可以记录每行操作结果
	private T result; 
	
	private String status = Status_Success;
	
	public T getResult() {
		return result;
	}

	public void setResult(T result) {
		this.result = result;
	}

	public ReturnMsg(Throwable e){
		super();
		hasError = true;
		errorMsg = e.getMessage();
		e.printStackTrace();
	}
	
	public ReturnMsg() {
		super();
	}
	
	public ReturnMsg(boolean hasError) {
		this();
		this.hasError = hasError;
		if(hasError) {
			this.status = Status_Error;
		}else {
			this.status = Status_Success;
		}
	}

	public ReturnMsg(boolean hasError, String errorCode) {
		this(hasError);
		this.errorCode = errorCode;
	}

	public ReturnMsg(boolean hasError, String errorCode, String msg) {
		this(hasError, errorCode);
		this.msg = msg;
	}

	public ReturnMsg(boolean hasError, String errorCode, String msg, String errorMsg) {
		this(hasError, errorCode, msg);
		this.errorMsg = errorMsg;
	}

	public boolean isHasError() {
		return hasError;
	}

	public void setHasError(boolean hasError) {
		this.hasError = hasError;
		if(hasError) {
			this.status = Status_Error;
		}else {
			this.status = Status_Success;
		}
	}

	public String getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public String getErrorMsg() {
		return errorMsg;
	}

	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}

	public List<String> getMsgList() {
		return msgList;
	}

	public void setMsgList(List<String> msgList) {
		this.msgList = msgList;
	}

	public JSONObject toJSON() {
		JSONObject json = JSONObject.fromObject(this);
		if(this.isHasError()){
			json.put("info", this.getErrorMsg());
		}else {
			json.put("info", this.getMsg());
		}
		return json;
	}
	
	public JSONObject toJSON(boolean mini) {
		if(!mini) {
			return toJSON();
		}
		JSONObject json = new JSONObject();
		if(this.isHasError()){
			json.put("info", this.getErrorMsg());
		}else {
			json.put("info", this.getMsg());
		}
		json.put("status", getStatus());
		json.put("result", getResult());
		return json;
	}
	
	public static ReturnMsg<Object> instanceSuccessMsg(String successMsg){
		ReturnMsg<Object> returnMsg = new ReturnMsg<Object>();
		returnMsg.setMsg(successMsg);
		return returnMsg;
	}
	public static ReturnMsg<Object> instanceSuccessMsg(String successMsg, Object result){
		ReturnMsg<Object> returnMsg = new ReturnMsg<Object>();
		returnMsg.setMsg(successMsg);
		returnMsg.setResult(result);
		return returnMsg;
	}

	public String getStatus() {
		if(hasError) {
			this.status = Status_Error;
		}else {
			this.status = Status_Success;
		}
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
