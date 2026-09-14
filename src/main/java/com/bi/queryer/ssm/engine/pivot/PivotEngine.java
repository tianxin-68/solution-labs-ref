package com.bi.queryer.ssm.engine.pivot;

import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.QueryPivotConfig;
import com.bi.queryer.ssm.engine.pivot.meta.ColTotalMeta;
import com.bi.queryer.ssm.engine.pivot.meta.PivotMeta;
import com.bi.queryer.ssm.engine.pivot.meta.RowDimMeta;
import com.bi.queryer.ssm.engine.pivot.meta.WholeTableTotalMeta;
import com.bi.queryer.ssm.engine.result.ResultDataSet;
import com.bi.queryer.ssm.engine.result.ResultDataSetColumn;
import com.bi.queryer.ssm.engine.session.QuerySessionSettingManager;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;
import com.bi.queryer.ssm.enums.AnalysisCalcType;
import com.bi.queryer.ssm.enums.AnalysisTotalType;
import com.bi.queryer.ssm.enums.QueryArea;
import com.bi.queryer.sys.db.DataType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.bi.queryer.util.BIUtil.isEmpty;
import static com.bi.queryer.util.BIUtil.isNotEmpty;

/**
 * @Author: contributor
 * @CreateTime: 2024-05-14  15:08
 * @Description: 行列转换-实现类
 */
public class PivotEngine {
    private final static Logger LOG = LoggerFactory.getLogger(PivotEngine.class);
    //引擎查询的结果集
    private final ResultDataSet resultDataSet;
    private final String sessionId;
    private final QueryConfigure queryConfig;

    private final QueryEngine queryEngine;

    //查询参数，包含查询转置参数
    protected PivotMeta pivotMeta;

    //结果列元信息map
    protected Map<String, ResultDataSetColumn> resultColumnFieldMap = new LinkedHashMap<>();
    protected Map<String, ResultDataSetColumn> resultLeafColumnFieldMap = new HashMap<>(1000);

    public PivotEngine(QueryConfigure queryConfig, ResultDataSet resultDataSet, QueryEngine queryEngine) {
        this.queryConfig = queryConfig;
        this.sessionId = queryConfig.getSessionId();
        this.resultDataSet = resultDataSet;
        this.pivotMeta = new PivotMeta(queryConfig, this.resultDataSet, queryConfig.getResult().getPivotConfig());
        this.queryEngine = queryEngine;
    }

    /**
     * 将行维度、列维度、指标、分析配置、占比汇总的元信息解析存储起来
     */
    public void prepare() {
        pivotMeta.init();
    }

    private boolean needPivot() {
        QueryPivotConfig pivotConfig = pivotMeta.getPivotConfig();
        //只针对配置了"所有指标"在行上的进行转置处理，其他的情况不做处理，直接返回。
        return pivotConfig.needPivot(pivotMeta.getQueryConfig());
    }

    //转换执行
    public ResultDataSet pivot(boolean isExport) {
        if (!needPivot()) {
            return this.resultDataSet;
        }

        LOG.info("[行列转置]开始：sessionId: {}, userName: {}, 转置前size: {} * {}", this.sessionId, UserManager.get().getName(),
                this.resultDataSet.getSize(), this.resultDataSet.getColumnSize());

        long start = System.currentTimeMillis();

        prepare();

        //构造columns
        List<ResultDataSetColumn> columns = buildColumns();

        //构建结果集
        ResultDataSet result = buildResultDataSet(columns);

        //展示列后处理，为前端展示
        postRebuildColumns(columns);

        pivotMeta.setCellMap(null);

        long end = System.currentTimeMillis();
        result.getProperties().put("pivotConsumeTime", (end - start));
        LOG.info("[行列转置]总耗时：{} ms, sessionId: {}, 转置后size: {} * {}", end - start, this.sessionId, result.getSize(), result.getColumnSize());

        if (!isExport) {
            Integer defaultRowLimit = QuerySessionSettingManager.getQueryRowLimit();
            if (result.getSize() > defaultRowLimit) {
                result.setRows(result.getRows().subList(0, defaultRowLimit));
            }
        }

        return result;
    }

