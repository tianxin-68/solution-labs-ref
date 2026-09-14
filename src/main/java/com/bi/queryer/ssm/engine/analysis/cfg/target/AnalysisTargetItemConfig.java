package com.bi.queryer.ssm.engine.analysis.cfg.target;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: contributor
 * @CreateTime: 2023-08-22  15:20
 * @Description: 同环比分析配置
 */
public class AnalysisTargetItemConfig {

    private List<String> measureIdList = new ArrayList<>();

    private List<TargetCalcConfig> configs = new ArrayList<>();


    public static class TargetCalcConfig {
        private String calcMode;

        private List<String> calcTypes = new ArrayList<>();

        public String getCalcMode() {
            return calcMode;
        }

        public void setCalcMode(String calcMode) {
            this.calcMode = calcMode;
        }

        public List<String> getCalcTypes() {
            return calcTypes;
        }

        public void setCalcTypes(List<String> calcTypes) {
            this.calcTypes = calcTypes;
        }

        public TargetCalcConfig(String calcMode, List<String> calcTypes) {
            this.calcMode = calcMode;
            this.calcTypes = calcTypes;
        }

        public TargetCalcConfig() {
        }
    }

    public List<String> getMeasureIdList() {
        return measureIdList;
    }

    public void setMeasureIdList(List<String> measureIdList) {
        this.measureIdList = measureIdList;
    }


    public List<TargetCalcConfig> getConfigs() {
        return configs;
    }

    public void setConfigs(List<TargetCalcConfig> configs) {
        this.configs = configs;
    }
}
