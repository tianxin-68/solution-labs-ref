package com.bi.queryer.ssm.portal.auth.vo.req;

import lombok.Data;

/**
 * @Author: contributor
 * @CreateTime: 2024-06-18  11:21
 */
@Data
public class PortalAuthBaseReq {

    /**
     * 资源类型id
     */
    private String resId;

    /**
     * 资源类型
     */
    private String resType;

    /**
     * 角色类型 : portal_admin 门户管理员 ,portal_worker 门户协作者,portal_viewer 门户查看者
     */
    private String roleType;
}
