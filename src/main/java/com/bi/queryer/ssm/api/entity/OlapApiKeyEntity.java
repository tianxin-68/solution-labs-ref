package com.bi.queryer.ssm.api.entity;

import com.bi.queryer.sys.enums.Enabled;

public class OlapApiKeyEntity {

    private String apiKey;

    /**
     * api key 应用平台
     */
    private String apiKeyPlatform;

    /**
     * 是否校验请求头里的用户
     */
    private Integer isCheckHeaderUsername;

    /**
     * 是否校验业务 id权限
     */
    private Integer isCheckBizId;

    /**
     * 业务 id权限校验类型
     */
    private String checkBizIdType;

    /**
     *  最大查询并发数
     */
    private Integer maxQueryConcurrency;

    /**
     * 日期入参是否必填
     */
    private Integer isQueryDateRequired = Enabled.YES.getId();

    /**
     * 是否系统级 key（1=系统级，跳过 hostname 校验）
     */
    private Integer isSystem;

    /**
     * 是否校验内网域名（1=校验请求 baseurl 必须为内网地址）
     */
    private Integer isCheckIntranetDomain;

    /**
     * 是否测试联调场景（仅标记，无运行时逻辑）
     */
    private Integer isTestJoint;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Integer getIsCheckHeaderUsername() {
        return isCheckHeaderUsername;
    }

    public void setIsCheckHeaderUsername(Integer isCheckHeaderUsername) {
        this.isCheckHeaderUsername = isCheckHeaderUsername;
    }

    public Integer getIsCheckBizId() {
        return isCheckBizId;
    }

    public void setIsCheckBizId(Integer isCheckBizId) {
        this.isCheckBizId = isCheckBizId;
    }

    public Integer getMaxQueryConcurrency() {
        return maxQueryConcurrency;
    }

    public void setMaxQueryConcurrency(Integer maxQueryConcurrency) {
        this.maxQueryConcurrency = maxQueryConcurrency;
    }

    public Integer getIsQueryDateRequired() {
        return isQueryDateRequired;
    }

    public void setIsQueryDateRequired(Integer isQueryDateRequired) {
        this.isQueryDateRequired = isQueryDateRequired;
    }

    public String getCheckBizIdType() {
        return checkBizIdType;
    }

    public void setCheckBizIdType(String checkBizIdType) {
        this.checkBizIdType = checkBizIdType;
    }

    public String getApiKeyPlatform() {
        return apiKeyPlatform;
    }

    public void setApiKeyPlatform(String apiKeyPlatform) {
        this.apiKeyPlatform = apiKeyPlatform;
    }

    public Integer getIsSystem() {
        return isSystem;
    }

    public void setIsSystem(Integer isSystem) {
        this.isSystem = isSystem;
    }

    public Integer getIsCheckIntranetDomain() {
        return isCheckIntranetDomain;
    }

    public void setIsCheckIntranetDomain(Integer isCheckIntranetDomain) {
        this.isCheckIntranetDomain = isCheckIntranetDomain;
    }

    public Integer getIsTestJoint() {
        return isTestJoint;
    }

    public void setIsTestJoint(Integer isTestJoint) {
        this.isTestJoint = isTestJoint;
    }
}
