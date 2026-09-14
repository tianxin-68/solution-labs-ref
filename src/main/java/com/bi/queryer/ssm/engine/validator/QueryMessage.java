package com.bi.queryer.ssm.engine.validator;

/**
 * @Author contributor
 * @Date 14:09 2024-04-16
 * @Description 查询错误信息
 **/
public class QueryMessage {
    private QueryMessageType type = QueryMessageType.SUCCESS;

    private String message = "";

    public QueryMessage() {
    }

    public QueryMessage(String message, QueryMessageType type) {
        this.type = type;
        this.message = message;
    }

    public QueryMessageType getType() {
        return type;
    }

    public void setType(QueryMessageType type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
