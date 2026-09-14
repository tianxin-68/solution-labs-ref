package com.bi.queryer.ssm.engine.validator;

/**
 * @Author contributor
 * @Date 14:09 2024-04-16
 * @Description 查询错误信息类型
 **/
public enum QueryMessageType {
    ERROR_TIMEOUT("查询超时", true, true),
    ERROR_MANUAL_KILL("手动kill", true, false),
    ERROR_CONFIG("配置错误", false, true),
    ERROR_OTHER("其他错误", true, true),
    ERROR_BLOCK("查询熔断", true, true),
    ERROR_RATE_LIMIT("查询限流", false, true),
    ERROR_TOO_MANY_PARTITION("查询分区数过多", false, true),
    ERROR_OLAP_API_RATE_LIMIT("olap_api查询限流", false, false),
    ERROR_VIEW_LIMIT("查询视图限流", false, false),
    SUCCESS("成功", true, true);

    private String desc;
    private boolean writeLog;
    private boolean sendMail;

    QueryMessageType(String desc, boolean writeLog, boolean sendMail) {
        this.desc = desc;
        this.writeLog = writeLog;
        this.sendMail = sendMail;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public boolean isWriteLog() {
        return writeLog;
    }

    public void setWriteLog(boolean writeLog) {
        this.writeLog = writeLog;
    }

    public boolean isSendMail() {
        return sendMail;
    }

    public void setSendMail(boolean sendMail) {
        this.sendMail = sendMail;
    }
}
