package com.bi.queryer.ssm.engine.result;

import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.promotion.PromotionManager;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultDataSetBuilder {

    protected ResultSet rs;
    protected ResultDataSet dataSet;
    protected QueryEngine engine;

    public DefaultDataSetBuilder(ResultSet rs, ResultDataSet dataSet, QueryEngine engine) {
        this.rs = rs;
        this.dataSet = dataSet;
        this.engine = engine;
    }

    public void buildDataSet() throws SQLException {
        List<ResultDataSetColumn> columns = dataSet.getColumns();

        // 统一列格式化
        engine.unifyColumnFormat(columns);

        int metaColCount = rs.getMetaData().getColumnCount();
        Set<String> metaColMap = new HashSet<>();
        for (int i = 1; i <= metaColCount; i++) {
            metaColMap.add(rs.getMetaData().getColumnLabel(i));
        }

        QueryField dateField = engine.getConfig().getResultCommonDateField();

        //是否配置按dt转置, dt转置之后，行数据就没有了dt字段，不需要再农历时间原始日期返回了
        boolean isDtPivot = engine.getConfig().getResult().getPivotConfig().getColDimensions().stream()
                .anyMatch(field -> Enabled.YES.getId().equals(field.getIsShow()) && BIConsts.DATE_CODE.equals(field.getCode()));


        while (rs.next()) {
            Map row = engine.getRowBuilder().buildRow(rs, columns, metaColMap);
            // 若显示农历，则将原始值返回给前端，用于排序
            if (dateField != null && !isDtPivot) {

                //业务日历，农历返回原始值处理
                if ( engine.getConfig().getSettings().isBusinessCalendar()) {
                    row.put(
                            BIConsts.DATE_RAW_KEY, PromotionManager.getFormatPromotionName(
                                    BIUtil.nvl(rs.getObject(dateField.getCode()), ""))
                    );
                } else if (engine.isShowLunarDate()) {
                    row.put(BIConsts.DATE_RAW_KEY, rs.getObject(dateField.getCode()));
                }

            }

            dataSet.addRow(row);
        }
    }

}
