package com.bi.queryer.ssm.api.vo.rsp;

/** genAI_feature/olap_api_v2_start */

import java.io.Serializable;

/**
 * datasetAndMetadata 返回的查询筛选项（构建查询时生效的筛选字段）
 */
public class OlapViewFilterItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private String columnName;

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }
}
/** genAI_feature/olap_api_v2_end */
