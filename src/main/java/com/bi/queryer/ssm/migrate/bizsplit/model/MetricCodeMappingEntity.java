package com.bi.queryer.ssm.migrate.bizsplit.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 指标字段映射表 {@code ssm_metric_expansion_field_mapping} 对应实体。
 * 看板/查询模板复制到新业务线时，配置里用到的字段 code/id 按
 * {@code (tplId, viewId, sourceBusinessline, targetBusinessline)} 查出该行的
 * {@code oldFieldCode → newFieldCode} / {@code oldFieldId → newFieldId} 做替换。
 * 不使用本表的 {@code source_metric_code}/{@code target_metric_code} 两列
 * （那是旧 {@code ssm_metric_expansion_mapping} 表遗留下来的字段，不作为映射来源）。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MetricCodeMappingEntity {

    private Long pkid;

    private String shadowCfgId;

    private String viewId;

    private String tplId;

    private String cfgId;

    private String sourceBusinessline;

    private String targetBusinessline;

    private String oldFieldId;

    private String oldFieldCode;

    private String oldFieldTitle;

    private String newFieldId;

    private String newFieldCode;

    private String newFieldTitle;
}
