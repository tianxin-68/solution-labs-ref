package com.bi.queryer.ssm.engine.accelerate.hot;

import com.bi.queryer.ssm.meta.accelerate.hot.HotTableInfo;

/**
 * @Author contributor
 * @Date 11:47 2024/8/23
 * @Description TODO
 **/
class HotTableSourceRelation {
    protected String sourceTableName;

    protected HotTableInfo trinoHotTable;

    protected HotTableInfo dorisHotTable;

    public HotTableSourceRelation(String sourceTableName) {
        this.sourceTableName = sourceTableName;
    }

    public String getSourceTableName() {
        return sourceTableName;
    }

    public void setSourceTableName(String sourceTableName) {
        this.sourceTableName = sourceTableName;
    }

    public HotTableInfo getTrinoHotTable() {
        return trinoHotTable;
    }

    public void setTrinoHotTable(HotTableInfo trinoHotTable) {
        this.trinoHotTable = trinoHotTable;
    }

    public HotTableInfo getDorisHotTable() {
        return dorisHotTable;
    }

    public void setDorisHotTable(HotTableInfo dorisHotTable) {
        this.dorisHotTable = dorisHotTable;
    }
}
