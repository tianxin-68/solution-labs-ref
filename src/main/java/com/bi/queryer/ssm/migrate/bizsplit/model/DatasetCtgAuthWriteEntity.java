package com.bi.queryer.ssm.migrate.bizsplit.model;

import lombok.Data;

/**
 * 数据集侧多维目录权限写入实体。
 */
@Data
public class DatasetCtgAuthWriteEntity {

    /** 权限主体 id */
    private String ownerId;

    /** 权限主体类型：user / dept */
    private String ownerType;

    /** 权限子项类型 */
    private String itemType;

    /** 目录 id */
    private String itemValue;

    /** 创建人 */
    private String createdBy;
}
