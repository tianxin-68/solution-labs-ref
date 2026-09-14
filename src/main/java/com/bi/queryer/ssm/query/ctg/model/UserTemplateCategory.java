package com.bi.queryer.ssm.query.ctg.model;

/**
 * @Author contributor
 * @Date 22:13 2023-11-14
 * @Description TODO
 **/
public class UserTemplateCategory {
    protected String userName;
    protected String sourceId;
    protected String targetId;

    public UserTemplateCategory(String userName, String sourceId, String targetId) {
        this.userName = userName;
        this.sourceId = sourceId;
        this.targetId = targetId;
    }

    public UserTemplateCategory() {
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }
}
