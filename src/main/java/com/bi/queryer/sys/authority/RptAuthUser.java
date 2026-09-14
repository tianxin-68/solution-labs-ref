package com.bi.queryer.sys.authority;

/**
 * @author contributor
 */
public class RptAuthUser {

    /**
     * 报表名
     */
    private String rptName;

    /**
     * 报表路径
     */
    private String rptPath;

    /**
     * 域账号
     */
    private String userName;

    /**
     * 真实姓名
     */
    private String userRealName;

    /**
     * 部门
     */
    private String deptName;

    /**
     * 权限名
     */
    private String roleName;

    /**
     * 邮件
     */
    private String email;

    public String getRptName() {
        return rptName;
    }

    public void setRptName(String rptName) {
        this.rptName = rptName;
    }

    public String getRptPath() {
        return rptPath;
    }

    public void setRptPath(String rptPath) {
        this.rptPath = rptPath;
    }

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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
}
