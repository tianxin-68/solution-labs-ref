package com.bi.queryer.sys.common;
import java.io.Serializable;

/**
 * 响应app请求的消息对象
 *
 */
public class SSMResponseMessage<T> implements Serializable {

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

	protected T data;

	public SSMResponseMessage() {
		this.success = true;
		this.message = MSG_SUCCESS;
	}

	public SSMResponseMessage(Boolean success) {
		this.success = success;
	}

	public SSMResponseMessage(Boolean success, String message) {
		this.success = success;
		this.message = message;
	}

	public SSMResponseMessage(Boolean success, String message, T data) {
		this(success, message);
		this.data = data;
	}

	public static <T> SSMResponseMessage<T> success(String message) {
		return SSMResponseMessage.success(true, message, null);
	}

	public static <T> SSMResponseMessage<T> success(String message, T data) {
		return SSMResponseMessage.success(true, message, data);
	}

	public static <T> SSMResponseMessage<T> success(Boolean success,String message, T data) {
		return new SSMResponseMessage(success, message, data);
	}

	public static <T> SSMResponseMessage<T> operationFailed(String message) {
		return new SSMResponseMessage(false, message);
	}

	public SSMResponseMessage(T data) {
		this();
		this.data = data;
	}

	public void set(boolean success, String message) {
		this.success = success;
		this.message = message;
	}

	public void set(boolean success, String message, T data) {
		this.set(success, message);
		this.data = data;
	}

	public void set(Throwable e) {
		this.success = false;
		this.message = e.getMessage();
		this.data = null;
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

	public void setData(T data) {
		this.data = data;
	}

}
