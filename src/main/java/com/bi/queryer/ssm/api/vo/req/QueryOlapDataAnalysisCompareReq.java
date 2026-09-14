package com.bi.queryer.ssm.api.vo.req;

import com.bi.queryer.util.BIUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author contributor
 * @Date 13:51 2026/4/28
 * @Description TODO
 **/
public class QueryOlapDataAnalysisCompareReq {
    private List<String> measures = new ArrayList<>();

    private List<String> calcModes = new ArrayList<>();

    private List<String> calcContents = new ArrayList<>();

    public List<String> getMeasures() {
        return measures;
    }

    public void setMeasures(List<String> measures) {
        this.measures = measures;
    }

    public List<String> getCalcModes() {
        return calcModes;
    }

    public void setCalcModes(List<String> calcModes) {
        this.calcModes = calcModes;
    }

    public List<String> getCalcContents() {
        return calcContents;
    }

    public void setCalcContents(List<String> calcContents) {
        this.calcContents = calcContents;
    }

    public boolean isEmpty() {
        return BIUtil.isEmpty(calcModes);
    }
}
