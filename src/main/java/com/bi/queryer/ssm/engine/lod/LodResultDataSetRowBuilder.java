package com.bi.queryer.ssm.engine.lod;

import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.analysis.AnalysisResultDataSetRowBuilder;

/**
 * @Author contributor
 * @Date 20:37 2024-07-22
 * @Description lod数据集行构建器
 **/
public class LodResultDataSetRowBuilder extends AnalysisResultDataSetRowBuilder {
    public LodResultDataSetRowBuilder(QueryEngine engine) {
        super(engine);
    }
}
