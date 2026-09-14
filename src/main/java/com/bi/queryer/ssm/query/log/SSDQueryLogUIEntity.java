package com.bi.queryer.ssm.query.log;

/**
 * 前端查询日志
 */
public class SSDQueryLogUIEntity {

    /**
     * 查询会话id
     */
    private String sessionId;

    /**
     * 查询过滤内容
     */
    private String queryFilter;

    /**
     * 查询用户
     */
    private String userName;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getQueryFilter() {
        return queryFilter;
    }

    public void setQueryFilter(String queryFilter) {
        this.queryFilter = queryFilter;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
