package com.bi.queryer.ssm.engine.result;

import com.bi.queryer.ssm.enums.AnalysisCalcMode;

public class ResultDataSetZbThbConfig {

    /**
     * 同环比计算方式
     */
    private String thbCalcMode = AnalysisCalcMode.NONE.getCode();

    /**
     * 占比计算方式
     */
    private String zbCalcMode = AnalysisCalcMode.NONE.getCode();


    public String getThbCalcMode() {
        return thbCalcMode;
    }

    public void setThbCalcMode(String thbCalcMode) {
        this.thbCalcMode = thbCalcMode;
    }

    public String getZbCalcMode() {
        return zbCalcMode;
    }

    public void setZbCalcMode(String zbCalcMode) {
        this.zbCalcMode = zbCalcMode;
    }

}
