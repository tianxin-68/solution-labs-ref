package com.bi.queryer.ssm.engine.config;

import com.bi.queryer.ssm.engine.analysis.cfg.compare.AnalysisCompareConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.target.AnalysisTargetConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.thb.AnalysisThbConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.total.AnalysisTotalConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.zb.AnalysisZbConfig;

/**
 * @Author contributor
 * @Date 18:01 2023-07-10
 * @Description 查询分析
 **/
public class QueryAnalysis {

    /**
     * 总计/小计
     */
    protected AnalysisTotalConfig total = new AnalysisTotalConfig();

    protected AnalysisZbConfig zb = new AnalysisZbConfig();

    protected AnalysisThbConfig thb = new AnalysisThbConfig();

    /**
     * 自定义对比
     */
    protected AnalysisCompareConfig compare = new AnalysisCompareConfig();

    /**
     * 贡献率
     */
    //protected AnalysisContributionRateConfig ctr = new AnalysisContributionRateConfig();
    protected AnalysisTargetConfig target = new AnalysisTargetConfig();

    public AnalysisTotalConfig getTotal() {
        return total;
    }

    public void setTotal(AnalysisTotalConfig total) {
        this.total = total;
    }

    public AnalysisZbConfig getZb() {
        return zb;
    }

    public void setZb(AnalysisZbConfig zb) {
        this.zb = zb;
    }

    public AnalysisThbConfig getThb() {
        return thb;
    }

    public void setThb(AnalysisThbConfig thb) {
        this.thb = thb;
    }

    public AnalysisCompareConfig getCompare() {
        return compare;
    }

    public void setCompare(AnalysisCompareConfig compare) {
        this.compare = compare;
    }

//    public AnalysisContributionRateConfig getCtr() {
//        return ctr;
//    }
//
//    public void setCtr(AnalysisContributionRateConfig ctr) {
//        this.ctr = ctr;
//    }

    public AnalysisTargetConfig getTarget() {
        return target;
    }

    public void setTarget(AnalysisTargetConfig target) {
        this.target = target;
    }

    public boolean hasTargetAnalysis(){
        return target != null && target.isActive();
    }
    public boolean isActive(){
        return total.isActive() || zb.isActive() || thb.isActive() || compare.isActive() || target.isActive();
    }
}
