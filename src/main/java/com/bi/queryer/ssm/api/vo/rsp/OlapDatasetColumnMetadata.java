package com.bi.queryer.ssm.api.vo.rsp;

/** genAI_feature/olap_api_v2_start */
import java.io.Serializable;

/**
 * 多维 API 数据集列元数据（白皮书列名、类型、释义）
 */
public class OlapDatasetColumnMetadata implements Serializable {

    private static final long serialVersionUID = 1L;

    private String columnName;

    private String columnType;

    private String columnDesc;

    private String aggregationType;

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getColumnType() {
        return columnType;
    }

    public void setColumnType(String columnType) {
        this.columnType = columnType;
    }

    public String getColumnDesc() {
        return columnDesc;
    }

    public void setColumnDesc(String columnDesc) {
        this.columnDesc = columnDesc;
    }

    public String getAggregationType() {
        return aggregationType;
    }

    public void setAggregationType(String aggregationType) {
        this.aggregationType = aggregationType;
    }
}
/** genAI_feature/olap_api_v2_end */
