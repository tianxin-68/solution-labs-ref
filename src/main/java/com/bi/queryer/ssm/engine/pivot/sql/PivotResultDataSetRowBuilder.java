package com.bi.queryer.ssm.engine.pivot.sql;

import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.analysis.cross.AnalysisCrossDimensionResultDataSetRowBuilder;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.cross.CrossDimensionResultDataSetRowBuilder;
import com.bi.queryer.ssm.engine.result.ResultDataSetColumn;
import com.bi.queryer.ssm.engine.result.ResultDataSetRowBuilder;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @Author contributor
 * @Date 13:48 2025/6/25
 * @Description TODO
 **/
public class PivotResultDataSetRowBuilder extends ResultDataSetRowBuilder {

    protected ResultDataSetRowBuilder finalRowBuilder = null;

    public PivotResultDataSetRowBuilder(QueryEngine engine) {
        super(engine);
        QueryConfigure config = engine.getConfig();
        if(config.hasAnalysis()){
            finalRowBuilder = new AnalysisCrossDimensionResultDataSetRowBuilder(engine);
        }else {
            finalRowBuilder = new CrossDimensionResultDataSetRowBuilder(engine);
        }
    }

    @Override
    public Map buildRow(ResultSet rs, List<ResultDataSetColumn> columns, Set<String> columnMetaCodes) throws SQLException {
        return finalRowBuilder.buildRow(rs, columns, columnMetaCodes);
    }
}
