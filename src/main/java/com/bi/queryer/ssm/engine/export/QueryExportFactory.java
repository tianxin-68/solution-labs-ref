package com.bi.queryer.ssm.engine.export;

import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.QueryPivotConfig;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.sys.config.SC;

/**
 * @Auther: contributor
 * @Date: 2025/2/24 11:42
 * @Description:
 */
public class QueryExportFactory {
    private QueryExportFactory() {
    }

    public static ExportEngine createQueryExportEngine(QueryEngine queryEngine, int totalSize) {
        int exportLimit = Integer.parseInt(SC.v("ssm.export.pivot.threshold", "1000000"));
        QueryConfigure queryConfig = queryEngine.getConfig();
        QueryPivotConfig pivotConfig = queryConfig.getResult().getPivotConfig();
        if (pivotConfig.needPivot(queryConfig) && totalSize < exportLimit) {
            return new CrossQueryExportEngine(queryEngine);
        } else {
            return new QueryExportEngine(queryEngine);
        }
    }
}
