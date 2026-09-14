package com.bi.queryer.ssm.engine.analysis.cfg.total;

import com.bi.queryer.ssm.engine.analysis.cfg.AnalysisItemConfig;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;
import com.bi.queryer.ssm.enums.AnalysisTotalType;
import com.bi.queryer.util.BIUtil;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author contributor
 * @Date 16:05 2023-08-04
 * @Description 统计分析项配置
 **/
public class AnalysisTotalItemConfig extends AnalysisItemConfig {
    /**
     * 统计维度id列表
     */
    protected List<String> dimIdList = new ArrayList<>();

    /**
     * 总计类型
     */
    protected AnalysisTotalType totalType = AnalysisTotalType.NONE;

    /**
     * 是否是占比总计
     */
    protected boolean isZbTotal = false;

    public boolean isActive(){
        AnalysisTotalType analysisTotalType = this.getTotalType();
        return analysisTotalType != null && analysisTotalType.isActive();
    }

    public List<String> getDimIdList() {
        return dimIdList;
    }

    public void setDimIdList(List<String> dimIdList) {
        this.dimIdList = dimIdList;
    }

    public AnalysisTotalType getTotalType() {
        if(totalType != null && !totalType.isActive()){
            AnalysisTotalType ct = AnalysisTotalType.get(AnalysisCalcMode.get(this.calcMode));
            return ct;
        }
        return totalType;
    }

    public void setTotalType(AnalysisTotalType totalType) {
        this.totalType = totalType;
    }

    public boolean isZbTotal() {
        return isZbTotal;
    }

    public void setZbTotal(boolean zbTotal) {
        isZbTotal = zbTotal;
    }

    public AnalysisTotalItemConfig clone(){
        return JSONObject.parseObject(BIUtil.toJSONString(this), this.getClass());
    }
}
