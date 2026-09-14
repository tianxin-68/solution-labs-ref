package com.bi.queryer.ssm.portal.auth.entity;

import lombok.Data;

/**
 * @Author: contributor
 * @CreateTime: 2024-06-18  10:52
 * @Description: 角色组织
 */
@Data
public class PortalRoleDept {

    private String roleId;

    private String deptId;

    private String createdBy;

    public PortalRoleDept() {

    }

    public PortalRoleDept(String roleId, String deptId, String createdBy) {
        this.roleId = roleId;
        this.deptId = deptId;
        this.createdBy = createdBy;
    }

}
