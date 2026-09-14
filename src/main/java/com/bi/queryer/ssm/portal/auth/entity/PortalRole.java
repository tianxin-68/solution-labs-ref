package com.bi.queryer.ssm.portal.auth.entity;

import lombok.Data;

/**
 * @Author: contributor
 * @CreateTime: 2024-06-17  20:13
 * @Description: 门户角色
 */
@Data
public class PortalRole {

    /**
     * 角色id
     */
    private String roleId;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 角色类型
     */
    private String roleType;

    /**
     * 门户id
     */
    private String portalId;

    /**
     * 创建人
     */
    private String createdBy;

}