    private ResultDataSet buildResultDataSet(List<ResultDataSetColumn> columns) {
        ResultDataSet result = new ResultDataSet();

        //帮助GC清空rows，避免内存不足
        this.resultDataSet.setRows(null);
        //构造rows
        result.setRows(buildRows(columns));
        result.setColumns(columns);

        //封装额外的字段信息
        result.setProperties(this.resultDataSet.getProperties());
        result.setSize(result.getRows().size());
        result.setTotalSize(this.resultDataSet.getTotalSize());
        result.setSessionId(this.resultDataSet.getSessionId());
        return result;
    }

    private void postRebuildColumns(List<ResultDataSetColumn> columns) {
        for (ResultDataSetColumn col : columns) {
            if (BIConsts.ROW_TOTAL_COLUMN_CODE.equals(col.getId())) {
                String aggTypeDesc = queryConfig.getAnalysis().getTotal().getAggConfig().getAggTypeDesc();
                col.setRawCode(BIConsts.ROW_TOTAL_COLUMN_CODE);
                col.setRawQueryArea(QueryArea.Measure.toString());
                col.setCalcMode(AnalysisCalcMode.ROW_TOTAL.getCode());
                col.setTitle(AnalysisCalcMode.ROW_TOTAL.getDesc() + "_" + aggTypeDesc);
                col.setDataType(DataType.Double.toString());
                col.setTotal(true);
                List<ResultDataSetColumn> columnList = col.getChildren();
                if (CollectionUtils.isEmpty(columnList)) {
                    continue;
                }
                if (pivotMeta.getPivotConfig().isMeasureOnRow()) {
                    columnList.get(0).setCalcMode(AnalysisCalcMode.ROW_TOTAL.getCode());
                    //行总计占比的特殊处理
                    columnList.removeIf(v -> AnalysisCalcMode.ZB_ROW_TOTAL.getCode().equals(v.getCalcMode()));

                    //去掉占行总计同环比
                    columnList.removeIf(v -> AnalysisCalcMode.ZB_ROW_TOTAL.getCode().equals(v.getZbThbConfig().getZbCalcMode()));
                } else {
                    for (ResultDataSetColumn column : columnList) {
                        List<ResultDataSetColumn> children = column.getChildren();
                        if (CollectionUtils.isEmpty(children)) {
                            continue;
                        }
                        children.get(0).setCalcMode(AnalysisCalcMode.ROW_TOTAL.getCode());
                        //行总计占比的特殊处理
                        children.removeIf(v -> AnalysisCalcMode.ZB_ROW_TOTAL.getCode().equals(v.getCalcMode()));
                        //去掉占行总计同环比
                        children.removeIf(v -> AnalysisCalcMode.ZB_ROW_TOTAL.getCode().equals(v.getZbThbConfig().getZbCalcMode()));
                    }
                }
                setRowTotalColumn(columnList);
            } else if (QueryArea.ColumnDimension.toString().equals(col.getRawQueryArea())
                    && BIUtil.isEmpty(col.getChildren())) {
                //如果指标转到行上，且没有同环比/汇总/对比查询，就把列维度设置为指标列，给前端展示
                col.setRawQueryArea(QueryArea.Measure.toString());
                col.setDataType(DataType.Double.toString());
            }
        }
    }

    private void setRowTotalColumn(List<ResultDataSetColumn> columns) {
        if (CollectionUtils.isEmpty(columns)) {
            return;
        }
        for (ResultDataSetColumn child : columns) {
            if (StringUtils.isEmpty(child.getCalcType())) {
                child.setCalcType(AnalysisCalcType.REAL_VALUE.getCode());
            }

            if (StringUtils.isEmpty(child.getCalcMode())) {
                child.setCalcMode(AnalysisCalcMode.ROW_TOTAL.getCode());
            }
            child.setTotal(true);

            setRowTotalColumn(child.getChildren());
        }
    }

