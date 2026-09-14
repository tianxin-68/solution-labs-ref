package com.bi.queryer.ssm.api.ticket;

public class CtgSensitiveFieldReq {

    /**
     * 目录名称
     */
    private String ctgName;

    /**
     * 工单号
     */
    private String taskId;

    public String getCtgName() {
        return ctgName;
    }

    public void setCtgName(String ctgName) {
        this.ctgName = ctgName;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
}