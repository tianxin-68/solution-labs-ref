package com.bi.queryer.ssm.portal.auth.entity;

import lombok.Data;

/**
 * @Author: contributor
 * @CreateTime: 2024-06-18  10:48
 * @Description: 门户角色人员
 */
@Data
public class PortalRoleUser {

    private String roleId;

    private String userName;

    private String createdBy;

    public PortalRoleUser() {

    }

    public PortalRoleUser(String roleId, String userName, String createdBy) {
        this.roleId = roleId;
        this.userName = userName;
        this.createdBy = createdBy;
    }

}
