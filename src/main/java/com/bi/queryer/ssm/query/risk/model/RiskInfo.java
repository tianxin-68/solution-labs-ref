package com.bi.queryer.ssm.query.risk.model;

import java.util.Date;

/**
 * 风控请求参数
 */
public class RiskInfo {

    /**
     * 用户名
     */
    private String username;

    /**
     * 接入系统名称
     */
    private String clientName;

    /**
     * 接入系统请求url
     */
    private String clientUrl;

    /**
     * 接入系统appid
     */
    private String clientAppId;

    /**
     * 关联接入系统日志id
     */
    private String clientLogId;

    /**
     * 访问/下载
     */
    private String userBehavior;

    /**
     * 数据量
     */
    private Integer dataNumber;

    /**
     * 设备指纹
     */
    private String blackBox;

    /**
     * 操作时间
     */
    private Date operateTime;

    /**
     * 数据内容
     */
    private RiskDataContent dataContent;

    /**
     * 查询客户端
     */
    private String queryClient = "web";

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientUrl() {
        return clientUrl;
    }

    public void setClientUrl(String clientUrl) {
        this.clientUrl = clientUrl;
    }

    public String getClientAppId() {
        return clientAppId;
    }

    public void setClientAppId(String clientAppId) {
        this.clientAppId = clientAppId;
    }

    public String getClientLogId() {
        return clientLogId;
    }

    public void setClientLogId(String clientLogId) {
        this.clientLogId = clientLogId;
    }

    public String getUserBehavior() {
        return userBehavior;
    }

    public void setUserBehavior(String userBehavior) {
        this.userBehavior = userBehavior;
    }

    public Integer getDataNumber() {
        return dataNumber;
    }

    public void setDataNumber(Integer dataNumber) {
        this.dataNumber = dataNumber;
    }

    public String getBlackBox() {
        return blackBox;
    }

    public void setBlackBox(String blackBox) {
        this.blackBox = blackBox;
    }

    public Date getOperateTime() {
        return operateTime;
    }

    public void setOperateTime(Date operateTime) {
        this.operateTime = operateTime;
    }

    public RiskDataContent getDataContent() {
        return dataContent;
    }

    public void setDataContent(RiskDataContent dataContent) {
        this.dataContent = dataContent;
    }

    public String getQueryClient() {
        return queryClient;
    }

    public void setQueryClient(String queryClient) {
        this.queryClient = queryClient;
    }
}
