package com.bi.queryer.ssm.engine.export;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.QueryExecuteParameter;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.function.IFunction;
import com.bi.queryer.ssm.engine.result.ResultDataSet;
import com.bi.queryer.ssm.engine.result.ResultDataSetColumn;
import com.bi.queryer.ssm.enums.AnalysisTotalType;
import com.bi.queryer.ssm.enums.ExportAppendStringType;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.period.DateUtil;
import com.csvreader.CsvWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 17:04 2023-08-14
 * @Description 查询导出器
 **/
public class QueryExportEngine extends ExportEngine {
    protected String querySql = "";

    protected CsvWriter csvWriter = null;

    protected String fileName = "";

    protected ResultDataSet headerDataSet = null;

    public QueryExportEngine(QueryEngine queryEngine){
        super(queryEngine);
    }

    @Override
    public void export(String fileRealName, String querySql){
        OutputStream fileOut = null;
        try {
            this.fileName = fileRealName;
            //this.querySql = queryEngine.buildSql();
            this.querySql = querySql;
            if(BIUtil.isEmpty(this.querySql)){
                this.querySql = queryEngine.buildSql();
            }

            fileOut = new FileOutputStream(new File(fileRealName));

            // 根据系统编码添加bom文件头，处理csv乱码问题
            byte[] uft8bom = {(byte) 0xef, (byte) 0xbb, (byte) 0xbf};
            fileOut.write(uft8bom);
            fileOut.flush();

            csvWriter = new CsvWriter(fileOut, ',', Charset.forName("UTF-8"));

            // 写表头
            this.headerDataSet = this.writeHeader();

            // 写内容
            this.writeBody();

            csvWriter.flush();
            fileOut.flush();

            //上报风控
            submitRisk();

        } catch (IOException e) {
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
    /**
    @Override
    public ResultDataSet execute(String sql, Integer maxRowCount, String queryId, List<ResultDataSetColumn> columns) {
        return super.execute(sql, maxRowCount, queryId, columns);
    }
    */

    @Override
    public ResultDataSet execute(String sql, QueryExecuteParameter executeParameter) {
        return super.execute(sql, executeParameter);
    }

    /**
     * 写表头
     */
    protected ResultDataSet writeHeader() throws IOException {
        if(BIUtil.isEmpty(querySql)) {
            return new ResultDataSet();
        }

        querySql = SSDUtil.rectifyOrderBySql(querySql,queryEngine.getSqlTips());
        String noDataSql = querySql.replace(queryEngine.getSqlTips(), "");

        noDataSql = String.format("%s select _exp_h.* from (%s) _exp_h where 1=2", queryEngine.getSqlTips(), noDataSql);

        String sessionId = cxt.getUser().getName() +  "_export_header_" + System.currentTimeMillis();

        // 不隐藏空列
        config.getSettings().setIsHideNullColumn(Enabled.NO.getId());
        ResultDataSet dataSet = this.queryEngine.execute(noDataSql, new QueryExecuteParameter(-1, sessionId, this.queryEngine.buildColumns())); //this.queryEngine.execute(noDataSql, -1, sessionId);

        // 去掉整表总计列
        List<ResultDataSetColumn> columns = dataSet.getColumns();
        columns = columns.stream().filter(c->!c.isWholeTableTotal()).collect(Collectors.toList());

        // 去掉隐藏列
        columns = columns.stream().filter(c-> !BIConsts.GROUPING_KEY.equals(c.getCode())).filter(c-> !BIConsts.GROUPING_VALUE.equals(c.getCode())).collect(Collectors.toList());

        dataSet.setColumns(columns);


        List<List<String>> headerTitles = this.buildHeader(dataSet.getColumns());
        for (List<String> headerTitle : headerTitles) {
            String[] headers = new String[headerTitle.size()];
            headerTitle.toArray(headers);
            csvWriter.writeRecord(headers);
        }

        return dataSet;
    }

    /**
     * 写表体：此处不写具体内容，具体内容在buildDataSet中流式处理，因为覆写了父类的buildDataSet
     */
    protected void writeBody(){
        String sessionId = cxt.getUser().getName() +  "_export_body_" + System.currentTimeMillis();
        //this.execute(this.querySql, -1, queryId, headerDataSet.getColumns());
        this.execute(this.querySql, new QueryExecuteParameter(-1, sessionId, headerDataSet.getColumns()));
    }

    @Override
    public void buildDataSet(ResultSet rs, ResultDataSet dataSet) throws SQLException {

        this.totalSize = 0;
        List<ResultDataSetColumn> headerColumns = headerDataSet.getColumns();
        List<ResultDataSetColumn> leafColumns = new ArrayList<>();
        headerColumns.forEach(c -> leafColumns.addAll(c.getLeafChildren()));

        // 统一列格式化
        this.unifyColumnFormat(leafColumns);

        ResultSetMetaData metadata = rs.getMetaData();
        int metaColCount = metadata.getColumnCount();
        Set<String> metaColMap = new HashSet<>();
        Map<String, ResultDataSetColumn> exportResultColumns = new LinkedHashMap<>();
        Map<String, QueryField> queryFields = config.getResult().getFields().stream().collect(Collectors.toMap(QueryField::getCode, QueryField -> QueryField, (f1, f2) -> f1));

        // 添加日期对比映射
        if (Enabled.value(config.getSettings().getShowDateRemark())) {
            this.compareDateMappings = DateUtil.buildCompareDateMapping(config,compareTitleMapping);
        }

        for (int i = 1; i <= metaColCount; i++) {
            String columnName = metadata.getColumnLabel(i);
            metaColMap.add(columnName);

            ResultDataSetColumn dataSetColumn = this.getColumn(columnName, leafColumns);
            if (dataSetColumn == null || !dataSetColumn.isExportable()) {
                continue;
            }

            String queryFieldName = columnName.contains(BIConsts.COLUMN_DIM_FIELD_SUFFIX) ? columnName.split(BIConsts.COLUMN_DIM_FIELD_SUFFIX)[0] : columnName;
            QueryField queryField = queryFields.get(queryFieldName);
            if (queryField != null && queryField.getMeta() != null) {

                String appendString = "";
                ExportAppendStringType exportAppendStringType = ExportAppendStringType.get(queryField.getMeta().getExportAppendString());
                if (ExportAppendStringType.TAB.getId().equalsIgnoreCase(exportAppendStringType.getId())) {
                    appendString = "\t";
                }
                dataSetColumn.setExportAppendString(appendString);

                //Columns 没有设置格式化，再从元数据获取
                if (StrUtil.isEmpty(dataSetColumn.getDataFormat())) {
                    dataSetColumn.setDataFormat(queryField.getMeta().getShowFormatExpression());
                }
            }

            exportResultColumns.put(columnName, dataSetColumn);
        }

        // 导出顺序和表头（叶子列）保持一致
        Map<String, ResultDataSetColumn> sortedExportResultColumn = new LinkedHashMap<>();
        for(ResultDataSetColumn column : leafColumns){
            if(exportResultColumns.containsKey(column.getCode())){
                sortedExportResultColumn.put(column.getCode(), exportResultColumns.get(column.getCode()));
            }
        }

        String aggTypeDesc = config.getAnalysis().getTotal().getAggConfig().getAggTypeDesc();
        try {
            while (rs.next()) {
                this.totalSize++;
                String[] row = new String[sortedExportResultColumn.size()];
                boolean totalValueConverted = false;
                int i = 0;
                for (String columnName : sortedExportResultColumn.keySet()) {
                    Object value = rs.getObject(columnName);
                    ResultDataSetColumn dataSetColumn = sortedExportResultColumn.get(columnName);
                    if (dataSetColumn != null) {
                        Object groupingValue = metaColMap.contains(BIConsts.GROUPING_VALUE) ? rs.getObject(BIConsts.GROUPING_VALUE) : null;
                        // 处理总计/小计标题
                        if(value == null && groupingValue != null && !totalValueConverted){
                            value = AnalysisTotalType.get(groupingValue + "").getDesc() + "_" + aggTypeDesc ;
                            totalValueConverted = true;
                        }else {
                            value = this.formatValue(value, dataSetColumn.getDataType(), dataSetColumn.getDataFormat(), dataSetColumn.getRawCode());
                        }

                        //月份导出，如果没有配置导出符，添加默认导出符，避免文件显示日期
                        if(IFunction.Format_Month.equalsIgnoreCase(dataSetColumn.getDataFormat())){
                            if(StrUtil.isEmpty(dataSetColumn.getExportAppendString())){
                                value += "\t";
                            }
                        }
                    }
                    if (value != null && dataSetColumn != null) {
                        String appendString = BIUtil.isEmpty(dataSetColumn.getExportAppendString()) ? "" : dataSetColumn.getExportAppendString();
                        value = appendString + value;
                    }
                    if (value == null) {
                        value = "";
                    }
                    if(!dataSetColumn.isHasAuth()) {
                        value = BIConsts.NO_AUTH_CONTENT;
                    }
                    row[i] = String.valueOf(value);
                    i++;
                }
                csvWriter.writeRecord(row, true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 通过列名获取列对象
     * @param columnName
     * @return
     */
    protected ResultDataSetColumn getColumn(String columnName, List<ResultDataSetColumn> columns) {
        ResultDataSetColumn result = null;
        if(BIUtil.isEmpty(columns)) {
            return result;
        }

        for(ResultDataSetColumn col : columns){
            if(col.getCode().equalsIgnoreCase(columnName)) {
                col.setExportable(true);
                return col;
            }
        }
        return result;
    }

    /**
     * 构建表头
     * @param columns
     * @return
     */
    private List<List<String>> buildHeader(List<ResultDataSetColumn> columns) {
        List<List<String>> headerTitles = ListUtil.list(false);
        Integer maxLevel = 0;
        for(ResultDataSetColumn c : columns){
            List<ResultDataSetColumn> leafChildren = c.getLeafChildren();
            int childLevel = leafChildren.stream().max((v1, v2) -> Integer.compare(v1.getLevel(), v2.getLevel())).get().getLevel();
            if(childLevel > maxLevel){
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
                Integer childNum = column.getLeafChildren().size();
                for (int i = 1; i < childNum; i++) {
                    headerTitles.get(level).add("");
                }
                buildCascadeHeader(column.getChildren(), headerTitles, level + 1);
            }
        }
    }
}
