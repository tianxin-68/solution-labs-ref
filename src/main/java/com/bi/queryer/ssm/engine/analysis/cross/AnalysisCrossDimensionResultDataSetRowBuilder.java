package com.bi.queryer.ssm.engine.analysis.cross;

import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.cross.CrossDimensionResultDataSetRowBuilder;
import com.bi.queryer.ssm.engine.result.ResultDataSetColumn;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;
import com.bi.queryer.ssm.enums.AnalysisTotalType;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
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
public class AnalysisCrossDimensionResultDataSetRowBuilder extends CrossDimensionResultDataSetRowBuilder {
    public AnalysisCrossDimensionResultDataSetRowBuilder(QueryEngine engine) {
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
            String columnCode = column.getCode();
            Object value = rs.getObject(columnCode);
            Object rawValue = value;
            boolean isNullColumn = isNullColumns.get(columnCode) == null ? true : isNullColumns.get(columnCode);
            isNullColumns.put(columnCode, isNullColumn && value == null);

            String columnRawCode = columnCode.split(BIConsts.COLUMN_DIM_FIELD_SUFFIX)[0];
            //兼容行总计
            columnRawCode = columnRawCode.replace("_"+ AnalysisCalcMode.ROW_TOTAL.getCode(),"");
            if(aclCodes.containsKey(columnRawCode)) {
                // 处理小计/总计
                if(value == null && row.get(BIConsts.GROUPING_VALUE) != null && !totalValueConverted){
                    value = AnalysisTotalType.get(row.get(BIConsts.GROUPING_VALUE) + "").getDesc();
                    totalValueConverted = true;
                }else {
                    value = this.engine.formatValue(value, column.getDataType(), this.getColumnFormat(column), columnCode);
                }
            }else {
                value = BIConsts.NO_AUTH_CONTENT;
            }
            row.put(column.getCode(), value);

            if(this.engine.getConfig().getSettings().getQueryRawValue()){
                row.put(column.getCode() + BIConsts.RAW_VALUE_COLUMN_CODE_SUFFIX, rawValue);
            }
        }

        return row;

    }
}
