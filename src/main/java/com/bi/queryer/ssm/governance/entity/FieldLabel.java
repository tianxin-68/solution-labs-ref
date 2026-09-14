package com.bi.queryer.ssm.governance.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 上游指标/维度打标结果（ssm_governance_field_label，每日全量，扫描 Job 数据源）
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FieldLabel {

    /** 自增主键，仅用于扫描分页游标 */
    private Long id;

    /** 目录（前台目录路径） */
    private String ctgPath;

    private String whitePaperCode;

    private String wpName;

    /** 白皮书类型：metric/dim */
    private String wpType;

    private String wpOwner;

    private String wpLevel;

    private String lastVisitTime;

    private Integer daysNoVisit;
}
