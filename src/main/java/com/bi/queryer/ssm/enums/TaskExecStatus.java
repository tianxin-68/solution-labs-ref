package com.bi.queryer.ssm.enums;

/**
 * @Author contributor
 * @Date 19:56 2024/10/10
 * @Description 脚本运行状态
 **/
public enum TaskExecStatus {
    READY("ready"), PADDING("padding"),RUNNING("running"),SUCCESS("success"),FAIL("fail"),CANCEL("cancel");
    private String code;
    private TaskExecStatus(String code){
        this.code = code;
    }

    public static TaskExecStatus get(String status){
        for(TaskExecStatus s : TaskExecStatus.values()){
            if(s.toString().equalsIgnoreCase(status)){
                return s;
            }
        }
        return READY;
    }

    public boolean isEnd() {
        return this == SUCCESS || this == FAIL || this == CANCEL;
    }

    public boolean isCancelled(){
        return this == CANCEL;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
