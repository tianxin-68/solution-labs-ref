package com.bi.queryer.ssm.api.vo.rsp;

import com.bi.queryer.sys.enums.Enabled;

/**
 * 数据模块权限明细
 */
public class DataCtgAuthDetailRsp {

    /**
     * 用户域账号
     */
    private String userName;

    /**
     * 用户名称
     */
    private String userRealName;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 权限来源  用户工单申请、主动授权、权限继承
     */
    private String authSource;

    /**
     * 权限来源详情
     */
    private String authSourceDetail;

    /**
     * 权限开始时间
     */
    private String authBeginDate;

    /**
     * 权限结束时间
     */
    private String authEndDate;

    /**
     * 工单申请的权限ID
     */
    private String authId = "";

    /**
     * 是否可删除
     */
    private Integer isCanDelete = Enabled.NO.getId();


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

    public String getAuthSource() {
        return authSource;
    }

    public void setAuthSource(String authSource) {
        this.authSource = authSource;
    }

    public String getAuthBeginDate() {
        return authBeginDate;
    }

    public void setAuthBeginDate(String authBeginDate) {
        this.authBeginDate = authBeginDate;
    }

    public String getAuthEndDate() {
        return authEndDate;
    }

    public void setAuthEndDate(String authEndDate) {
        this.authEndDate = authEndDate;
    }

    public String getAuthId() {
        return authId;
    }

    public void setAuthId(String authId) {
        this.authId = authId;
    }

    public Integer getIsCanDelete() {
        return isCanDelete;
    }

    public void setIsCanDelete(Integer isCanDelete) {
        this.isCanDelete = isCanDelete;
    }

    public String getAuthSourceDetail() {
        return authSourceDetail;
    }

    public void setAuthSourceDetail(String authSourceDetail) {
        this.authSourceDetail = authSourceDetail;
    }
}
