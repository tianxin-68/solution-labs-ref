package com.bi.queryer.ssm.engine.analysis.cfg.thb;

import java.util.List;

/**
 * @Author: contributor
 * @CreateTime: 2023-08-22  15:25
 * @Description: 分析-同环比-时间粒度同环比配置
 */
public class AnalysisThbCalcSettingConfig {

    /**
     * 时间粒度
     */
    private String dateGranularity;

    /**
     * 同环比方式
     */
    private List<String> calcModes;

    public String getDateGranularity() {
        return dateGranularity;
    }

    public void setDateGranularity(String dateGranularity) {
        this.dateGranularity = dateGranularity;
    }

    public List<String> getCalcModes() {
        return calcModes;
    }

    public void setCalcModes(List<String> calcModes) {
        this.calcModes = calcModes;
    }
}
