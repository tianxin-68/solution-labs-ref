package com.bi.queryer.ssm.engine;


import com.bi.queryer.ssm.engine.analysis.AnalysisEngine;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.cross.CrossDimensionQueryEngine;
import com.bi.queryer.ssm.engine.lod.LodQueryEngine;
import com.bi.queryer.ssm.engine.pivot.sql.PivotQueryEngine;
import com.bi.queryer.ssm.engine.prepare.PrepareQueryEngine;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.util.BIUtil;

import java.util.List;

public class QueryFactory {

    /**
     * 创建查询引擎
     * @param config
     * @param cxt
     * @return
     */
    public static QueryEngine createEngine(QueryConfigure config, QueryContext cxt) {
        // 走sql转置：当有指标排序时，需要通过sql转置查询不能走java转置，避免数据丢失
        boolean isQueryPivot = SSDUtil.isQueryPivot(config);
        if(isQueryPivot){
            return new PivotQueryEngine(config, cxt);
        }

        List<QueryField> lodFields = config.getLodFields();

        boolean isCrossQuery = BIUtil.isNotEmpty(config.getResult().getColDimensions());

        // lod查询引擎
        if(config != null && BIUtil.isNotEmpty(lodFields)) {
            return new LodQueryEngine(config, cxt);
        }

        if(BIUtil.isNotEmpty(lodFields)){
            // 从查询配置中删除这些字段
            config.getResult().getFields().removeAll(lodFields);
            config.getResult().getMeasures().removeAll(lodFields);
        }

        // 分析查询引擎
        if(config != null && config.hasAnalysis()) {
            return new AnalysisEngine(config, cxt);
        }

        // 列维度查询引擎
        if(config != null && isCrossQuery) {
            return new CrossDimensionQueryEngine(config, cxt);
        }

        // 普通查询引擎
        return new QueryEngine(config, cxt);
    }

    public static PrepareQueryEngine createPrepareEngine(QueryConfigure config, QueryContext cxt){
        PrepareQueryEngine engine = new PrepareQueryEngine(config, cxt);
        return engine;
    }

}
