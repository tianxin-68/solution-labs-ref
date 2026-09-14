package com.bi.queryer.ssm.engine.result;

import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.util.BIConsts;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @Author contributor
 * @Date 20:27 2024-07-22
 * @Description 结果数据集行行构建器
 **/
public class ResultDataSetRowBuilder {
    protected QueryEngine engine;
    public ResultDataSetRowBuilder(QueryEngine engine){
        this.engine = engine;
    }

    public Map buildRow(ResultSet rs, List<ResultDataSetColumn> columns, Set<String> columnMetaCodes) throws SQLException {
        Map row = new HashMap(32);
        for (ResultDataSetColumn column : columns) {
            Object value = columnMetaCodes.contains(column.getCode()) ? rs.getObject(column.getCode()) : null;
            Object rawValue = value;
            value = column.isHasAuth() ? this.engine.formatValue(value, column.getDataType(), column.getDataFormat(), column.getRawCode()) : BIConsts.NO_AUTH_CONTENT;
            row.put(column.getCode(), value);
            if(this.engine.getConfig().getSettings().getQueryRawValue()){
                row.put(column.getCode() + BIConsts.RAW_VALUE_COLUMN_CODE_SUFFIX, rawValue);
            }
        }
        /**
         // 废弃，移到分析结果行构建器中
         if (hasAnalysis) {
         row.put(BIConsts.GROUPING_VALUE, metaColMap.contains(BIConsts.GROUPING_VALUE) ? rs.getObject(BIConsts.GROUPING_VALUE) : null);
         row.put(BIConsts.GROUPING_KEY, metaColMap.contains(BIConsts.GROUPING_KEY) ? rs.getObject(BIConsts.GROUPING_KEY) : null);
         }
         */
        return row;
    }
}
