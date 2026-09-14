package com.bi.queryer.ssm.migrate.bizsplit.model;

import com.bi.queryer.ssm.meta.MetaField;

/**
 * 单个膨胀目标运行时对象。
 * 在映射表记录基础上挂载已校验的 MetaField，供构建 field JSON 与公式替换使用。
 */
public class MetricExpansionTarget {

    /** 目标指标编码，与映射表 target_metric_code 一致 */
    private String targetMetricCode;
    /** 目标业务线，用于计算字段变体按业务线对齐 */
    private String targetBusinessline;
    /** 已通过存在性/可用性校验的目标指标元数据 */
    private MetaField metaField;

    public String getTargetMetricCode() {
        return targetMetricCode;
    }

    public void setTargetMetricCode(String targetMetricCode) {
        this.targetMetricCode = targetMetricCode;
    }

    public String getTargetBusinessline() {
        return targetBusinessline;
    }

    public void setTargetBusinessline(String targetBusinessline) {
        this.targetBusinessline = targetBusinessline;
    }

    public MetaField getMetaField() {
        return metaField;
    }

    public void setMetaField(MetaField metaField) {
        this.metaField = metaField;
    }
}
