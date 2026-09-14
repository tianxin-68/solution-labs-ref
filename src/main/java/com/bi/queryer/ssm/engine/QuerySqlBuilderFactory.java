package com.bi.queryer.ssm.engine;

import com.bi.queryer.ssm.engine.analysis.AnalysisMultiModelSqlBuilder;
import com.bi.queryer.ssm.engine.analysis.TargetAnalysisMultiModelSqlBuilder;
import com.bi.queryer.ssm.engine.analysis.cross.AnalysisCrossDimensionSqlBuilder;
import com.bi.queryer.ssm.engine.analysis.cross.TargetAnalysisCrossDimensionSqlBuilder;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.cross.CrossDimensionSqlBuilder;
import com.bi.queryer.ssm.engine.model.StarModel;

import java.util.List;

/**
 * @Author contributor
 * @Date 21:02 2024-07-24
 * @Description 查询sql构建器工厂类
 **/
public abstract class QuerySqlBuilderFactory {

    /**
     * 明细表sql构建器
     * @return
     */
    public static MultiModelQuerySqlBuilder createSqlBuilder(QueryEngine engine){
        MultiModelQuerySqlBuilder sqlBuilder = null;
        QueryConfigure config = engine.config;
        if(engine.config.hasAnalysis()) {
            if (config.hasTargetAnalysis()) {
                sqlBuilder = new TargetAnalysisMultiModelSqlBuilder(engine.config, engine.cxt, engine.models);
            } else {
                sqlBuilder = new AnalysisMultiModelSqlBuilder(engine.config, engine.cxt, engine.models);
            }
        }else{
            sqlBuilder = new MultiModelQuerySqlBuilder(engine.config, engine.cxt, engine.models);
        }
        return sqlBuilder;
    }

    /**
     * 交叉表sql构建器
     * @param engine
     * @return
     */
    public static CrossDimensionSqlBuilder createCrossSqlBuilder(QueryEngine engine) {
        CrossDimensionSqlBuilder sqlBuilder = null;
        if (engine.config.hasAnalysis()) {
            if (engine.config.hasTargetAnalysis()) {
                sqlBuilder = new TargetAnalysisCrossDimensionSqlBuilder(engine);
            } else {
                sqlBuilder = new AnalysisCrossDimensionSqlBuilder(engine);
            }
        } else {
            sqlBuilder = new CrossDimensionSqlBuilder(engine);
        }
        return sqlBuilder;
    }
}
