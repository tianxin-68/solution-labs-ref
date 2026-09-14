package com.bi.queryer.ssm.mgr.hot.model;

/**
 * @Author contributor
 * @Date 11:38 2024/8/23
 * @Description 热表请求
 **/
public class HotTableInfoRequest {
    private String hotTableId;

    private String sourceTableName;

    private String hotTableName;

    private String hotDbEngine;

    private String etlJobName;

    public String getSourceTableName() {
        return sourceTableName;
    }

    public void setSourceTableName(String sourceTableName) {
        this.sourceTableName = sourceTableName;
    }

    public String getHotTableId() {
        return hotTableId;
    }

    public void setHotTableId(String hotTableId) {
        this.hotTableId = hotTableId;
    }

    public String getHotTableName() {
        return hotTableName;
    }

    public void setHotTableName(String hotTableName) {
        this.hotTableName = hotTableName;
    }

    public String getHotDbEngine() {
        return hotDbEngine;
    }

    public void setHotDbEngine(String hotDbEngine) {
        this.hotDbEngine = hotDbEngine;
    }

    public String getEtlJobName() {
        return etlJobName;
    }

    public void setEtlJobName(String etlJobName) {
        this.etlJobName = etlJobName;
    }
}
