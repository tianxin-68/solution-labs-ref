package com.bi.queryer.sys.user.vo;

/**
 * @author contributor
 */
public class UserAuth {


    private String userName;

    private String resId;

    private String resType;

    private Integer isAppend;

    /**
     * 权限时长
     */
    private Integer activeDurationDays;

    public String getResId() {
        return resId;
    }

    public void setResId(String resId) {
        this.resId = resId;
    }

    public String getResType() {
        return resType;
    }

    public void setResType(String resType) {
        this.resType = resType;
    }

    public Integer getIsAppend() {
        return isAppend;
    }

    public void setIsAppend(Integer isAppend) {
        this.isAppend = isAppend;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Integer getActiveDurationDays() {
        return activeDurationDays;
    }

    public void setActiveDurationDays(Integer activeDurationDays) {
        this.activeDurationDays = activeDurationDays;
    }
}