    /**
     * 构建结果列
     */
    public List<ResultDataSetColumn> buildColumns() {
        List<ResultDataSetColumn> columns = new ArrayList<>();

        //列 = 行 + 列*指标*同环比
        buildRowDimColumns(columns);

        List<String> cols = this.pivotMeta.getColDimCodes();
        if (CollectionUtils.isNotEmpty(cols)) {
            String rootCol = cols.get(0);
            Set<String> rootCols = getColDimValues(rootCol);

            int idx = 0;
            for (String value : rootCols) {
                columns.add(buildColColumns(rootCol, value, idx));
                idx++;
            }
        } else {
            //没有列维度
            buildMeasureColumnsWithoutColDim(columns);
        }

        //整表总计
        for (Map.Entry<String, WholeTableTotalMeta> entry : pivotMeta.getWholeTableTotalCols().entrySet()) {
            columns.add(entry.getValue().getColumn());
        }

        return columns;
    }

    /**
     * 构建行维度列
     */
    public void buildRowDimColumns(List<ResultDataSetColumn> columns) {
        for (String row : this.pivotMeta.getRowDimCodes()) {
            ResultDataSetColumn rowColumn = pivotMeta.getColumnFieldMap().get(row);
            if (rowColumn == null || QueryArea.ColumnDimension.toString().equals(rowColumn.getRawQueryArea())) {
                continue;
            }
            rowColumn.setTitle(rowColumn.getRawTitle());
            rowColumn.setCode(rowColumn.getRawCode());
            rowColumn.setRawQueryArea(QueryArea.RowDimension.toString());
            rowColumn.setChildren(new ArrayList<>());
            columns.add(rowColumn);
        }
    }

    /**
     * 构建指标列(没有列维度的场景)
     */
    public void buildMeasureColumnsWithoutColDim(List<ResultDataSetColumn> columns) {
        //构造指标+同环比
        for (ResultDataSetColumn measure : pivotMeta.getMeasureCols().values()) {
            ResultDataSetColumn col = measure.clone();
            col.setCode(measure.getRawCode());
            col.setParentCode(null);
            col.setChildren(new ArrayList<>());
            col.setId(col.getCode());
            buildAnalysisColumn(measure);
            columns.add(col);
        }
    }

    /**
     * 构建分析同环比字段
     */
    public void buildAnalysisColumn(ResultDataSetColumn col) {
        for (ResultDataSetColumn analysis : col.getChildren()) {
            ResultDataSetColumn analysisCol = analysis.clone();
            analysisCol.setCode(col.getCode() + "_" + pivotMeta.buildAnalysisKey(analysisCol));
            analysisCol.setParentCode(col.getCode());
            col.addChild(analysisCol);
        }
    }

