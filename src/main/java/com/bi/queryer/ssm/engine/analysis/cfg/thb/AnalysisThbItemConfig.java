package com.bi.queryer.ssm.engine.analysis.cfg.thb;

import com.bi.queryer.ssm.engine.analysis.cfg.ctr.AnalysisContributionRateConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: contributor
 * @CreateTime: 2023-08-22  15:20
 * @Description: 同环比分析配置
 */
public class AnalysisThbItemConfig {

    private List<String> measureIdList = new ArrayList<>();

    private List<String> calcModes = new ArrayList<>();

    private List<String> calcTypes = new ArrayList<>();

    private AnalysisContributionRateConfig ctr = new AnalysisContributionRateConfig();

    /**
     * 占比同环比配置
     */
    private List<AnalysisZbThbConfig> zbThbConfigs = new ArrayList<>();

    /**
     * 目标值同环比配置
     */
    private List<AnalysisTargetThbConfig> targetThbConfigs = new ArrayList<>();

    private String percentFieldRatioUnit;

    public List<String> getMeasureIdList() {
        return measureIdList;
    }

    public void setMeasureIdList(List<String> measureIdList) {
        this.measureIdList = measureIdList;
    }

    public List<String> getCalcTypes() {
        return calcTypes;
    }

    public void setCalcTypes(List<String> calcTypes) {
        this.calcTypes = calcTypes;
    }

    public String getPercentFieldRatioUnit() {
        return percentFieldRatioUnit;
    }

    public void setPercentFieldRatioUnit(String percentFieldRatioUnit) {
        this.percentFieldRatioUnit = percentFieldRatioUnit;
    }

    public List<String> getCalcModes() {
        return calcModes;
    }

    public void setCalcModes(List<String> calcModes) {
        this.calcModes = calcModes;
    }

    public AnalysisContributionRateConfig getCtr() {
        return ctr;
    }

    public void setCtr(AnalysisContributionRateConfig ctr) {
        this.ctr = ctr;
    }

    public List<AnalysisZbThbConfig> getZbThbConfigs() {
        return zbThbConfigs;
    }

    public void setZbThbConfigs(List<AnalysisZbThbConfig> zbThbConfigs) {
        this.zbThbConfigs = zbThbConfigs;
    }

    public List<AnalysisTargetThbConfig> getTargetThbConfigs() {
        return targetThbConfigs;
    }

    public void setTargetThbConfigs(List<AnalysisTargetThbConfig> targetThbConfigs) {
        this.targetThbConfigs = targetThbConfigs;
    }
}
