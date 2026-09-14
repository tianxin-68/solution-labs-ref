package com.bi.queryer.ssm.engine.interceptor;

/**
 * @Author contributor
 * @Date 11:34 2025/12/5
 * @Description 查询拦截结果
 **/
public class QueryInterceptResult {
    protected boolean intercepted = false;
    protected String message;
    protected Object result;
    protected String code;

    public QueryInterceptResult(){

    }

    public QueryInterceptResult(boolean intercepted) {
        this.intercepted = intercepted;
    }

    public QueryInterceptResult(boolean intercepted, String message) {
        this.intercepted = intercepted;
        this.message = message;
    }

    public QueryInterceptResult(boolean intercepted, String message, String code) {
        this.intercepted = intercepted;
        this.message = message;
        this.code = code;
    }

    public boolean isIntercepted() {
        return intercepted;
    }

    public void setIntercepted(boolean intercepted) {
        this.intercepted = intercepted;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