    /**
     * 构建末级节点
     */
    public ResultDataSetColumn buildColColumns(String colDimCode, String colDimValue, int idx) {
        ResultDataSetColumn column = new ResultDataSetColumn();
        if (BIConsts.ROW_TOTAL_COLUMN_CODE.equals(colDimValue)) {
            column.setCode(BIConsts.ROW_TOTAL_COLUMN_CODE);
            column.setId(BIConsts.ROW_TOTAL_COLUMN_CODE);
        } else {
            column.setCode(BIConsts.COLUMN_DIM_FIELD_SUFFIX + "0_" + idx);
            column.setId(colDimValue + BIConsts.SEPARATOR);
        }

        column.setRawQueryArea(QueryArea.ColumnDimension.toString());
        column.setTitle(colDimValue);
        column.setRawCode(colDimCode);
        column.setRawTitle(pivotMeta.getDimCodeTitleMap().get(colDimCode));

        List<ResultDataSetColumn> columnList = new ArrayList<>(pivotMeta.getMeasureCols().values());

        //构造指标+同环比
        for (ResultDataSetColumn measure : columnList) {
            ResultDataSetColumn col = measure.clone();
            if (CollectionUtils.isEmpty(measure.getChildren())) {
                if (BIConsts.ROW_TOTAL_COLUMN_CODE.equals(column.getCode())) {
                    col.setCode(measure.getRawCode() + "_" + AnalysisCalcMode.ROW_TOTAL.getCode());
                } else {
                    col.setCode(measure.getRawCode() + "_" + column.getCode());
                }
                col.setParentCode(column.getCode());
                col.setChildren(new ArrayList<>());
                col.setId(col.getCode());
                column.addChild(col);
            } else {
                col.setCode(column.getCode() + "/" + measure.getRawCode());
                col.setParentCode(column.getCode());
                col.setChildren(new ArrayList<>());
                col.setId(col.getCode());
                column.addChild(col);
                for (ResultDataSetColumn thb : measure.getChildren()) {
                    // 目标值不出行总计
//                    if (BIConsts.ROW_TOTAL_COLUMN_CODE.equals(colDimValue) && thb.getTargetConfig() != null && thb.getTargetConfig().isActive()) {
//                        continue;
//                    }
                    ResultDataSetColumn thbCol = thb.clone();
                    String analysisKey = pivotMeta.buildAnalysisKey(thbCol);
                    if (StringUtils.isEmpty(analysisKey) || "_".equals(analysisKey)) {
                        if (BIConsts.ROW_TOTAL_COLUMN_CODE.equals(column.getCode())) {
                            thbCol.setCode(measure.getRawCode() + "_" + AnalysisCalcMode.ROW_TOTAL.getCode());
                        } else {
                            thbCol.setCode(measure.getRawCode() + "_" + column.getCode());
                        }
                    } else {
                        if (BIConsts.ROW_TOTAL_COLUMN_CODE.equals(column.getCode())) {
                            thbCol.setCode(measure.getRawCode() + "_" + AnalysisCalcMode.ROW_TOTAL.getCode() + "_" + analysisKey);
                        } else {
                            thbCol.setCode(measure.getRawCode() + "_" + analysisKey + "_" + column.getCode());
                        }
                    }
                    thbCol.setParentCode(col.getCode());
                    col.addChild(thbCol);
                }
            }
        }
        return column;
    }

