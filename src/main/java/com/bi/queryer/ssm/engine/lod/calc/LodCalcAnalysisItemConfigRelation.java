package com.bi.queryer.ssm.engine.lod.calc;

import com.bi.queryer.ssm.engine.analysis.cfg.compare.AnalysisCompareItemConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.thb.AnalysisThbItemConfig;
import com.bi.queryer.ssm.engine.config.field.QueryField;

/**
 * @Author contributor
 * @Date 15:01 2024-06-18
 * @Description lod计算字段的分析配置：包括同环比、自定义对比
 **/
public class LodCalcAnalysisItemConfigRelation {
    private QueryField lodCalcField = null;

    private AnalysisThbItemConfig thbItemConfig = null;

    private AnalysisCompareItemConfig compareItemConfig = null;

    public LodCalcAnalysisItemConfigRelation(QueryField lodCalcField) {
        this.lodCalcField = lodCalcField;
    }

    public QueryField getLodCalcField() {
        return lodCalcField;
    }

    public void setLodCalcField(QueryField lodCalcField) {
        this.lodCalcField = lodCalcField;
    }

    public AnalysisThbItemConfig getThbItemConfig() {
        return thbItemConfig;
    }

    public void setThbItemConfig(AnalysisThbItemConfig thbItemConfig) {
        this.thbItemConfig = thbItemConfig;
    }

    public AnalysisCompareItemConfig getCompareItemConfig() {
        return compareItemConfig;
    }

    public void setCompareItemConfig(AnalysisCompareItemConfig compareItemConfig) {
        this.compareItemConfig = compareItemConfig;
    }

    public LodCalcAnalysisItemConfigRelation(QueryField lodCalcField, AnalysisThbItemConfig thbItemConfig, AnalysisCompareItemConfig compareItemConfig) {
        this.lodCalcField = lodCalcField;
        this.thbItemConfig = thbItemConfig;
        this.compareItemConfig = compareItemConfig;
    }

    public boolean hasAnaysisConfig(){
        return this.thbItemConfig != null || this.compareItemConfig != null;
    }
}
