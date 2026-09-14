package com.bi.queryer.ssm.engine.config;

import cn.hutool.core.collection.CollUtil;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.util.SSDUtil;

import java.util.ArrayList;
import java.util.List;

public class QueryPivotConfig {
    private boolean isMeasureOnRow;
    /**
     * 需转置的列维度
     */
    private final List<QueryField> colDimensions = new ArrayList<>(8);

    /**
     * 列维度的值
     */
    private List<String> colDimValues = new ArrayList<>();

    public boolean isMeasureOnRow() {
        return isMeasureOnRow;
    }

    public QueryPivotConfig(boolean isMeasureOnRow) {
        this.isMeasureOnRow = isMeasureOnRow;
    }

    public void setMeasureOnRow(boolean measureOnRow) {
        isMeasureOnRow = measureOnRow;
    }

    public void addColDimension(QueryField field) {
        colDimensions.add(field);
    }

    public List<QueryField> getColDimensions() {
        return colDimensions;
    }

    public boolean needPivot(QueryConfigure queryConfig) {
        return isMeasureOnRow || CollUtil.isNotEmpty(colDimensions);
    }

    public List<String> getColDimValues() {
        return colDimValues;
    }

    public void setColDimValues(List<String> colDimValues) {
        this.colDimValues = colDimValues;
    }
}