    /**
     * 构建结果集
     */
    public List<Map<String, Object>> buildRows(List<ResultDataSetColumn> columns) {
        int rowSize = pivotMeta.getRowSize();
        List<Map<String, Object>> resultData = new ArrayList<>(rowSize);
        List<String> rowCodes = new ArrayList<>(this.pivotMeta.getRowDimCodes());
        QueryPivotConfig pivotConfig = this.pivotMeta.getPivotConfig();
        if (pivotConfig.isMeasureOnRow()) {
            //所有指标配置在行上
            rowCodes.add(BIConsts.ALL_MEASURE_CODE);
        }
        long t1 = System.currentTimeMillis();
        List<List<String>> result = new ArrayList<>(rowSize);

        //是否展示农历
        boolean isShowLunarDate = this.queryConfig.isShowLunarDate();

        //构建列总计, 放在最前面
        if (checkShowColTotal()) {
            buildColTotalResult(rowCodes, result);
        }

        //一个行维度都没有，只有所有指标的场景
        int rowDimSize = rowCodes.size();
        if (CollectionUtils.isEmpty(pivotMeta.getRowDimLeafNodeList())) {
            List<String> rowDataList = new ArrayList<>(rowDimSize);
            extendRowResultList(result, rowDataList, true);
        } else {
            //构建转置后行维值列表
            for (RowDimMeta rowDimMeta : pivotMeta.getRowDimLeafNodeList()) {
                List<String> rowDataList = new ArrayList<>(rowDimSize);
                buildRowData(rowDimMeta, rowDataList);

                if (CollectionUtils.isEmpty(rowDataList)) {
                    continue;
                }
                //列小计在第一个值，不展示
                if (rowDataList.get(0).equals(AnalysisTotalType.COL_SUBTOTAL.getCode())) {
                    continue;
                }

                extendRowResultList(result, rowDataList, false);
            }
        }

        long t2 = System.currentTimeMillis();
        LOG.info("[行列转置]构建行维度值的列表耗时：{} ms, sessionId: {}", t2 - t1, this.sessionId);

        //构建结果列的元信息
        for (ResultDataSetColumn column : columns) {
            pivotMeta.buildColumnFieldMap(column, resultColumnFieldMap, resultLeafColumnFieldMap, false);
        }

        //构建结果列叶子节点的唯一值
        //结果叶子列的唯一值
        Map<String, String> resultLeafColumnUniqueKeysMap = new HashMap<>(128);
        for (Map.Entry<String, ResultDataSetColumn> e : resultLeafColumnFieldMap.entrySet()) {
            List<String> resultLeafColumnKeys = new ArrayList<>(16);
            pivotMeta.buildColumnKey(e.getValue(), resultLeafColumnKeys, resultColumnFieldMap);
            resultLeafColumnUniqueKeysMap.put(e.getKey(), pivotMeta.toListKey(resultLeafColumnKeys));
        }

        //提前处理整表总计
        Map<String, Object> wholeTableTotalMetaMap = new HashMap<>(pivotMeta.getWholeTableTotalCols().values().size());
        for (WholeTableTotalMeta meta : pivotMeta.getWholeTableTotalCols().values()) {
            wholeTableTotalMetaMap.put(meta.getColumn().getCode(), meta.getValue());
        }

        long t3 = System.currentTimeMillis();
        LOG.info("[行列转置]构建结果列的唯一值结构耗时：{} ms, sessionId: {}", t3 - t2, this.sessionId);

        pivotMeta.setRowDimRootMap(null);
        pivotMeta.setRowDimLeafNodeList(null);
        int columnSize = pivotMeta.getColumnSize();
        String aggTypeDesc = queryConfig.getAnalysis().getTotal().getAggConfig().getAggTypeDesc();
        for (List<String> rowSet : result) {
            List<String> rowKeys = new ArrayList<>(rowDimSize);
            Map<String, Object> rowData = new HashMap<>(columnSize);
            //rowData.put(pivotMeta._GRP_V, 0);

            //将行维度的值添加到单元格的唯一数组中
            int i = 0;
            for (String rowCode : rowCodes) {
                String value = rowSet.get(i++);
                String desc = value;
                //列小计
                if (AnalysisTotalType.COL_SUBTOTAL.getCode().equals(value)) {
                    rowData.put(BIConsts.GROUPING_VALUE, value);
                    desc = AnalysisTotalType.COL_SUBTOTAL.getDesc() + "_" + aggTypeDesc;
                } else if (AnalysisTotalType.COL_TOTAL.getCode().equals(value)) {
                    rowData.put(BIConsts.GROUPING_VALUE, value);
                    desc = AnalysisTotalType.COL_TOTAL.getDesc() + "_" + aggTypeDesc;
                }

                if (BIConsts.ALL_MEASURE_CODE.equals(rowCode)) {
                    ResultDataSetColumn measureCol = pivotMeta.getMeasureCols().get(value);
                    desc = measureCol.getTitle();

                    //添加指标id，便于前台展示指标样式
                    rowData.put("measure_code", measureCol.getRawCode());
                    rowData.put("measure_id", isNotEmpty(measureCol.getChildren()) ? measureCol.getChildren().get(0).getId() : measureCol.getId());
                }

                rowData.put(rowCode, desc);
                rowKeys.add(value);
            }

            //添加每一行中列的数据
            String rowKey = pivotMeta.toListKey(rowKeys);
            Map<String, Object> rowDataMap = pivotMeta.getRowData(rowKey);
            for (String columnKey : resultLeafColumnFieldMap.keySet()) {
                String colKey = resultLeafColumnUniqueKeysMap.get(columnKey);
                rowData.put(columnKey, rowDataMap.get(colKey));
            }

            //整表总计
            rowData.putAll(wholeTableTotalMetaMap);

            //存grouping key
            rowData.put(BIConsts.GROUPING_KEY, rowDataMap.get(BIConsts.GROUPING_KEY));

            //农历日期原始值处理
            if(isShowLunarDate){
                rowData.put(BIConsts.DATE_RAW_KEY, rowDataMap.get(BIConsts.DATE_RAW_KEY));
            }

            //构造列值
            resultData.add(rowData);
        }

        long t4 = System.currentTimeMillis();
        LOG.info("[行列转置]构建结果列的数据耗时：{} ms, sessionId: {}", t4 - t3, this.sessionId);

        return resultData;
    }

