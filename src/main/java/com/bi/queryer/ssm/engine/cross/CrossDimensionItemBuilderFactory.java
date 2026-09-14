package com.bi.queryer.ssm.engine.cross;

import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.SingleModelSqlBuilder;
import com.bi.queryer.ssm.engine.analysis.AnalysisSingleModelSqlBuilder;
import com.bi.queryer.ssm.engine.analysis.AnalysisSingleModelSqlBuilderAvgWrapper;
import com.bi.queryer.ssm.engine.analysis.cross.AnalysisCrossDimensionItemBuilder;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;

/**
 * @Author contributor
 * @Date 17:32 2024-06-07
 * @Description 交叉表头构建器工厂类
 **/
public abstract class CrossDimensionItemBuilderFactory {

    public static CrossDimensionItemBuilder createItemBuilder(QueryConfigure config, QueryContext cxt, QueryEngine engine){
        CrossDimensionItemBuilder itemBuilder = new CrossDimensionItemBuilder(config, cxt);
        // 不支持日期过滤参数构建表头，则直接返回
        if(!itemBuilder.canBuildHeaderDataSetByParameter()){
            return itemBuilder;
        }
        if(engine != null && engine.getSqlBuilder() != null && engine.getSqlBuilder().getSingleModelSQLBuilder() != null){
            SingleModelSqlBuilder singleModelSqlBuilder = engine.getSqlBuilder().getSingleModelSQLBuilder();
            AnalysisSingleModelSqlBuilder analysisSingleModelSqlBuilder = null;
            if(singleModelSqlBuilder instanceof AnalysisSingleModelSqlBuilderAvgWrapper){
                AnalysisSingleModelSqlBuilderAvgWrapper avgWrapper = (AnalysisSingleModelSqlBuilderAvgWrapper) singleModelSqlBuilder;
                analysisSingleModelSqlBuilder = avgWrapper.getDefaultAnalysisSingleModelSqlBuilder();
            }
            if(singleModelSqlBuilder instanceof AnalysisSingleModelSqlBuilder){
                analysisSingleModelSqlBuilder = (AnalysisSingleModelSqlBuilder) singleModelSqlBuilder;
            }

            if(analysisSingleModelSqlBuilder == null){
                return itemBuilder;
            }

            AnalysisCalcMode calcMode = analysisSingleModelSqlBuilder.getAnalysisCalcMode();
            if(calcMode == null){
                return itemBuilder;
            }
            AnalysisCrossDimensionItemBuilder analysisItemBuilder = new AnalysisCrossDimensionItemBuilder(config, cxt);
            analysisItemBuilder.setAnalysisCalcMode(analysisSingleModelSqlBuilder.getAnalysisCalcMode());
            analysisItemBuilder.setCustomCompareIndex(analysisSingleModelSqlBuilder.getCustomCompareIndex());

            itemBuilder = analysisItemBuilder;
        }
        return itemBuilder;
    }

}
