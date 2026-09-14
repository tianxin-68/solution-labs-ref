package com.bi.queryer.ssm.engine.export;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.QueryExecuteParameter;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.function.IFunction;
import com.bi.queryer.ssm.engine.pivot.PivotFactory;
import com.bi.queryer.ssm.engine.result.ResultDataSet;
import com.bi.queryer.ssm.engine.result.ResultDataSetColumn;
import com.bi.queryer.ssm.enums.AnalysisTotalType;
import com.bi.queryer.ssm.enums.QueryArea;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.period.DateUtil;
import com.csvreader.CsvWriter;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Auther: contributor
 * @Date: 2025/2/24 10:21
 * @Description:
 */
public class CrossQueryExportEngine extends ExportEngine {
    protected String querySql = "";

    protected CsvWriter csvWriter = null;

    protected String fileName = "";

    public CrossQueryExportEngine(QueryEngine queryEngine) {
        super(queryEngine);
    }

    @Override
    public void export(String fileRealName, String querySql) {
        OutputStream fileOut = null;
        try {
            this.fileName = fileRealName;
            this.querySql = querySql;
            if (BIUtil.isEmpty(this.querySql)) {
                this.querySql = queryEngine.buildSql();
            }

            fileOut = Files.newOutputStream(new File(fileRealName).toPath());

            // 根据系统编码添加bom文件头，处理csv乱码问题
            byte[] uft8bom = {(byte) 0xef, (byte) 0xbb, (byte) 0xbf};
            fileOut.write(uft8bom);
            fileOut.flush();

            csvWriter = new CsvWriter(fileOut, ',', StandardCharsets.UTF_8);

            writeCsv();

            csvWriter.flush();
            fileOut.flush();

            //上报风控
            submitRisk();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (csvWriter != null) {
                    csvWriter.close();
                }
                if (fileOut != null) {
                    fileOut.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void writeCsv() throws Exception {
        String sessionId = cxt.getUser().getName() + "_cross_export_" + System.currentTimeMillis();

        // 不隐藏空列
        config.getSettings().setIsHideNullColumn(Enabled.NO.getId());
        ResultDataSet dataSet = queryEngine.execute(this.querySql, new QueryExecuteParameter(-1, sessionId, this.queryEngine.buildColumns()));
        dataSet = PivotFactory.createPivotEngine(this.getConfig(), dataSet, queryEngine).pivot(true);

        writeHeader(dataSet);

        writeData(dataSet);
    }

    private void writeData(ResultDataSet dataSet) throws Exception {
        List<ResultDataSetColumn> headerColumns = dataSet.getColumns();
        List<ResultDataSetColumn> leafColumns = new ArrayList<>();
        headerColumns.forEach(c -> leafColumns.addAll(c.getLeafChildren()));

        // 统一列格式化
        //this.unifyColumnFormat(leafColumns);

        if (Enabled.value(config.getSettings().getShowDateRemark())) {
            this.compareDateMappings = DateUtil.buildCompareDateMapping(config, compareTitleMapping);
        }

        Map<String, String> exportAppendStringMap = config.getResult().getFields().stream().filter(v -> Enabled.isFalse(v.getIsAnalysis()))
                .filter(v -> v.getMeta() != null && StrUtil.isNotEmpty(v.getMeta().getExportAppendString()))
                .collect(Collectors.toMap(QueryField::getCode, v -> v.getMeta().getExportAppendString(), (f1, f2) -> f1));

        for (ResultDataSetColumn dataSetColumn : leafColumns) {
            for (Map.Entry<String, String> entry : exportAppendStringMap.entrySet()) {
                if (dataSetColumn.getRawCode().contains(entry.getKey())) {
                    dataSetColumn.setExportAppendString(entry.getValue());
                    break;
                }
            }
        }

        boolean isMeasureOnRow = config.getResult().getPivotConfig().isMeasureOnRow();
        for (Map<String, Object> rs : dataSet.getRows()) {
            String[] row = new String[leafColumns.size()];
            //boolean totalValueConverted = false;
            int i = 0;
            String exportAppendString = isMeasureOnRow ? exportAppendStringMap.get(rs.get("measure_code") + "") : null;
            for (ResultDataSetColumn dataSetColumn : leafColumns) {
                String columnName = dataSetColumn.getCode();
                Object value = rs.get(columnName);
                //Object groupingValue = rs.getOrDefault(BIConsts.GROUPING_VALUE, null);
                // 处理总计/小计标题
                //if (groupingValue != null && !totalValueConverted && !QueryArea.Measure.toString().equalsIgnoreCase(dataSetColumn.getRawQueryArea())) {
                //    value = AnalysisTotalType.get(groupingValue + "").getDesc();
                //    totalValueConverted = true;
                //}

                //月份导出，如果没有配置导出符，添加默认导出符，避免文件显示日期
                if (IFunction.Format_Month.equalsIgnoreCase(dataSetColumn.getDataFormat())) {
                    if (StrUtil.isEmpty(dataSetColumn.getExportAppendString())) {
                        value += "\t";
                    }
                }
                if (value != null) {
                    String appendString;
                    if (isMeasureOnRow && QueryArea.Measure.toString().equals(dataSetColumn.getRawQueryArea())) {
                        appendString = BIUtil.isEmpty(exportAppendString) ? "" : "\t";
                    } else {
                        appendString = BIUtil.isEmpty(dataSetColumn.getExportAppendString()) ? "" : "\t";
                    }
                    value = appendString + value;
                }
                if (value == null) {
                    value = "";
                }
                if (!dataSetColumn.isHasAuth()) {
                    value = BIConsts.NO_AUTH_CONTENT;
                }
                row[i] = String.valueOf(value);
                i++;
            }
            csvWriter.writeRecord(row, true);
        }
        this.totalSize = dataSet.getRows().size();
    }

    /**
     * 写表头
     */
    protected void writeHeader(ResultDataSet dataSet) throws IOException {
        // 去掉整表总计列 和 去掉隐藏列
        List<ResultDataSetColumn> columns = dataSet.getColumns();
        columns = columns.stream().filter(c -> !c.isWholeTableTotal())
                .filter(c -> !BIConsts.GROUPING_KEY.equals(c.getCode()))
                .filter(c -> !BIConsts.GROUPING_VALUE.equals(c.getCode())).collect(Collectors.toList());

        dataSet.setColumns(columns);

        List<List<String>> headerTitles = this.buildHeader(dataSet.getColumns());
        for (List<String> headerTitle : headerTitles) {
            String[] headers = new String[headerTitle.size()];
            headerTitle.toArray(headers);
            csvWriter.writeRecord(headers);
        }
    }

    /**
     * 构建表头
     */
    private List<List<String>> buildHeader(List<ResultDataSetColumn> columns) {
        List<List<String>> headerTitles = ListUtil.list(false);
        int maxLevel = 0;
        for (ResultDataSetColumn c : columns) {
            List<ResultDataSetColumn> leafChildren = c.getLeafChildren();
            int childLevel = leafChildren.stream().max(Comparator.comparingInt(ResultDataSetColumn::getLevel)).get().getLevel();
            if (childLevel > maxLevel) {
                maxLevel = childLevel;
            }
        }
        for (int i = 0; i < (maxLevel + 1); i++) {
            List<String> titleArr = ListUtil.list(false);
            headerTitles.add(titleArr);
        }
        buildCascadeHeader(columns, headerTitles, 0);
        return headerTitles;
    }

    private void buildCascadeHeader(List<ResultDataSetColumn> columns, List<List<String>> headerTitles, int level) {
        for (ResultDataSetColumn column : columns) {
            headerTitles.get(level).add(column.getTitle());
            if (CollectionUtil.isEmpty(column.getChildren())) {
                for (int i = level + 1; i < headerTitles.size(); i++) {
                    headerTitles.get(i).add("");
                }
            } else {
                int childNum = column.getLeafChildren().size();
                for (int i = 1; i < childNum; i++) {
                    headerTitles.get(level).add("");
                }
                buildCascadeHeader(column.getChildren(), headerTitles, level + 1);
            }
        }
    }
}
