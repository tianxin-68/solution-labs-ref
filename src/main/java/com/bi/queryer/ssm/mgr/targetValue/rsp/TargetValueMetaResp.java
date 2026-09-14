package com.bi.queryer.ssm.mgr.targetValue.rsp;

import java.util.ArrayList;
import java.util.List;

/**
 * @Auther: contributor
 * @Date: 2025/12/13 11:44
 * @Description:
 */
public class TargetValueMetaResp {
    private String measureCode;

    private String dateType;

    private String dateGranularity;


    private List<String> supportDims = new ArrayList<>();

    private List<String> targetTypes = new ArrayList<>();

    public String getMeasureCode() {
        return measureCode;
    }

    public void setMeasureCode(String measureCode) {
        this.measureCode = measureCode;
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

    public List<String> getSupportDims() {
        return supportDims;
    }

    public void setSupportDims(List<String> supportDims) {
        this.supportDims = supportDims;
    }

    public List<String> getTargetTypes() {
        return targetTypes;
    }

    public void setTargetTypes(List<String> targetTypes) {
        this.targetTypes = targetTypes;
    }
}
