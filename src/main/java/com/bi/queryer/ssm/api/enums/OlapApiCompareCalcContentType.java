package com.bi.queryer.ssm.api.enums;

import com.bi.queryer.ssm.enums.AnalysisCalcType;

public enum OlapApiCompareCalcContentType {
    DIFF_RATIO("差异率", AnalysisCalcType.RATIO),
    DIFF_VALUE("差值", AnalysisCalcType.VALUE),
    FACT_VALUE("实际值", AnalysisCalcType.REAL_VALUE);

    private String desc;
    private AnalysisCalcType calcType;

    private OlapApiCompareCalcContentType(String desc, AnalysisCalcType calcType) {
        this.desc = desc;
        this.calcType = calcType;
    }

    public static OlapApiCompareCalcContentType get(String code){
        for(OlapApiCompareCalcContentType v : values() ){
            if(v.toString().equalsIgnoreCase(code)){
                return v;
            }
        }
        return DIFF_RATIO;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public AnalysisCalcType getCalcType() {
        return calcType;
    }

    public void setCalcType(AnalysisCalcType calcType) {
        this.calcType = calcType;
    }
}
