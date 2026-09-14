package com.bi.queryer.ssm.inspection.table.entity;

public class InspectionTableEntity {

    /**
     * 表名
     */
    private String tableName;

    /**
     * 表负责人
     */
    private String tableOwner;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getTableOwner() {
        return tableOwner;
    }

    public void setTableOwner(String tableOwner) {
        this.tableOwner = tableOwner;
    }
}
