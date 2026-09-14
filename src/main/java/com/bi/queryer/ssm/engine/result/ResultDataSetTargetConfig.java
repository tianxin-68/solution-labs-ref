package com.bi.queryer.ssm.engine.result;

import com.bi.queryer.ssm.enums.AnalysisCalcMode;
import com.bi.queryer.ssm.enums.AnalysisCalcType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;

public class ResultDataSetTargetConfig {

    private String rawMeasureCode;

    /**
     * 同环比计算方式
     */
    private String targetCalcMode = AnalysisCalcMode.NONE.getCode();

    /**
     * 占比计算方式
     */
    private String targetCalcType = AnalysisCalcType.UNKNOW.getCode();

    public String getRawMeasureCode() {
        return rawMeasureCode;
    }

    public void setRawMeasureCode(String rawMeasureCode) {
        this.rawMeasureCode = rawMeasureCode;
    }

    public String getTargetCalcMode() {
        return targetCalcMode;
    }

    public void setTargetCalcMode(String targetCalcMode) {
        this.targetCalcMode = targetCalcMode;
    }

    public String getTargetCalcType() {
        return targetCalcType;
    }

    public void setTargetCalcType(String targetCalcType) {
        this.targetCalcType = targetCalcType;
    }

    @JsonIgnore
    public String getTargetCode() {
        String res = "";
        if (StringUtils.isNotBlank(targetCalcMode)) {
            res = "_" + targetCalcMode;
            if (AnalysisCalcType.CONTRIBUTION_RATE.getCode().equals(targetCalcType)) {
                res += "_" + targetCalcType + "_" + AnalysisCalcType.RATIO.getCode();
            } else if (StringUtils.isNotBlank(targetCalcType)) {
                res += "_" + targetCalcType;
            }
        }
        return res;
    }

    public String getTargetTitle() {
        String res = "";
        if (StringUtils.isNotBlank(targetCalcMode)) {
            res = AnalysisCalcMode.get(targetCalcMode).getDesc();
            if (AnalysisCalcMode.TIME_PROGRESS.getCode().equals(targetCalcMode)) {
            } else if (AnalysisCalcType.CONTRIBUTION_RATE.getCode().equals(targetCalcType)) {
                res += "差值贡献率";
            } else if (AnalysisCalcType.REAL_VALUE.getCode().equals(targetCalcType)) {
                res += "值";
            } else if (AnalysisCalcType.VALUE.getCode().equals(targetCalcType)) {
                res += AnalysisCalcType.VALUE.getTitle();
            } else if (AnalysisCalcType.RATIO.getCode().equals(targetCalcType)) {
                res += "达成率";
            } else if (AnalysisCalcType.P_RATIO.getCode().equals(targetCalcType)) {
                res = String.format("预估%s达成率", res);
            }
        }
        return res;
    }

    @JsonIgnore
    public boolean isActive() {
        return StringUtils.isNotBlank(targetCalcMode);
    }
}
