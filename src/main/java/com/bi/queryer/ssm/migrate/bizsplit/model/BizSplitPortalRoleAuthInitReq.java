package com.bi.queryer.ssm.migrate.bizsplit.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 多维门户角色权限初始化请求（方法3）。
 *
 * 将旧角色 id 下的用户/组织授权复制到新角色 id，excludeDeptIds 命中的用户/组织不写入。
 *
 * excludeDeptIds 须传完整部门 id 列表：不会根据父部门自动展开子部门，需排除的子部门须逐一传入。
 */
@Data
public class BizSplitPortalRoleAuthInitReq {

    /** 新角色 id，写入时替换 role_id */
    private String newRoleId;

    /** 旧角色 id，作为权限查询条件 */
    private String oldRoleId;

    /**
     * 排除的部门 id 集合（须传完整列表，子部门也需逐一传入，不会自动展开）。
     * 用户 HR 部门或组织 dept_id 精确命中则跳过该条记录。
     */
    private List<String> excludeDeptIds = new ArrayList<>();
}
