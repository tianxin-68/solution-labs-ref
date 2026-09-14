package com.bi.queryer.ssm.portal.auth.entity;

import lombok.Data;

/**
 * @Author: contributor
 * @CreateTime: 2024-06-19  14:55
 * @Description: 角色功能权限
 */
@Data
public class PortalRoleFunc {

    /**
     * 角色id
     */
    private String roleId;

    /**
     * 功能编码
     */
    private String funcCode;
}
