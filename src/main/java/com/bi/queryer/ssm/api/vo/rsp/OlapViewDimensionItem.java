package com.bi.queryer.ssm.api.vo.rsp;

/** genAI_feature/olap_api_v2_start */

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * datasetAndMetadata 返回的查询维度是否在结果集中
 */
public class OlapViewDimensionItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private String columnName;

    private boolean isInDataSet;

    private String sensitiveLevel;

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    @JsonProperty("isInDataSet")
    public boolean isInDataSet() {
        return isInDataSet;
    }

    public void setInDataSet(boolean inDataSet) {
        isInDataSet = inDataSet;
    }

    public String getSensitiveLevel() {
        return sensitiveLevel;
    }

    public void setSensitiveLevel(String sensitiveLevel) {
        this.sensitiveLevel = sensitiveLevel;
    }
}
/** genAI_feature/olap_api_v2_end */
