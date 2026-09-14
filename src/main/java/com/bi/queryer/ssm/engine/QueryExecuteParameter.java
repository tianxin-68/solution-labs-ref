package com.bi.queryer.ssm.engine;

import com.bi.queryer.ssm.engine.result.ResultDataSetColumn;
import com.bi.queryer.sys.db.DataSourceType;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author contributor
 * @Date 11:04 2024-05-15
 * @Description 查询执行参数
 **/
public class QueryExecuteParameter {
    /**
     * 可查询限制最大行数，-1=不限制
     */
    private Integer maxRowCount = -1;

    /**
     * 当前查询的sessionId，用于标识唯一参数，也便于kill
     */
    private String sessionId = "";

    /**
     * 查询结果列
     */
    private List<ResultDataSetColumn> columns = null;

    /**
     * 数据源
     */
    private DataSourceType dataSourceType = null;

    public QueryExecuteParameter(){

    }

    public QueryExecuteParameter(Integer maxRowCount) {
        this.maxRowCount = maxRowCount;
    }

    public QueryExecuteParameter(Integer maxRowCount, String sessionId) {
        this.maxRowCount = maxRowCount;
        this.sessionId = sessionId;
    }

    public QueryExecuteParameter(Integer maxRowCount, String sessionId, List<ResultDataSetColumn> columns) {
        this.maxRowCount = maxRowCount;
        this.sessionId = sessionId;
        this.columns = columns;
    }

    public QueryExecuteParameter(Integer maxRowCount, String sessionId, List<ResultDataSetColumn> columns, DataSourceType dataSourceType) {
        this.maxRowCount = maxRowCount;
        this.sessionId = sessionId;
        this.columns = columns;
        this.dataSourceType = dataSourceType;
    }

    public Integer getMaxRowCount() {
        return maxRowCount;
    }

    public void setMaxRowCount(Integer maxRowCount) {
        this.maxRowCount = maxRowCount;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public List<ResultDataSetColumn> getColumns() {
        return columns;
    }

    public void setColumns(List<ResultDataSetColumn> columns) {
        this.columns = columns;
    }

    public DataSourceType getDataSourceType() {
        return dataSourceType;
    }

    public void setDataSourceType(DataSourceType dataSourceType) {
        this.dataSourceType = dataSourceType;
    }
}
