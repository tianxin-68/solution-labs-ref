package com.bi.queryer.sys.user.vo;

public class UserRsp {

    /**
     * 用户名
     */
    private String userName;

    /**
     * 用户姓名
     */
    private String userRealName;

    /**
     * 部门名称
     */
    private String deptName;


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

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

}
