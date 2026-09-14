package com.bi.queryer.ssm.api.entity;

/**
 * 用户-机器名映射实体（openapi_user_hostname_mapping）
 */
public class OpenapiUserHostnameMappingEntity {

    private String userName; // 用户名

    private String hostname; // 机器名

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }
}
