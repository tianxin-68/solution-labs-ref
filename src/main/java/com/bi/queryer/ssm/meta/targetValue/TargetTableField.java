package com.bi.queryer.ssm.meta.targetValue;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @Author contributor
 * @Date 17:11 2024/12/2
 * @Description 目标字段
 **/
public class TargetTableField {
    private String tableId;

    private String tableTitle;

    private String tableOwner;

    private String targetType;

    private String fieldType;

    private String dateType;

    private String dateGranularity;

    private String whitePaperCode;

    private String whitePaperName;

    private Integer isPositive;

    public String getTableId() {
        return tableId;
    }

    public void setTableId(String tableId) {
        this.tableId = tableId;
    }

    public String getTableTitle() {
        return tableTitle;
    }

    public void setTableTitle(String tableTitle) {
        this.tableTitle = tableTitle;
    }

    public String getTableOwner() {
        return tableOwner;
    }

    public void setTableOwner(String tableOwner) {
        this.tableOwner = tableOwner;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

    public String getDateType() {
        return dateType;
    }

    public void setDateType(String dateType) {
        this.dateType = dateType;
    }

    public String getDateGranularity() {
        return dateGranularity;
    }

    public void setDateGranularity(String dateGranularity) {
        this.dateGranularity = dateGranularity;
    }

    public String getWhitePaperCode() {
        return whitePaperCode;
    }

    public void setWhitePaperCode(String whitePaperCode) {
        this.whitePaperCode = whitePaperCode;
    }

    public String getWhitePaperName() {
        return whitePaperName;
    }

    public void setWhitePaperName(String whitePaperName) {
        this.whitePaperName = whitePaperName;
    }

    public Integer getIsPositive() {
        return isPositive;
    }

    public void setIsPositive(Integer isPositive) {
        this.isPositive = isPositive;
    }

    public List<String> getTargetTypeList() {
        String[] targetTypes = StringUtils.split(targetType, ",");
        if (targetTypes == null) {
            return new ArrayList<>();
        } else {
            return Arrays.asList(targetTypes);
        }
    }
}
