package com.bi.queryer.ssm.api.ticket;

import java.util.ArrayList;
import java.util.List;

/**
 * 申请人同部门目录权限授权情况
 */
public class UserDeptCtgAuthRsp {

    /**
     * 部门人数
     */
    private Integer deptUserCount;

    /**
     * 授权人数
     */
    private Integer authUserCount;

    /**
     * 授权明细
     */
    private List<UserDeptCtgAuthDetail> authDetailList = new ArrayList<>();


    public Integer getDeptUserCount() {
        return deptUserCount;
    }

    public void setDeptUserCount(Integer deptUserCount) {
        this.deptUserCount = deptUserCount;
    }

    public Integer getAuthUserCount() {
        return authUserCount;
    }

    public void setAuthUserCount(Integer authUserCount) {
        this.authUserCount = authUserCount;
    }

    public List<UserDeptCtgAuthDetail> getAuthDetailList() {
        return authDetailList;
    }

    public void setAuthDetailList(List<UserDeptCtgAuthDetail> authDetailList) {
        this.authDetailList = authDetailList;
    }
}