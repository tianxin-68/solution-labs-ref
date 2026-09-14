package com.bi.queryer.ssm.engine.cross;

import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.result.ResultDataSetColumn;
import com.bi.queryer.ssm.engine.result.ResultDataSetRowBuilder;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @Author contributor
 * @Date 21:54 2024-07-24
 * @Description 交叉表数据集行构建器
 **/
public class CrossDimensionResultDataSetRowBuilder extends ResultDataSetRowBuilder {
    protected Map<String, String> aclCodes = null;
    protected Map<String, Boolean> isNullColumns = null; // 是否是空列

    public CrossDimensionResultDataSetRowBuilder(QueryEngine engine) {
        super(engine);
        this.aclCodes = engine.getCxt().getAclFields();
        this.isNullColumns = new HashMap<>();
    }

    public Map buildRow(ResultSet rs, List<ResultDataSetColumn> columns, Set<String> columnMetaCodes) throws SQLException {
        Map row = new HashMap(32);
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
                value = this.engine.formatValue(value, column.getDataType(), getColumnFormat(column), columnRawCode);
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

    protected String getColumnFormat(ResultDataSetColumn column) {
      return column.getDataFormat();
    }

    public Map<String, Boolean> getIsNullColumns() {
        return isNullColumns;
    }

    public void setIsNullColumns(Map<String, Boolean> isNullColumns) {
        this.isNullColumns = isNullColumns;
    }
}
