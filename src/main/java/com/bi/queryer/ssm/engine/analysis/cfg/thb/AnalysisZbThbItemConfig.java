package com.bi.queryer.ssm.engine.analysis.cfg.thb;

import com.bi.queryer.ssm.engine.analysis.cfg.AnalysisItemConfig;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;

/**
 * 占比同环比的分析配置
 */
public class AnalysisZbThbItemConfig extends AnalysisItemConfig {

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

    /**
     * 获取实际同环比的计算类型
     * @return
     */
    public AnalysisCalcMode getRawThbCalcMode() {
        return AnalysisCalcMode.get(thbCalcMode);
    }

}
