package com.bi.queryer.ssm.engine.lod;

import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.SingleModelSqlBuilder;
import com.bi.queryer.ssm.engine.analysis.AnalysisSingleModelSqlBuilder;
import com.bi.queryer.ssm.engine.analysis.AnalysisSingleModelSqlBuilderAvgWrapper;
import com.bi.queryer.ssm.engine.analysis.AnalysisSqlBuilder;
import com.bi.queryer.ssm.engine.config.QueryConfigure;

/**
 * @Author contributor
 * @Date 19:52 2023-12-21
 * @Description 重写sql构建器，便于指定SingleModelBuilder
 **/
public class AnalysisSqlBuilderLodWrapper extends AnalysisSqlBuilder {
    private LodQueryConfigureItem configureItem = null;

    public AnalysisSqlBuilderLodWrapper(QueryConfigure config, QueryContext cxt, QueryEngine queryEngine) {
        super(config, cxt, queryEngine);
    }

    public AnalysisSqlBuilderLodWrapper(QueryConfigure config, QueryContext cxt, QueryEngine queryEngine, LodQueryConfigureItem configureItem) {
        this(config, cxt, queryEngine);
        this.configureItem = configureItem;
    }

    @Override
    protected AnalysisSingleModelSqlBuilder createAnalysisSingleModeSqlBuilder(QueryConfigure cfg, QueryContext _cxt) {
        LodSingleModelSqlBuilder singleModelSqlBuilder = new LodSingleModelSqlBuilder(cfg, _cxt, configureItem); //super.createAnalysisSingleModeSqlBuilder(cfg, _cxt);
        return singleModelSqlBuilder;
    }

    @Override
    protected AnalysisSingleModelSqlBuilderAvgWrapper createAnalysisSingleModeSqlBuilderAvgWrapper(SingleModelSqlBuilder singleModelSqlBuilder) {
        AnalysisSingleModelSqlBuilderAvgWrapper avgWrapper = null;
        if(this.configureItem.getAggExpressionType().isAvgByDay()){
            avgWrapper = new LodSingleModelSqlBuilderAvgWrapper(singleModelSqlBuilder);
        }else {
            avgWrapper = super.createAnalysisSingleModeSqlBuilderAvgWrapper(singleModelSqlBuilder);
        }
        return avgWrapper;
    }

    public LodQueryConfigureItem getConfigureItem() {
        return configureItem;
    }

    public void setConfigureItem(LodQueryConfigureItem configureItem) {
        this.configureItem = configureItem;
    }
}
