package com.bi.queryer.ssm.engine.pivot;

import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.QueryPivotConfig;
import com.bi.queryer.ssm.engine.result.ResultDataSet;

/**
 * @Author: contributor
 * @CreateTime: 2024-05-23  13:35
 */
public class PivotFactory {
    private PivotFactory() {
    }

    public static PivotEngine createPivotEngine(QueryConfigure queryConfig, ResultDataSet resultDataSet, QueryEngine queryEngine) {
        QueryPivotConfig pivotConfig = queryConfig.getResult().getPivotConfig();
        if (pivotConfig.isMeasureOnRow()) {
            return new MeasurePivotEngine(queryConfig, resultDataSet, queryEngine);
        } else {
            return new PivotEngine(queryConfig, resultDataSet, queryEngine);
        }
    }
}
