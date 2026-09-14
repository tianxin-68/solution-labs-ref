package com.bi.queryer.ssm.changenotice.entity;

import lombok.Data;

/**
 * 变更通知对象明细实体，对应表 mgp_change_notice_object。
 * object_ctg 含 CHANGE/MAPPING/MUST_QUERY 等角色。
 */
@Data
public class ChangeNoticeObject {

    /** 主键 */
    private Long id;
    /** 通知ID */
    private Long noticeId;
    /** 对象角色：CHANGE/MAPPING等 */
    private String objectCtg;
    /** 对象编码 */
    private String objectId;
    /** 对象名称 */
    private String objectName;
    /** 对象类型：METRIC/DIMENSION等 */
    private String objectType;
    /** 替换新对象编码 */
    private String relObjectId;
    /** 替换新对象名称 */
    private String relObjectName;
    /** 替换新对象类型 */
    private String relObjectType;
    /** 排序 */
    private Double sortId;
}
