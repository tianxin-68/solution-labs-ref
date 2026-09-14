package com.bi.queryer.ssm.portal.auth.entity;

import lombok.Data;

/**
 * 文件夹/看板权限继承配置
 * 对应表 ssm_portal_resource_auth_inherit
 */
@Data
public class PortalResourceAuthInherit {

    private Long id;

    /** 所属门户ID，用于按门户批量查询 */
    private String portalId;

    private String resId;

    private String resType;

    /** 是否开启继承 1=开启 0=关闭 */
    private Integer inheritEnabled;

    private String createdBy;

    private String createdTime;

    private String updatedBy;

    private String updatedTime;
}
