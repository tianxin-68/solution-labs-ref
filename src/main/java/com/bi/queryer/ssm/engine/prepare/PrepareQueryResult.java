package com.bi.queryer.ssm.engine.prepare;

import com.bi.queryer.ssm.engine.result.ResultDataSet;

/**
 * @Author contributor
 * @Date 13:45 2025/10/23
 * @Description 预查询结果
 **/
public class PrepareQueryResult {
    private ResultDataSet crossDimensionItemDataSet;

    public ResultDataSet getCrossDimensionItemDataSet() {
        return crossDimensionItemDataSet;
    }

    public void setCrossDimensionItemDataSet(ResultDataSet crossDimensionItemDataSet) {
        this.crossDimensionItemDataSet = crossDimensionItemDataSet;
    }
}
