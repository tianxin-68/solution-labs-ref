package com.bi.queryer.ssm.api.entity;

public class OlapApiKeyUserEntity {

    private String apiKey;

    private String userName;

    private Integer maxQueryRowNum;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Integer getMaxQueryRowNum() {
        return maxQueryRowNum;
    }

    public void setMaxQueryRowNum(Integer maxQueryRowNum) {
        this.maxQueryRowNum = maxQueryRowNum;
    }
}
