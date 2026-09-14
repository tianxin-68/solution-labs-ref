package com.bi.queryer.ssm.engine.analysis.cfg;

import com.bi.queryer.ssm.engine.analysis.cfg.ctr.AnalysisContributionRateItemConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.thb.AnalysisZbThbItemConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.total.AnalysisTotalItemConfig;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;

/**
 * @Author contributor
 * @Date 18:23 2023-07-10
 * @Description 查询配置工程类
 **/
public abstract class AnalysisConfigFactory {
    public static AnalysisItemConfig get(String calcMode) {
        AnalysisCalcMode ct = AnalysisCalcMode.get(calcMode);
        if (ct.isTotal()) {
            return new AnalysisTotalItemConfig();
        } else if (AnalysisCalcMode.CONTRIBUTION_RATE == ct) {
            return new AnalysisContributionRateItemConfig();
        } else if(AnalysisCalcMode.ZB_THB == ct){
            return new AnalysisZbThbItemConfig();
        }else{
            return new AnalysisItemConfig();
        }
    }
}
