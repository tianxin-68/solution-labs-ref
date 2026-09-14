package com.bi.queryer.ssm.mgr.fieldCtg.model;

import lombok.Data;

/**
 * @Author: contributor
 * @CreateTime: 2024-07-23  17:57
 * @Description: 字段目录导出实体
 */
@Data
public class FieldCtgExportEntity {

    /**
     * 目录id
     */
    private String ctgId;

    /**
     * 目录名称
     */
    private String ctgName;

    /**
     * 目录路径
     */
    private String ctgPath;

    /**
     * 字段id
     */
    private String fieldId;

    /**
     * 字段名称
     */
    private String fieldName;

    /**
     * 字段编码
     */
    private String fieldCode;

    /**
     * 字段标题
     */
    private String fieldTitle;

    /**
     * 表id
     */
    private String tableId;

    /**
     * 表名
     */
    private String tableName;

    /**
     * 表owner
     */
    private String tableOwner;
}
