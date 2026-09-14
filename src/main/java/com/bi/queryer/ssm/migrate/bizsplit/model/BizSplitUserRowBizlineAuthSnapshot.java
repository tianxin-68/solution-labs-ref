package com.bi.queryer.ssm.migrate.bizsplit.model;

import lombok.Data;

/**
 * 用户业务线行级权限快照（含 HR 部门）。
 * 一条记录对应一个用户的一条有效业务线权限。
 */
@Data
public class BizSplitUserRowBizlineAuthSnapshot {

    /** 用户名 */
    private String userName;

    /** HR 部门路径 DEPT_ID（INNER JOIN 在职员工，必有值） */
    private String deptId;

    /** 业务线 item_value */
    private String itemValue;
}
