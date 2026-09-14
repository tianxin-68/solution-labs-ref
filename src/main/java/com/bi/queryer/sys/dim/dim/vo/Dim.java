package com.bi.queryer.sys.dim.dim.vo;

import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.JSONSerializable;
import com.alibaba.fastjson.JSONObject;

/**
 * @author contributor
 */
public class Dim implements JSONSerializable {

    private String cfgId;

    /**
     * 模块编码
     */
    private String moduleCode;

    /**
     * 模块名称
     */
    private String moduleName;

    /**
     * 维度编码
     */
    private String dimCode;

    /**
     * 维度名称
     */
    private String dimName;

    /**
     * 维度表名（带scheam）
     */
    private String dimTableName;

    /**
     * 维度表sql
     */
    private String dimTableSql;

    /**
     * 维度编码列字段名
     */
    private String codeFieldName;

    /**
     * 维度值列字段名
     */
    private String valueFieldName;

    /**
     * 上级维度编码列字段名
     */
    private String parentCodeFieldName;

    /**
     * 跟节点值
     */
    private String rootValue;

    /**
     * 数据源
     */
    private String dimDatasource;

    /**
     * 是否可用
     */
    private Integer isActive;

    /**
     * 创建时间
     */
    private String createdTime;

    /**
     * 修改时间
     */
    private String updatedTime;

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 修改人
     */
    private String updatedBy;

    /**
     * 维度数据api
     */
    private String dimDataApi;

    public String getCfgId() {
        return cfgId;
    }

    public void setCfgId(String cfgId) {
        this.cfgId = cfgId;
    }

    public String getModuleCode() {
        return moduleCode;
    }

    public void setModuleCode(String moduleCode) {
        this.moduleCode = moduleCode;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getDimCode() {
        return dimCode;
    }

    public void setDimCode(String dimCode) {
        this.dimCode = dimCode;
    }

    public String getDimName() {
        return dimName;
    }

    public void setDimName(String dimName) {
        this.dimName = dimName;
    }

    public String getDimTableName() {
        return dimTableName;
    }

    public void setDimTableName(String dimTableName) {
        this.dimTableName = dimTableName;
    }

    public String getCodeFieldName() {
        return codeFieldName;
    }

    public void setCodeFieldName(String codeFieldName) {
        this.codeFieldName = codeFieldName;
    }

    public String getValueFieldName() {
        return valueFieldName;
    }

    public void setValueFieldName(String valueFieldName) {
        this.valueFieldName = valueFieldName;
    }

    public String getParentCodeFieldName() {
        return parentCodeFieldName;
    }

    public void setParentCodeFieldName(String parentCodeFieldName) {
        this.parentCodeFieldName = parentCodeFieldName;
    }

    public Integer getIsActive() {
        return isActive;
    }

    public void setIsActive(Integer isActive) {
        this.isActive = isActive;
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

    public String getRootValue() {
        return rootValue;
    }

    public void setRootValue(String rootValue) {
        this.rootValue = rootValue;
    }

    public String getDimDatasource() {
        return dimDatasource;
    }

    public void setDimDatasource(String dimDatasource) {
        this.dimDatasource = dimDatasource;
    }

    public String getDimTableSql() {
        return dimTableSql;
    }

    public void setDimTableSql(String dimTableSql) {
        this.dimTableSql = dimTableSql;
    }

    public String getDimDataApi() {
        return dimDataApi;
    }

    public void setDimDataApi(String dimDataApi) {
        this.dimDataApi = dimDataApi;
    }

    @Override
    public JSONObject toJSON() {
        return BIUtil.toJSONObject(this);
    }
}
