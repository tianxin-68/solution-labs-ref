package com.bi.queryer.ssm.meta.accelerate.hot;

import com.bi.queryer.ssm.engine.accelerate.hot.etl.model.EtlTableField;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIUtil;

/**
 * @Author contributor
 * @Date 16:57 2024/8/19
 * @Description 热表基础信息表
 **/
public class HotTableInfo implements Cloneable{
    private String hotTableId;

    private String sourceTableName;

    private String hotTableName;

    private String hotDbEngine;

    private Integer hotDataInitDays = 0;

    private String dataMinDate ;

    private String dataMaxDate ;

    private String dataFinishTime;

    /**
     * 最近完成时间：用于记录上一次昨天完成时间，若当天已完成时和dataFinishTime一致
     */
    private String recentDataFinishTime;

    private String etlScriptId;

    private String etlScriptName;

    private String etlJobName;

    private Integer isActive ;

    private String createdBy;

    private String updatedBy;

    private String createdTime;

    private String updatedTime;

    private HotTableSource hotTableSource = new HotTableSource();

    public String getHotTableId() {
        return hotTableId;
    }

    public void setHotTableId(String hotTableId) {
        this.hotTableId = hotTableId;
    }

    public String getSourceTableName() {
        return sourceTableName;
    }

    public void setSourceTableName(String sourceTableName) {
        this.sourceTableName = sourceTableName;
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

    public Integer getHotDataInitDays() {
        return hotDataInitDays;
    }

    public void setHotDataInitDays(Integer hotDataInitDays) {
        this.hotDataInitDays = hotDataInitDays;
    }

    public String getDataMinDate() {
        if(dataMinDate == null) {
            dataMinDate = "";
        }
        return dataMinDate;
    }

    public void setDataMinDate(String dataMinDate) {
        this.dataMinDate = dataMinDate;
    }

    public String getDataMaxDate() {
        if(dataMaxDate == null){
            dataMaxDate = "";
        }
        return dataMaxDate;
    }

    public void setDataMaxDate(String dataMaxDate) {
        this.dataMaxDate = dataMaxDate;
    }

    public String getDataFinishTime() {
        if(dataFinishTime == null){
            dataFinishTime = "";
        }
        return dataFinishTime;
    }

    public void setDataFinishTime(String dataFinishTime) {
        this.dataFinishTime = dataFinishTime;
    }

    public String getEtlJobName() {
        return etlJobName;
    }

    public void setEtlJobName(String etlJobName) {
        this.etlJobName = etlJobName;
    }

    public Integer getIsActive() {
        return isActive;
    }

    public void setIsActive(Integer isActive) {
        this.isActive = isActive;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(String createdTime) {
        this.createdTime = createdTime;
    }

    public String getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(String updatedTime) {
        this.updatedTime = updatedTime;
    }

    public String getEtlScriptId() {
        return etlScriptId;
    }

    public void setEtlScriptId(String etlScriptId) {
        this.etlScriptId = etlScriptId;
    }

    public String getEtlScriptName() {
        return etlScriptName;
    }

    public void setEtlScriptName(String etlScriptName) {
        this.etlScriptName = etlScriptName;
    }

    public HotTableSource getHotTableSource() {
        return hotTableSource;
    }

    public void setHotTableSource(HotTableSource hotTableSource) {
        this.hotTableSource = hotTableSource;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null){
            return false;
        }
        HotTableInfo t = (HotTableInfo) obj;

        boolean isEqual = t.getHotTableId().equalsIgnoreCase(this.hotTableId);
        isEqual = isEqual || (t.getHotTableName().equalsIgnoreCase(this.hotTableName) && t.getHotDbEngine().equalsIgnoreCase(this.hotDbEngine));
        return isEqual;
    }

    @Override
    public int hashCode() {
        return this.hotTableId.hashCode();
    }

    public String getHotTableSchema() {
        if(BIUtil.isEmpty(hotTableName)){
            return "";
        }
        String[] tableInfos = hotTableName.split("\\.");
        String hotTableSchema = tableInfos[0];
        return hotTableSchema;
    }

    public String getHotTableShortName() {
        if(BIUtil.isEmpty(hotTableName)){
            return "";
        }
        String[] tableInfos = hotTableName.split("\\.");
        String hotTableShortName = tableInfos[tableInfos.length - 1];
        return hotTableShortName;
    }

    public HotTableInfo clone(){
        HotTableInfo c = null;
        try {
            c = (HotTableInfo) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
        return c;
    }
    public String getRecentDataFinishTime() {
        if(recentDataFinishTime == null){
            recentDataFinishTime = "";
        }
        return recentDataFinishTime;
    }

    public void setRecentDataFinishTime(String recentDataFinishTime) {
        this.recentDataFinishTime = recentDataFinishTime;
    }

    public boolean isPartitionTable(){
        return this.hotTableSource != null && Enabled.isTrue(this.hotTableSource.getIsNeedPartition());
    }
}
