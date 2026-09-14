package com.bi.queryer.ssm.query.template.model;

import lombok.Data;

/**
 * 部门实体类
 */
@Data
public class DeptEntity {

    /**
     * 部门ID
     */
    private String deptId;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 部门路径
     */
    private String deptPathName;
}
