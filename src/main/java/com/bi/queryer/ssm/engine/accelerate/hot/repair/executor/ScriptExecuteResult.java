package com.bi.queryer.ssm.engine.accelerate.hot.repair.executor;

/**
 * @Author contributor
 * @Date 19:25 2024/10/12
 * @Description TODO
 **/
public class ScriptExecuteResult {
    private String execId;
    private String execInfo;

    private String execStatus;

    private String execEndTime;

    private boolean success = true;

    public String getExecId() {
        return execId;
    }

    public void setExecId(String execId) {
        this.execId = execId;
    }

    public String getExecInfo() {
        return execInfo;
    }

    public void setExecInfo(String execInfo) {
        this.execInfo = execInfo;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getExecStatus() {
        return execStatus;
    }

    public void setExecStatus(String execStatus) {
        this.execStatus = execStatus;
    }

    public String getExecEndTime() {
        return execEndTime;
    }

    public void setExecEndTime(String execEndTime) {
        this.execEndTime = execEndTime;
    }
}
