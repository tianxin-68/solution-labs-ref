package com.bi.queryer.ssm.meta;

import java.util.List;

/**
 * 实时数据表信息
 */
public class RtTableInfo {

    /**
     * 表全名
     */
    private String tableFullName;

    /**
     * 数据切片粒度
     */
    private String dataSliceGranularity;

    /**
     * 支持的维度字段列表
     */
    private List<String> dimCodeList;

    /**
     * 字段ID
     */
    private String fieldId;

    public String getTableFullName() {
        return tableFullName;
    }

    public void setTableFullName(String tableFullName) {
        this.tableFullName = tableFullName;
    }

    public String getDataSliceGranularity() {
        return dataSliceGranularity;
    }

    public void setDataSliceGranularity(String dataSliceGranularity) {
        this.dataSliceGranularity = dataSliceGranularity;
    }

    public List<String> getDimCodeList() {
        return dimCodeList;
    }

    public void setDimCodeList(List<String> dimCodeList) {
        this.dimCodeList = dimCodeList;
    }

    public String getFieldId() {
        return fieldId;
    }

    public void setFieldId(String fieldId) {
        this.fieldId = fieldId;
    }
}
