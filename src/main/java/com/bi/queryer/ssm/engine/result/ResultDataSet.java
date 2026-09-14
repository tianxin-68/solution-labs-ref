package com.bi.queryer.ssm.engine.result;

import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author contributor
 * @Date 15:20 2022-10-20
 * @Description 数据集
 **/
public class ResultDataSet implements Serializable {
    private String sessionId;
    private List<Map<String, Object>> rows = new ArrayList<>();
    private List<List<Object>> rowList = new ArrayList<>();

    private Map<String, Integer> columnIndex = new HashMap<>();

    private List<ResultDataSetColumn> columns = new ArrayList<>();

    private Map<String, Object> properties = new HashMap<>();

    private Integer size = 0;

    private Integer totalSize = 0;

    private String logId;

    /**
     * 查询使用的表名集合
     */
    private List<String> queryTableNames = new ArrayList<>();

    /**
     * 数据更新时间
     */
    private String dataUpdateTime;

    /**
     * 是否是实时数据集
     */
    private Integer isRtDataSet = Enabled.NO.getId();

    /**
     * csv内容
     */
    private String csvContent;

    /**
     * csv数据集下载地址
     */
    private String csvDatasetUrl;

    public List<Map<String, Object>> getRows() {
        return rows;
    }

    public void setRows(List<Map<String, Object>> rows) {
        this.rows = rows;
    }

    public List<List<Object>> getRowList() {
        return rowList;
    }

    public void setRowList(List<List<Object>> rowList) {
        this.rowList = rowList;
    }

    public Map<String, Integer> getColumnIndex() {
        return columnIndex;
    }

    public void setColumnIndex(Map<String, Integer> columnIndex) {
        this.columnIndex = columnIndex;
    }

    public List<ResultDataSetColumn> getColumns() {
        return columns;
    }

    public void setColumns(List<ResultDataSetColumn> columns) {
        this.columns = columns;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public Integer getSize() {
        if (BIUtil.isNotEmpty(rows)) {
            size = rows.size();
        } else if (BIUtil.isNotEmpty(rowList)) {
            size = rowList.size();
        }
        return size;
    }

    public Integer getColumnSize() {
        if (rows != null && rows.size() > 0) {
            Map<String, Object> rowData = rows.get(0);
            if (rowData != null) {
                return rowData.size();
            }
        } else if (rowList != null && rowList.size() > 0) {
            List<Object> rowData = rowList.get(0);
            if (rowData != null) {
                return rowData.size();
            }
        }
        return null;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Integer getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(Integer totalSize) {
        this.totalSize = totalSize;
    }

    public ResultDataSet addRow(Map<String, Object> row){
        this.rows.add(row);
        return this;
    }

    public ResultDataSet addColumn(ResultDataSetColumn column) {
        this.columns.add(column);
        return this;
    }

    public void clear() {
        if (this.columns != null) {
            this.columns.clear();
        }
        if (this.rows != null) {
            this.rows.clear();
        }
        if (this.rowList != null) {
            this.rowList.clear();
        }
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getLogId() {
        return logId;
    }

    public void setLogId(String logId) {
        this.logId = logId;
    }

    public List<String> getQueryTableNames() {
        return queryTableNames;
    }

    public void setQueryTableNames(List<String> queryTableNames) {
        this.queryTableNames = queryTableNames;
    }

    public String getDataUpdateTime() {
        return dataUpdateTime;
    }

    public void setDataUpdateTime(String dataUpdateTime) {
        this.dataUpdateTime = dataUpdateTime;
    }

    public Integer getIsRtDataSet() {
        return isRtDataSet;
    }

    public void setIsRtDataSet(Integer isRtDataSet) {
        this.isRtDataSet = isRtDataSet;
    }

    public String getCsvDatasetUrl() {
        return csvDatasetUrl;
    }

    public void setCsvDatasetUrl(String csvDatasetUrl) {
        this.csvDatasetUrl = csvDatasetUrl;
    }

    public boolean isEmpty(){
        return BIUtil.isEmpty(columns) || (BIUtil.isEmpty(rows));
    }

    public String getCsvContent() {
        return csvContent;
    }

    public void setCsvContent(String csvContent) {
        this.csvContent = csvContent;
    }
}
