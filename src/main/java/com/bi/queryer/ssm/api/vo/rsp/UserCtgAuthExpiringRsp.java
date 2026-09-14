package com.bi.queryer.ssm.api.vo.rsp;

/**
 * 目录权限临期提醒查询结果
 */
public class UserCtgAuthExpiringRsp {

    /**
     * 用户域账号
     */
    private String userName;

    /**
     * 用户真实姓名
     */
    private String userRealName;

    /**
     * 目录ID
     */
    private String ctgId;

    /**
     * 模块名称全路径（视图 ctg_name_path）
     */
    private String ctgNamePath;

    /**
     * 权限到期日期
     */
    private String authEndDate;

    /**
     * 临期天数（7/3/1）
     */
    private Integer expireDay;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserRealName() {
        return userRealName;
    }

    public void setUserRealName(String userRealName) {
        this.userRealName = userRealName;
    }

    public String getCtgId() {
        return ctgId;
    }

    public void setCtgId(String ctgId) {
        this.ctgId = ctgId;
    }

    public String getCtgNamePath() {
        return ctgNamePath;
    }

    public void setCtgNamePath(String ctgNamePath) {
        this.ctgNamePath = ctgNamePath;
    }

    public String getAuthEndDate() {
        return authEndDate;
    }

    public void setAuthEndDate(String authEndDate) {
        this.authEndDate = authEndDate;
    }

    public Integer getExpireDay() {
        return expireDay;
    }

    public void setExpireDay(Integer expireDay) {
        this.expireDay = expireDay;
    }
}
