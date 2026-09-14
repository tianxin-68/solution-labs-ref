package com.bi.queryer.ssm.engine.session;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author contributor
 * @Date 14:05 2022-11-08
 * @Description 查询会话
 **/
public class QuerySession {
    private String sessionId;
    private String queryId;
    private String dsKey;
    private String serverIp;
    private String createdTime;
    private String createdBy;
    private String templateId;
    private String viewId;
    private List<String> tableNames = new ArrayList<>();

    private String olapApiKey;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getQueryId() {
        return queryId;
    }

    public void setQueryId(String queryId) {
        this.queryId = queryId;
    }

    public String getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(String createdTime) {
        this.createdTime = createdTime;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getDsKey() {
        return dsKey;
    }

    public void setDsKey(String dsKey) {
        this.dsKey = dsKey;
    }

    public String getServerIp() {
        return serverIp;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getViewId() {
        return viewId;
    }

    public void setViewId(String viewId) {
        this.viewId = viewId;
    }

    public List<String> getTableNames() {
        return tableNames;
    }

    public void setTableNames(List<String> tableNames) {
        this.tableNames = tableNames;
    }

    public String getOlapApiKey() {
        return olapApiKey;
    }

    public void setOlapApiKey(String olapApiKey) {
        this.olapApiKey = olapApiKey;
    }
}