    /**
     * 扩充行维度的结果
     */
    public void extendRowResultList(List<List<String>> result, List<String> list, boolean emptyRowDim) {
        if (emptyRowDim) {
            //行维度为空的时候，加一个空行维度值
            list.add("");
        }
        result.add(list);
    }

    /**
     * 构建行数据
     */
    public void buildRowData(RowDimMeta rowDimMeta, List<String> list) {
        RowDimMeta parent = rowDimMeta.getParent();
        if (parent != null) {
            buildRowData(parent, list);
        }
        list.add(rowDimMeta.getValue());
    }

    /**
     * 构建列总计的结果
     */
    public void buildColTotalResult(List<String> rows, List<List<String>> result) {
        List<String> colTotalValueList = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            if (i == 0) {
                colTotalValueList.add(AnalysisTotalType.COL_TOTAL.getCode());
            } else {
                colTotalValueList.add("");
            }
        }
        result.add(colTotalValueList);
    }

    /**
     * 判断是否出列总计
     */
    public boolean checkShowColTotal() {
        ColTotalMeta colTotalMeta = pivotMeta.getColTotalMeta();
        if (!Enabled.value(colTotalMeta.getIsActive())) {
            return false;
        }

        List<String> colList = pivotMeta.getColDimCodes();
        return BIUtil.isListEqual(colTotalMeta.getColDimCodeList(), colList);
    }

    //获取列维度值
    public Set<String> getColDimValues(String key) {
        Set<String> values = pivotMeta.getColDimValues().get(key);
        values = resortColDimValues(values, key);
        //if (values.size() > BIConsts.COLUMN_DIM_ITEM_MAX_COUNT) {
        //    values = values.stream().limit(BIConsts.COLUMN_DIM_ITEM_MAX_COUNT).collect(Collectors.toSet());
        //}

        return values;
    }

    //把列维度中的维度特殊值排到最后去
    private Set<String> resortColDimValues(Set<String> rows, String key) {
        if (isEmpty(rows)) {
            return Collections.emptySet();
        }

        ResultDataSetColumn colDimColumn = this.resultDataSet.getColumns().stream()
                .filter(col -> key.equals(col.getRawCode())).findFirst().orElse(new ResultDataSetColumn());
        List<String> specialDimValueRows = new ArrayList<>(4);
        Map<String, String> dimNumberMap = BIConsts.SPECIAL_DIM_VALUE_NUMBER_MAP;
        Set<String> result = new LinkedHashSet<>(rows.size());
        for (String order : pivotMeta.getPivotConfig().getColDimValues()) {
            order = String.valueOf(queryEngine.formatValue(order, colDimColumn.getDataType(), colDimColumn.getDataFormat(), colDimColumn.getRawCode()));
            if (rows.contains(order)) {
                if (dimNumberMap.containsKey(order)) {
                    specialDimValueRows.add(order);
                } else {
                    result.add(order);
                }
            }
        }
        result.addAll(specialDimValueRows);
        return result;
    }
}
