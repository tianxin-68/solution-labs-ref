package com.bi.queryer.ssm.engine.result;

import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.promotion.PromotionManager;
import com.bi.queryer.ssm.util.CsvUtil;
import com.bi.queryer.sys.db.DataType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import com.csvreader.CsvWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OlapApiDataSetBuilder extends DefaultDataSetBuilder {
    public OlapApiDataSetBuilder(ResultSet rs, ResultDataSet dataSet, QueryEngine engine) {
        super(rs, dataSet, engine);
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


        // 展平多级表头为一级表头
        List<CsvUtil.FlatColumn> flatColumns = CsvUtil.flattenColumns(columns);
        if (flatColumns.isEmpty()) {
            return;
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CsvWriter csvWriter = null;

        try {

            csvWriter = new CsvWriter(outputStream, ',', StandardCharsets.UTF_8);

            // 写入表头
            String[] headers = new String[flatColumns.size()];
            for (int i = 0; i < flatColumns.size(); i++) {
                headers[i] = flatColumns.get(i).getFlatTitle();
            }

            csvWriter.writeRecord(headers);

            int rowCount = 0;
            while (rs.next()) {
                rowCount++;
                Map row = engine.getRowBuilder().buildRow(rs, columns, metaColMap);
                // 若显示农历，则将原始值返回给前端，用于排序
                if (dateField != null && !isDtPivot) {

                    //业务日历，农历返回原始值处理
                    if (engine.getConfig().getSettings().isBusinessCalendar()) {
                        row.put(
                                BIConsts.DATE_RAW_KEY, PromotionManager.getFormatPromotionName(
                                        BIUtil.nvl(rs.getObject(dateField.getCode()), ""))
                        );
                    } else if (engine.isShowLunarDate()) {
                        row.put(BIConsts.DATE_RAW_KEY, rs.getObject(dateField.getCode()));
                    }

                }

                String[] values = new String[flatColumns.size()];
                for (int i = 0; i < flatColumns.size(); i++) {
                    CsvUtil.FlatColumn flatColumn = flatColumns.get(i);
                    Object value = row.get(flatColumn.getCode());
                    // 去掉数值的千分位
                    if(value != null){
                        if(DataType.getType(flatColumn.getDataType()) != DataType.String) {
                            value = value.toString().replace(",", "");
                        }
                        // 若是日期，则去掉日期的同环比的附加信息
                        // 如：20250301 六 || 年同(-1) 20240301 五  -> 20250301
                        if("dt".equalsIgnoreCase(flatColumn.getCode()) && value.toString().contains("||")  && value.toString().contains(" ")) {
                            value = value.toString().split("\\|\\|")[0];
                            value = value.toString().split(" ")[0];
                        }
                    }
                    values[i] = value == null ? "" : String.valueOf(value);
                }
                csvWriter.writeRecord(values);

            }

            csvWriter.flush();
            outputStream.flush();
            dataSet.setSize(rowCount);
            dataSet.setCsvContent(outputStream.toString(StandardCharsets.UTF_8.name()));
        } catch (Exception e) {
            System.out.println("CSV转换失败" + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (csvWriter != null) {
                    csvWriter.close();
                }
                outputStream.close();
            } catch (IOException e) {
                System.out.println("关闭CSV写入流失败" + e.getMessage());
                e.printStackTrace();
            }
        }
    }

}
