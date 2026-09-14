package com.bi.queryer.ssm.engine.analysis.cfg;

import com.bi.queryer.ssm.enums.AnalysisCalcMode;

public class AnalysisDataSourceCfg {

    /**
     * 分析计算方式
     */
    protected AnalysisCalcMode analysisCalcMode;

    /**
     * 自定义对比的索引
     */
    protected Integer customCompareIndex = -1;

    /**
     * 偏移的日期表达式
     */
    protected String offsetDateExpression;


    public AnalysisCalcMode getAnalysisCalcMode() {
        return analysisCalcMode;
    }

    public void setAnalysisCalcMode(AnalysisCalcMode analysisCalcMode) {
        this.analysisCalcMode = analysisCalcMode;
    }

    public Integer getCustomCompareIndex() {
        return customCompareIndex;
    }

    public void setCustomCompareIndex(Integer customCompareIndex) {
        this.customCompareIndex = customCompareIndex;
    }

    public String getOffsetDateExpression() {
        return offsetDateExpression;
    }

    public void setOffsetDateExpression(String offsetDateExpression) {
        this.offsetDateExpression = offsetDateExpression;
    }
}
