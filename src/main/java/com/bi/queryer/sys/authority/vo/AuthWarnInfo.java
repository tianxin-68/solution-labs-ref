package com.bi.queryer.sys.authority.vo;

/**
 * @author contributor
 */
public class AuthWarnInfo {

    /**
     * 用户
     */
    private String userName;

    /**
     * 临期天数
     */
    private Integer expireDay;

    /**
     * 到期日期
     */
    private String expireDate;

    /**
     * 报表名
     */
    private String resName;

    /**
     * 菜单路径
     */
    private String menuPath;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Integer getExpireDay() {
        return expireDay;
    }

    public void setExpireDay(Integer expireDay) {
        this.expireDay = expireDay;
    }

    public String getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(String expireDate) {
        this.expireDate = expireDate;
    }

    public String getResName() {
        return resName;
    }

    public void setResName(String resName) {
        this.resName = resName;
    }

    public String getMenuPath() {
        return menuPath;
    }

    public void setMenuPath(String menuPath) {
        this.menuPath = menuPath;
    }
}
