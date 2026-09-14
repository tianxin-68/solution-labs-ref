package com.bi.queryer.ssm.governance.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 上游视图打标结果（ssm_governance_view_label，每日全量，扫描 Job 数据源）
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ViewLabel {

    /** 自增主键，仅用于扫描分页游标 */
    private Long id;

    private String viewId;

    private String cfgId;

    private String viewName;

    private String viewType;

    /** 模板类型（如 snapshot/normal 等，包含 snapshot 即为快照） */
    private String tplType;

    private String tplId;

    private String ctgId;

    private String tplName;

    private String tplOwner;

    private String viewOwner;

    private String tplCreatedTime;

    private Integer isPublicDomain;

    private Integer isVipOwner;

    private Integer isPortal;

    private Integer isOwnerAllLeft;

    private Integer daysNoVisit;

    private String lastVisitTime;
}
