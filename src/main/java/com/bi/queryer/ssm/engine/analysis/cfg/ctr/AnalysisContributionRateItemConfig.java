package com.bi.queryer.ssm.engine.analysis.cfg.ctr;

import com.bi.queryer.ssm.engine.analysis.cfg.AnalysisItemConfig;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;

/**
 * @Author contributor
 * @Date 09:53 2023-09-11
 * @Description 单个贡献率配置项
 **/
public class AnalysisContributionRateItemConfig extends AnalysisItemConfig {
    // 贡献率计算方式
    private String ctrCalcMode = AnalysisCalcMode.NONE.getCode();

    /**
     * 对比项索引，用于区分多个自定义对比
     */
    private Integer compareIndex = 0;

    public Integer getCompareIndex() {
        return compareIndex;
    }

    public void setCompareIndex(Integer compareIndex) {
        this.compareIndex = compareIndex;
    }

    public String getCtrCalcMode() {
        return ctrCalcMode;
    }

    public void setCtrCalcMode(String ctrCalcMode) {
        this.ctrCalcMode = ctrCalcMode;
    }

    /**
     * 获取实际同环比的计算类型
     * @return
     */
    public AnalysisCalcMode getRawThbCalcMode() {
        return AnalysisCalcMode.get(ctrCalcMode);
    }
}
