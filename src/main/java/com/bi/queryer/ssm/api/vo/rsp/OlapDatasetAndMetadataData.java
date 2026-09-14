package com.bi.queryer.ssm.api.vo.rsp;

/** genAI_feature/olap_api_v2_start */

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * /olap/api/query/datasetAndMetadata 返回的 data 结构
 */
public class OlapDatasetAndMetadataData implements Serializable {

    private static final long serialVersionUID = 1L;

    private String dataset;

    private List<OlapDatasetColumnMetadata> metadata;

    /**
     * 查询涉及的维度及是否在返回的 dataset 列中（与请求 queryDimensions 或视图行/列维度一致）
     */
    private List<OlapViewDimensionItem> viewDimensions;

    /**
     * 查询涉及的指标及是否在结果中展示（与请求 queryMetrics 或视图指标一致）
     */
    private List<OlapViewMetricItem> viewMetrics;

    /**
     * 构建查询时生效的筛选项（与请求 filters 或视图筛选区有值的字段一致）
     */
    private List<OlapViewFilterItem> viewFilters;

    // 查询视图链接
    private String viewUrl;

    // 查询视图名称
    private String viewName;

    private String templateName;

    /***
     * 数据集最后更新时间
     */
    private String dataLastUpdateTime;

    private Map<String, Object> remark;

    public String getDataset() {
        return dataset;
    }

    public void setDataset(String dataset) {
        this.dataset = dataset;
    }

    public List<OlapDatasetColumnMetadata> getMetadata() {
        return metadata;
    }

    public void setMetadata(List<OlapDatasetColumnMetadata> metadata) {
        this.metadata = metadata;
    }

    public List<OlapViewDimensionItem> getViewDimensions() {
        return viewDimensions;
    }

    public void setViewDimensions(List<OlapViewDimensionItem> viewDimensions) {
        this.viewDimensions = viewDimensions;
    }

    public List<OlapViewMetricItem> getViewMetrics() {
        return viewMetrics;
    }

    public void setViewMetrics(List<OlapViewMetricItem> viewMetrics) {
        this.viewMetrics = viewMetrics;
    }

    public List<OlapViewFilterItem> getViewFilters() {
        return viewFilters;
    }

    public void setViewFilters(List<OlapViewFilterItem> viewFilters) {
        this.viewFilters = viewFilters;
    }

    public Map<String, Object> getRemark() {
        return remark;
    }

    public void setRemark(Map<String, Object> remark) {
        this.remark = remark;
    }

    public String getDataLastUpdateTime() {
        return dataLastUpdateTime;
    }

    public void setDataLastUpdateTime(String dataLastUpdateTime) {
        this.dataLastUpdateTime = dataLastUpdateTime;
    }

    public String getViewUrl() {
        return viewUrl;
    }

    public void setViewUrl(String viewUrl) {
        this.viewUrl = viewUrl;
    }

    public String getViewName() {
        return viewName;
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }
}
/** genAI_feature/olap_api_v2_end */
