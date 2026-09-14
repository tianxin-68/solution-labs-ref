package com.bi.queryer.ssm.engine.lod;

import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.analysis.AnalysisEngine;
import com.bi.queryer.ssm.engine.analysis.AnalysisSqlBuilder;
import com.bi.queryer.ssm.engine.config.QueryConfigure;

/**
 * @Author contributor
 * @Date 19:49 2023-12-21
 * @Description 分析引擎查询时，重写sql构建器的创建，便于指定SingleModelBuilder
 **/
public class AnalysisEngineLodWrapper extends AnalysisEngine {
    private LodQueryConfigureItem configureItem = null;
    public AnalysisEngineLodWrapper(QueryConfigure config, QueryContext cxt) {
        super(config, cxt);
    }

    public AnalysisEngineLodWrapper(QueryConfigure config, QueryContext cxt, LodQueryConfigureItem configureItem) {
        this(config, cxt);
        this.configureItem = configureItem;
    }


    @Override
    protected AnalysisSqlBuilder createAnalysisSqlBuilder(QueryConfigure config, QueryContext cxt, QueryEngine rawEngine) {
        AnalysisSqlBuilderLodWrapper sqlBuilder = new AnalysisSqlBuilderLodWrapper(config, cxt, rawEngine, configureItem); //super.createAnalysisSqlBuilder(config, cxt, rawEngine);
        return sqlBuilder;
    }

    public LodQueryConfigureItem getConfigureItem() {
        return configureItem;
    }

    public void setConfigureItem(LodQueryConfigureItem configureItem) {
        this.configureItem = configureItem;
    }
}
