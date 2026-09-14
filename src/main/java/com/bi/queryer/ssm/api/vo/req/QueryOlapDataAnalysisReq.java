package com.bi.queryer.ssm.api.vo.req;

/**
 * @Author contributor
 * @Date 13:50 2026/4/28
 * @Description TODO
 **/
public class QueryOlapDataAnalysisReq {
    private QueryOlapDataAnalysisCompareReq compare = new QueryOlapDataAnalysisCompareReq();

    public boolean isEmpty(){
        return compare == null || compare.isEmpty();
    }

    public QueryOlapDataAnalysisCompareReq getCompare() {
        return compare;
    }

    public void setCompare(QueryOlapDataAnalysisCompareReq compare) {
        this.compare = compare;
    }
}