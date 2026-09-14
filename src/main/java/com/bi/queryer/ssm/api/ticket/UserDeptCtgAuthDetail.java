package com.bi.queryer.ssm.api.ticket;

/**
 * 同部门单个用户的目录权限授权情况
 */
public class UserDeptCtgAuthDetail {

    /**
     * 用户域账号
     */
    private String userName;


    /**
     * 权限开始日期
     */
    private String authStartDate;

    /**
     * 权限到期日期，为空表示没有权限
     */
    private String authEndDate;

    /**
     * 权限来源
     */
    private String authSource;

    /**
     * 是否拥有权限
     */
    private Integer hasAuth;


    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getAuthStartDate() {
        return authStartDate;
    }

    public void setAuthStartDate(String authStartDate) {
        this.authStartDate = authStartDate;
    }

    public String getAuthEndDate() {
        return authEndDate;
    }

    public void setAuthEndDate(String authEndDate) {
        this.authEndDate = authEndDate;
    }

    public String getAuthSource() {
        return authSource;
    }

    public void setAuthSource(String authSource) {
        this.authSource = authSource;
    }

    public Integer getHasAuth() {
        return hasAuth;
    }

    public void setHasAuth(Integer hasAuth) {
        this.hasAuth = hasAuth;
    }
}