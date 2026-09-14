package com.bi.queryer.ssm.portal.auth.entity;

import lombok.Data;

/**
 * @Author: contributor
 * @CreateTime: 2024-06-19  09:39
 * @Description: 门户角色权限
 */
@Data
public class PortalRoleAuth {

    private String roleId;

    private String resId;

    private String resType;

    private Integer activeDurationDays;

}
