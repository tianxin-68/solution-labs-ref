package com.bi.queryer.ssm.engine.interceptor.partition;

/**
 * @Author contributor
 * @Date 13:52 2025/12/9
 * @Description 分区限制条
 **/
public class PartitionLimitTable {
    /**
     * 表名（带库名）
     */
    private String tableName;
    private Integer maxPartitionCount = -1;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public Integer getMaxPartitionCount() {
        return maxPartitionCount;
    }

    public void setMaxPartitionCount(Integer maxPartitionCount) {
        this.maxPartitionCount = maxPartitionCount;
    }
}
