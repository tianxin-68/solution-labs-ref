package com.bi.queryer.ssm.mgr.targetValue;

import java.util.List;

/**
 * @Author contributor
 * @Date 17:36 2024/12/3
 * @Description TODO
 **/
public class TargetValueRequest {
    private String dateType;

    private String dateGranularity;
    private List<String> measureCodes;

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

    public List<String> getMeasureCodes() {
        return measureCodes;
    }

    public void setMeasureCodes(List<String> measureCodes) {
        this.measureCodes = measureCodes;
    }
}
