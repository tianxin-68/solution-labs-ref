package com.bi.queryer.sys.common;

/**
 * @Auther: contributor
 * @Date: 2025/8/7 14:21
 * @Description:
 */


import com.bi.queryer.sys.exception.BIException;

import java.io.Serializable;

public class BizBaseResponse<T> implements Serializable {

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

    protected String code;

    public BizBaseResponse() {
        this.success = true;
        this.message = MSG_SUCCESS;
    }

    public BizBaseResponse(Boolean success) {
        this.success = success;
    }

    public BizBaseResponse(Boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public BizBaseResponse(Boolean success, String message, T data) {
        this(success, message);
        this.data = data;
    }

    public BizBaseResponse(Boolean success, String message, T data, String code) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.code = code;
    }

    public BizBaseResponse(T data) {
        this();
        this.data = data;
    }

    public BizBaseResponse(Throwable e) {
        this.success = false;
        this.message = e.getMessage();
        this.data = null;
        if (e instanceof BIException) {
            this.code = ((BIException) e).getCode();
        }
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

    public static <T> BizBaseResponse<T> success(T data) {
        return new BizBaseResponse<T>(data);
    }

    public static <T> BizBaseResponse<T> fail(String message) {
        return new BizBaseResponse<T>(false, message);
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

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
