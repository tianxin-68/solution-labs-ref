package com.bi.queryer.ssm.changenotice.entity;

import lombok.Data;

/**
 * 变更通知筛选条件实体，对应表 mgp_change_notice_object_filter。
 * 既用于匹配时的二次校验，也在使用约束场景作为必须同查筛选返回。
 */
@Data
public class ChangeNoticeObjectFilter {

    /** 主键 */
    private Long id;
    /** 通知ID */
    private Long noticeId;
    /** 对象角色，固定 FILTER */
    private String objectCtg;
    /** 筛选对象编码 */
    private String objectId;
    /** 筛选对象名称 */
    private String objectName;
    /** 筛选对象类型：METRIC/DIMENSION */
    private String objectType;
    /** 筛选值，逗号分隔 */
    private String filterValues;
    /** 过滤方式：include/exclude */
    private String filterType;
    /** 筛选方式：detail/agg */
    private String filterValueMode;
}
