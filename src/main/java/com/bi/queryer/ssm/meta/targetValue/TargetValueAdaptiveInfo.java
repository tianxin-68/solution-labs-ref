package com.bi.queryer.ssm.meta.targetValue;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author contributor
 * @Date 18:48 2024/12/3
 * @Description 目标值指标适配信息
 **/
public class TargetValueAdaptiveInfo {

    private TargetTableField measureField;

    private List<String> dateGranularityList = new ArrayList<>();

    private List<TargetTableField> dimensionList = new ArrayList<>();

    public TargetValueAdaptiveInfo() {
    }

    public TargetValueAdaptiveInfo(TargetTableField measureField, List<String> dateGranularityList, List<TargetTableField> dimensionList) {
        this.measureField = measureField;
        this.dateGranularityList = dateGranularityList;
        this.dimensionList = dimensionList;
    }


    public TargetTableField getMeasureField() {
        return measureField;
    }

    public void setMeasureField(TargetTableField measureField) {
        this.measureField = measureField;
    }

    public List<String> getDateGranularityList() {
        return dateGranularityList;
    }

    public void setDateGranularityList(List<String> dateGranularityList) {
        this.dateGranularityList = dateGranularityList;
    }

    public List<TargetTableField> getDimensionList() {
        return dimensionList;
    }

    public void setDimensionList(List<TargetTableField> dimensionList) {
        this.dimensionList = dimensionList;
    }
}
