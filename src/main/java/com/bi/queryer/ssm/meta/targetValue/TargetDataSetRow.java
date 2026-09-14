package com.bi.queryer.ssm.meta.targetValue;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author contributor
 * @Date 10:45 2024/12/3
 * @Description 指标数据集行：指标值+多个维度值
 **/
public class TargetDataSetRow {
    private String measureCode;

    private Object measureValue;

    private Map<String, TargetDataSetCell> dimensionValues = new HashMap<>(15);

    public TargetDataSetRow() {
    }

    public TargetDataSetRow(String measureCode, Object measureValue) {
        this.measureCode = measureCode;
        this.measureValue = measureValue;
    }

    public void addDimensionValue(String dimensionCode, String dimensionValue){
        dimensionValues.put(dimensionCode, new TargetDataSetCell(dimensionCode, dimensionValue));
    }

    public String getMeasureCode() {
        return measureCode;
    }

    public void setMeasureCode(String measureCode) {
        this.measureCode = measureCode;
    }

    public Object getMeasureValue() {
        return measureValue;
    }

    public void setMeasureValue(Object measureValue) {
        this.measureValue = measureValue;
    }

    public Map<String, TargetDataSetCell> getDimensionValues() {
        return dimensionValues;
    }

    public void setDimensionValues(Map<String, TargetDataSetCell> dimensionValues) {
        this.dimensionValues = dimensionValues;
    }
}
