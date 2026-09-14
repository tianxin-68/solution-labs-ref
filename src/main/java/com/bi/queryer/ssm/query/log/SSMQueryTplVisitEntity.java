package com.bi.queryer.ssm.query.log;

/**
 * @Auther: contributor
 * @Date: 2025/12/3 14:26
 * @Description:
 */
public class SSMQueryTplVisitEntity {

    //query_template、analysis_template
    private String tplType;
    private String tplId;
    private String viewId;

    private String env;

    // 树开始加载时间
    private String treeLoadingStartTime;

    // 树结束加载时间
    private String treeLoadingEndTime;

    // 模板开始加载时间
    private String tplLoadingStartTime;

    // 模板结束加载时间
    private String tplLoadingEndTime;

    private String userName;
    private String createdBy;
    private String createdTime;

    public String getTplType() {
        return tplType;
    }

    public void setTplType(String tplType) {
        this.tplType = tplType;
    }

    public String getTplId() {
        return tplId;
    }

    public void setTplId(String tplId) {
        this.tplId = tplId;
    }

    public String getViewId() {
        return viewId;
    }

    public void setViewId(String viewId) {
        this.viewId = viewId;
    }

    public String getTreeLoadingStartTime() {
        return treeLoadingStartTime;
    }

    public void setTreeLoadingStartTime(String treeLoadingStartTime) {
        this.treeLoadingStartTime = treeLoadingStartTime;
    }

    public String getTreeLoadingEndTime() {
        return treeLoadingEndTime;
    }

    public void setTreeLoadingEndTime(String treeLoadingEndTime) {
        this.treeLoadingEndTime = treeLoadingEndTime;
    }

    public String getTplLoadingStartTime() {
        return tplLoadingStartTime;
    }

    public void setTplLoadingStartTime(String tplLoadingStartTime) {
        this.tplLoadingStartTime = tplLoadingStartTime;
    }

    public String getTplLoadingEndTime() {
        return tplLoadingEndTime;
    }

    public void setTplLoadingEndTime(String tplLoadingEndTime) {
        this.tplLoadingEndTime = tplLoadingEndTime;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(String createdTime) {
        this.createdTime = createdTime;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }
}
