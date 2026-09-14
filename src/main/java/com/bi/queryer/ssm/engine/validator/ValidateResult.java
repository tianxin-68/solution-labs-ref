package com.bi.queryer.ssm.engine.validator;

/**
 * @Author contributor
 * @Date 17:17 2024-04-12
 * @Description 校验结果
 **/
public class ValidateResult {

    private boolean success = true;

    private String message = "";

    public ValidateResult() {
    }

    public ValidateResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
