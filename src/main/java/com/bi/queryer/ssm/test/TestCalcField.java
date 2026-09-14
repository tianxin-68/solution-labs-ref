package com.bi.queryer.ssm.test;

import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.mgr.fieldDef.model.ManualIndexWhitePaperEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author contributor
 * @Date 17:44 2024/11/21
 * @Description TODO
 **/
public class TestCalcField {

    protected String tableId;

    private String tableName;

    protected String fieldId;

    protected String fieldCode;

    protected String fieldTitle;

    protected String fieldName;

    protected String oldExpression;

    protected String newExpression;

    protected List<String> refIdList = new ArrayList<>();

    // <fieldCode, whitePaperCode>
    protected Map<String, ManualIndexWhitePaperEntity> refWhitePapers = new HashMap<>();

    protected String owner ;

    public TestCalcField() {
    }

    public TestCalcField(MetaField meta) {
        this.fieldId = meta.getId();
        this.fieldName = meta.getName();
        this.fieldCode = meta.getCode();
        this.fieldTitle = meta.getTitle();
        this.owner = meta.getCreatedBy();
        this.oldExpression = meta.getRawAggExpression();
    }

    public TestCalcField(String fieldId, String oldExpression, String newExpression) {
        this.fieldId = fieldId;
        this.oldExpression = oldExpression;
        this.newExpression = newExpression;
    }

    public String getFieldId() {
        return fieldId;
    }

    public void setFieldId(String fieldId) {
        this.fieldId = fieldId;
    }

    public String getOldExpression() {
        return oldExpression;
    }

    public void setOldExpression(String oldExpression) {
        this.oldExpression = oldExpression;
    }

    public String getNewExpression() {
        return newExpression;
    }

    public void setNewExpression(String newExpression) {
        this.newExpression = newExpression;
    }

    @Override
    public String toString() {
        return fieldId + "\t" + oldExpression + "\t" + newExpression;
    }

    public List<String> getRefIdList() {
        return refIdList;
    }

    public void setRefIdList(List<String> refIdList) {
        this.refIdList = refIdList;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getFieldCode() {
        return fieldCode;
    }

    public void setFieldCode(String fieldCode) {
        this.fieldCode = fieldCode;
    }

    public String getFieldTitle() {
        return fieldTitle;
    }

    public void setFieldTitle(String fieldTitle) {
        this.fieldTitle = fieldTitle;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getTableId() {
        return tableId;
    }

    public void setTableId(String tableId) {
        this.tableId = tableId;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public Map<String, ManualIndexWhitePaperEntity> getRefWhitePapers() {
        return refWhitePapers;
    }

    public void setRefWhitePapers(Map<String, ManualIndexWhitePaperEntity> refWhitePapers) {
        this.refWhitePapers = refWhitePapers;
    }
}
