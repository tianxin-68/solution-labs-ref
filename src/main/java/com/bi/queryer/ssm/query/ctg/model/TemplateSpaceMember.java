package com.bi.queryer.ssm.query.ctg.model;

import lombok.Data;

@Data
public class TemplateSpaceMember {

    /**
     * 用户名
     */
    private String userName;

    /**
     * 用户姓名
     */
    private String userRealName;

    /**
     * 部门ID
     */
    private String deptId;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 操作权限角色 admin=管理员/member=成员
     */
    private String ownerRole;
}
