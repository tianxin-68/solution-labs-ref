package com.bi.queryer.ssm.migrate.bizsplit.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 多维目录权限初始化请求（方法2）。
 *
 * 将旧目录 id 下的权限复制到新目录 id，excludeDeptIds 命中的用户/组织不写入。
 *
 * excludeDeptIds 须传完整部门 id 列表：不会根据父部门自动展开子部门，需排除的子部门须逐一传入。
 */
@Data
public class BizSplitCtgDataAuthInitReq {

    /** 新目录 id，写入时替换 item_code / item_value */
    private String newCtgId;

    /** 旧目录 id，作为权限查询条件 */
    private String oldCtgId;

    /**
     * 排除的部门 id 集合（须传完整列表，子部门也需逐一传入，不会自动展开）。
     * 用户 HR 部门或组织 owner_id 精确命中则跳过该条权限。
     */
    private List<String> excludeDeptIds = new ArrayList<>();
}
