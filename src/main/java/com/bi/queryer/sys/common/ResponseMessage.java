package com.bi.queryer.sys.common;

import com.bi.queryer.sys.exception.BIException;

import java.io.Serializable;
import java.util.HashMap;

/**
 * 响应app请求的消息对象
 * 
 */
public class ResponseMessage implements Serializable {
	
	public static final String MSG_SUCCESS = "success";
	
	public static final String MSG_FAIL = "fail";

	/**
     * <pre>
     * 
     * </pre>
     */
    private static final long serialVersionUID = -6212847872373060227L;

    // 执行状态码
	protected Boolean success;

	// 错误描述
	protected String message;

	protected Object data;

	protected String code;
	
	public ResponseMessage() {
		this.success = true;
		this.message = MSG_SUCCESS;
	}
	
	public ResponseMessage(Boolean success) {
		this.success = success;
	}
	
	public ResponseMessage(Boolean success, String message) {
		this.success = success;
		this.message = message;
	}
	
	public ResponseMessage(Boolean success, String message,Object data) {
		this(success, message);
		this.data = data;
	}

	public ResponseMessage(Boolean success, String message, Object data, String code) {
		this.success = success;
		this.message = message;
		this.data = data;
		this.code = code;
	}

	public ResponseMessage(Object data) {
		this();
		if (null == data) {
			this.data = new HashMap();
		} else {
			this.data = data;
		}
	}
	
	public ResponseMessage(Throwable e) {
		e.printStackTrace();
		this.success = false;
		this.message = e.getMessage();
		this.data = "";
		if(e instanceof BIException){
			this.code = ((BIException) e).getCode();
		}
	}
	
	public void set(boolean success, String message) {
		this.success = success;
		this.message = message;
	}
	
	public void set(boolean success, String message, Object data) {
		this.set(success, message);
		this.data = data;
	}
	
	public void set(Throwable e) {
		this.success = false;
		this.message = e.getMessage();
		this.data = new HashMap();
	}

	public static ResponseMessage success(Object data) {
		return new ResponseMessage(data);
	}

	public static ResponseMessage fail(String message) {
		return new ResponseMessage(false, message);
	}

	public Boolean getSuccess() {
		return success;
	}
	
	public void setSuccess(Boolean success) {
		this.success = success;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}
}