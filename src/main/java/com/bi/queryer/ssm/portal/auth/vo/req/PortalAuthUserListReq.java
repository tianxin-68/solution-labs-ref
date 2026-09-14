package com.bi.queryer.ssm.portal.auth.vo.req;

import lombok.Data;

/**
 * 查询有权限的用户清单请求
 */
@Data
public class PortalAuthUserListReq {

    private String resId;

    private String resType;

    /** 关键词：按用户名 / 域账号搜索 */
    private String keyword;

    /** 按部门过滤 */
    private String deptId;

    /**
     * 按授权来源过滤
     * individual=（本层）按个人授权 org=（本层）按组织授权 parent_dir=（从父层）继承父层权限
     */
    private String authSource;

    private Integer pageNum = 1;

    private Integer pageSize = 20;
}
