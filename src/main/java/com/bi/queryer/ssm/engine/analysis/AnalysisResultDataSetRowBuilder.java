package com.bi.queryer.ssm.engine.analysis;

import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.result.ResultDataSetColumn;
import com.bi.queryer.ssm.engine.result.ResultDataSetRowBuilder;
import com.bi.queryer.ssm.enums.AnalysisTotalType;
import com.bi.queryer.util.BIConsts;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @Author contributor
 * @Date 20:35 2024-07-22
 * @Description 分析结果集行构建器
 **/
public class AnalysisResultDataSetRowBuilder extends ResultDataSetRowBuilder {
    public AnalysisResultDataSetRowBuilder(QueryEngine engine) {
        super(engine);
    }

    @Override
    public Map buildRow(ResultSet rs, List<ResultDataSetColumn> columns, Set<String> columnMetaCodes) throws SQLException {
        Map row = new HashMap(32);

        // 添加分析所需key字段
        row.put(BIConsts.GROUPING_VALUE, columnMetaCodes.contains(BIConsts.GROUPING_VALUE) ? rs.getObject(BIConsts.GROUPING_VALUE) : null);
        row.put(BIConsts.GROUPING_KEY, columnMetaCodes.contains(BIConsts.GROUPING_KEY) ? rs.getObject(BIConsts.GROUPING_KEY) : null);

        boolean totalValueConverted = false;
        for (ResultDataSetColumn column : columns) {
            Object value = columnMetaCodes.contains(column.getCode()) ? rs.getObject(column.getCode()) : null;
            Object rawValue = value;
            // 处理小计/总计
            if(value == null && row.get(BIConsts.GROUPING_VALUE) != null && !totalValueConverted){
                value = column.isHasAuth() ? AnalysisTotalType.get(row.get(BIConsts.GROUPING_VALUE) + "").getDesc() + "_" + column.getTotalAggTypeDesc() : BIConsts.NO_AUTH_CONTENT;
                totalValueConverted = true;
            }else {
                value = column.isHasAuth() ? this.engine.formatValue(value, column.getDataType(), column.getDataFormat(), column.getRawCode()) : BIConsts.NO_AUTH_CONTENT;
            }
            row.put(column.getCode(), value);
            if(this.engine.getConfig().getSettings().getQueryRawValue()){
                row.put(column.getCode() + BIConsts.RAW_VALUE_COLUMN_CODE_SUFFIX, rawValue);
            }
        }
        return row;
    }
}
