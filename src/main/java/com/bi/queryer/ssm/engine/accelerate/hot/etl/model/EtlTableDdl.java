package com.bi.queryer.ssm.engine.accelerate.hot.etl.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author contributor
 * @Date 17:26 2024/8/21
 * @Description TODO
 **/
public class EtlTableDdl {
    private List<EtlTableField> commonFields = new ArrayList<>();

    private EtlTableField partitionField = null;

    private String dropHotTableSql = "";

    private String createHotTableSql = "";

    private String sourceTableSql = "";

    private String etlScriptContent = "";

    private String repairScriptContent = "";

    private List<String> logList = new ArrayList<>();

    public List<EtlTableField> getCommonFields() {
        return commonFields;
    }

    public void setCommonFields(List<EtlTableField> commonFields) {
        this.commonFields = commonFields;
    }

    public EtlTableField getPartitionField() {
        return partitionField;
    }

    public void setPartitionField(EtlTableField partitionField) {
        this.partitionField = partitionField;
    }

    public String getDropHotTableSql() {
        return dropHotTableSql;
    }

    public void setDropHotTableSql(String dropHotTableSql) {
        this.dropHotTableSql = dropHotTableSql;
    }

    public String getCreateHotTableSql() {
        return createHotTableSql;
    }

    public void setCreateHotTableSql(String createHotTableSql) {
        this.createHotTableSql = createHotTableSql;
    }

    public String getSourceTableSql() {
        return sourceTableSql;
    }

    public void setSourceTableSql(String sourceTableSql) {
        this.sourceTableSql = sourceTableSql;
    }

    public String getEtlScriptContent() {
        return etlScriptContent;
    }

    public void setEtlScriptContent(String etlScriptContent) {
        this.etlScriptContent = etlScriptContent;
    }

    public String getRepairScriptContent() {
        return repairScriptContent;
    }

    public void setRepairScriptContent(String repairScriptContent) {
        this.repairScriptContent = repairScriptContent;
    }

    public List<String> getLogList() {
        return logList;
    }

    public void setLogList(List<String> logList) {
        this.logList = logList;
    }
}
