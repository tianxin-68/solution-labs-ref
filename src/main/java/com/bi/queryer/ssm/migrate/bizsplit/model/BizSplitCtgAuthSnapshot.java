package com.bi.queryer.ssm.migrate.bizsplit.model;

import lombok.Data;

/**
 * 多维目录权限快照，统一承载 Portal 与数据集侧查询结果。
 */
@Data
public class BizSplitCtgAuthSnapshot {

    /** 权限主体 id：用户域账号或部门 id */
    private String ownerId;

    /** 权限主体类型 */
    private String ownerType;

    /** 数据集侧 item_type，Portal 来源为空 */
    private String itemType;

    /** 目录 id（旧） */
    private String itemValue;

    /** 目录名称（Portal 来源） */
    private String itemName;

    /** 创建人 */
    private String createdBy;

    /** 权限有效期（天，Portal 来源） */
    private Integer activeDurationDays;

    /** 来源：portal / dataset */
    private String source;
}
