package com.bi.queryer.ssm.migrate.bizsplit.model;

/**
 * 指标膨胀映射表实体，对应表 ssm_metric_expansion_mapping。
 *
 * 字段语义：
 * - source_metric_code：待膨胀/替换的源指标编码
 * - target_metric_code：膨胀后的目标指标编码；同一 source 可有 N 条（N=1 即替换）
 * - source_businessline：源业务线；执行入参必填，每次只处理一个
 * - target_businessline：目标业务线；门户入参可选过滤，未指定时按 owner 的 DEPT_ID 收窄；
 *   同时用于计算字段多依赖变体对齐
 */
public class MetricExpansionMappingEntity {

    /** 主键，决定同一 source 下 target 的展开顺序 */
    private Long pkid;
    /** 源指标编码 */
    private String sourceMetricCode;
    /** 目标指标编码 */
    private String targetMetricCode;
    /** 源业务线（如「保养」），与入参 sourceBusinessline 对应 */
    private String sourceBusinessline;
    /** 目标业务线（如「保养油液」「保养配件」），与入参/owner 过滤及计算字段变体对齐 */
    private String targetBusinessline;

    public Long getPkid() {
        return pkid;
    }

    public void setPkid(Long pkid) {
        this.pkid = pkid;
    }

    public String getSourceMetricCode() {
        return sourceMetricCode;
    }

    public void setSourceMetricCode(String sourceMetricCode) {
        this.sourceMetricCode = sourceMetricCode;
    }

    public String getTargetMetricCode() {
        return targetMetricCode;
    }

    public void setTargetMetricCode(String targetMetricCode) {
        this.targetMetricCode = targetMetricCode;
    }

    public String getSourceBusinessline() {
        return sourceBusinessline;
    }

    public void setSourceBusinessline(String sourceBusinessline) {
        this.sourceBusinessline = sourceBusinessline;
    }

    public String getTargetBusinessline() {
        return targetBusinessline;
    }

    public void setTargetBusinessline(String targetBusinessline) {
        this.targetBusinessline = targetBusinessline;
    }
}
