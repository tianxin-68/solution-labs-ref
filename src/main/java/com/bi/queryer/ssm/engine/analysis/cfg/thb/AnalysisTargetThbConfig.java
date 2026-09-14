package com.bi.queryer.ssm.engine.analysis.cfg.thb;

import lombok.Data;

import java.util.List;

@Data
public class AnalysisTargetThbConfig {

    private String measureId;

    private List<TargetCalcConfig> targetList;

    @Data
    public static class TargetCalcConfig {
        private String targetCalcMode;
        private String targetCalcType;
    }
}
