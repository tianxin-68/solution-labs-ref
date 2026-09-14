package com.bi.queryer.ssm.migrate.bizsplit.model;

import java.util.List;

/**
 * 单视图指标膨胀/筛选替换执行结果。
 *
 * 结果判定：
 * errorMessage 非空表示该视图处理失败，批量场景下其他视图仍继续。
 * skipped=true 表示 filter 与指标均未变更且未写影子。
 * skipped=false 表示已成功写入影子，可通过 filterRewritten / metricExpanded 区分具体变更类型。
 *
 * filterRewritten=true 表示 tplConfig.filter 中业务线 values 被改写；
 * metricExpanded=true 表示 datasetId 在配置范围内且完成了指标膨胀。
 */
public class MetricExpansionExecuteRsp {

    /** 视图 id */
    private String viewId;
    /** 模板 id */
    private String tplId;
    /** 正式配置 id（写影子时不改变） */
    private String cfgId;
    /** 影子配置 id；成功写入影子或已存在影子时返回 */
    private String shadowCfgId;
    /** 实际膨胀的 source 编码列表；未做指标膨胀时为 null */
    private List<String> expandedSourceCodes;
    /** 写入影子的指标编码串；仅改筛选时与正式 dtl 相同 */
    private String measureCodes;
    /** 是否因无任何变更而跳过（未写库） */
    private Boolean skipped;
    /** 是否改写了 filter 中 businessline/CGW/BQO 的筛选 values */
    private Boolean filterRewritten;
    /** 是否执行了指标膨胀（datasetId 在 SC.v 配置范围内且命中可膨胀 source） */
    private Boolean metricExpanded;
    /** 单视图失败信息；批量场景下不中断其他视图 */
    private String errorMessage;

    public String getViewId() {
        return viewId;
    }

    public void setViewId(String viewId) {
        this.viewId = viewId;
    }

    public String getTplId() {
        return tplId;
    }

    public void setTplId(String tplId) {
        this.tplId = tplId;
    }

    public String getCfgId() {
        return cfgId;
    }

    public void setCfgId(String cfgId) {
        this.cfgId = cfgId;
    }

    public String getShadowCfgId() {
        return shadowCfgId;
    }

    public void setShadowCfgId(String shadowCfgId) {
        this.shadowCfgId = shadowCfgId;
    }

    public List<String> getExpandedSourceCodes() {
        return expandedSourceCodes;
    }

    public void setExpandedSourceCodes(List<String> expandedSourceCodes) {
        this.expandedSourceCodes = expandedSourceCodes;
    }

    public String getMeasureCodes() {
        return measureCodes;
    }

    public void setMeasureCodes(String measureCodes) {
        this.measureCodes = measureCodes;
    }

    public Boolean getSkipped() {
        return skipped;
    }

    public void setSkipped(Boolean skipped) {
        this.skipped = skipped;
    }

    public Boolean getFilterRewritten() {
        return filterRewritten;
    }

    public void setFilterRewritten(Boolean filterRewritten) {
        this.filterRewritten = filterRewritten;
    }

    public Boolean getMetricExpanded() {
        return metricExpanded;
    }

    public void setMetricExpanded(Boolean metricExpanded) {
        this.metricExpanded = metricExpanded;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
