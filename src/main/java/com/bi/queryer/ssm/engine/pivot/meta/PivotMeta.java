package com.bi.queryer.ssm.engine.pivot.meta;

import cn.hutool.core.collection.CollUtil;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.QueryPivotConfig;
import com.bi.queryer.ssm.engine.result.ResultDataSet;
import com.bi.queryer.ssm.engine.result.ResultDataSetColumn;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;
import com.bi.queryer.ssm.enums.AnalysisTotalType;
import com.bi.queryer.ssm.enums.QueryArea;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @Author: contributor
 * @CreateTime: 2024-05-23  10:44
 * @Description: 行列转化，解析表格的元信息
 */
@Data
public class PivotMeta {
    private final static Logger LOG = LoggerFactory.getLogger(PivotMeta.class);
    protected QueryConfigure queryConfig;

    private ResultDataSet resultDataSet;
    private final boolean isQueryPivot;

    //列元信息map， 列header所有节点的信息
    protected Map<String, ResultDataSetColumn> columnFieldMap = new LinkedHashMap<>();

    //列header所有指标叶子节点的信息
    protected Map<String, ResultDataSetColumn> leafColumnFieldMap = new LinkedHashMap<>();

    //列维度值信息
    private Map<String, Set<String>> colDimValues = new HashMap<>();

    private List<String> rowDimCodes = new ArrayList<>(8);

    private List<String> colDimCodes = new ArrayList<>(4);

    //维度编码和其标题的对应关系，便于最后返回结果时，构建rawTitle
    private Map<String, String> dimCodeTitleMap = new HashMap<>(100);

    //指标列信息
    private Map<String, ResultDataSetColumn> measureCols = new LinkedHashMap<>();

    //分析列信息
    private Map<String, ResultDataSetColumn> analysisCols = new LinkedHashMap<>();

    //列总计元信息
    private ColTotalMeta colTotalMeta = new ColTotalMeta();

    //整表总计列信息
    private Map<String, WholeTableTotalMeta> wholeTableTotalCols = new LinkedHashMap<>();

    //行列转换中间状态数据：{ key = 行key, value = { key = 列key, value = cell值}}, 和转置后的结构很像
    private Map<String, Map<String, Object>> cellMap = null;

    private Map<String, RowDimMeta> rowDimRootMap = new HashMap<>(256);

    //行维度树的叶子节点
    private List<RowDimMeta> rowDimLeafNodeList = new ArrayList<>();

    //转置配置
    private QueryPivotConfig pivotConfig;

    public final String COL_CUR_TITLE = "当期值";
    public final String COL_CUR_CODE = "_cur";

    public PivotMeta(QueryConfigure queryConfig, ResultDataSet resultDataSet, QueryPivotConfig pivotConfig) {
        this.queryConfig = queryConfig;
        this.resultDataSet = resultDataSet;
        this.pivotConfig = pivotConfig;
        this.isQueryPivot = SSDUtil.isQueryPivot(queryConfig);
    }

    public void init() {
        //构建原始结果列的元信息，找出叶子节点
        for (ResultDataSetColumn column : this.resultDataSet.getColumns()) {
            buildColumnFieldMap(column, columnFieldMap, leafColumnFieldMap, true);
        }

        //对结果集进行解析，
        //1 获取行维度所有的值
        //2 获取列维度所有的值
        //3 获取所有的指标
        //4 获取所有的分析列
        for (Map.Entry<String, ResultDataSetColumn> entry : columnFieldMap.entrySet()) {
            ResultDataSetColumn field = entry.getValue();
            QueryArea queryArea = QueryArea.get(field.getRawQueryArea());
            switch (queryArea) {
                case RowDimension:
                    buildRowDimension(field);
                    break;
                case ColumnDimension:
                    buildColumnDimension(field);
                    break;
                case Measure:
                    buildMeasure(field);
                    break;
            }
        }

        buildCell();
    }

    //递归构建列的元信息, 找出所有指标叶子节点信息和所有节点信息
    public void buildColumnFieldMap(ResultDataSetColumn column, Map<String, ResultDataSetColumn> columnFieldMap,
                                    Map<String, ResultDataSetColumn> leafColumnFieldMap, boolean excludeColDim) {
        columnFieldMap.put(column.getCode(), column);
        if (CollUtil.isNotEmpty(column.getChildren())) {
            for (ResultDataSetColumn child : column.getChildren()) {
                buildColumnFieldMap(child, columnFieldMap, leafColumnFieldMap, excludeColDim);
            }
        } else {
            QueryArea queryArea = QueryArea.get(column.getRawQueryArea());
            AnalysisCalcMode calcMode = AnalysisCalcMode.get(column.getCalcMode());
            if (QueryArea.RowDimension == queryArea || AnalysisCalcMode.WHOLE_TABLE_TOTAL == calcMode) {
                return;
            } else if (excludeColDim && QueryArea.ColumnDimension == queryArea) {
                return;
            }
            leafColumnFieldMap.put(column.getCode(), column);
        }
    }

    //解析行维度
    public void buildRowDimension(ResultDataSetColumn field) {
        rowDimCodes.add(field.getCode());
        dimCodeTitleMap.put(field.getCode(), field.getTitle());
    }

    //构建行维度树，便于更快的构建结果集的行数据
    public boolean buildRowDimTreeMeta(Map<String, Object> row) {
        String _grp_v = BIUtil.nvl(row.get(BIConsts.GROUPING_VALUE), "");
        AnalysisTotalType totalType = AnalysisTotalType.get(_grp_v);

        //列总计此处不处理
        if (AnalysisTotalType.COL_TOTAL == totalType) {
            return false;
        }

        //没有行维度不处理
        if (CollUtil.isEmpty(rowDimCodes) && CollUtil.isEmpty(colDimValues)) {
            return false;
        }

        int idx = 0;
        for (String code : rowDimCodes) {
            String value = BIUtil.nvl(row.get(code), "");
            if (idx == 0) {
                RowDimMeta rowDimMeta = rowDimRootMap.get(value);
                boolean add = false;
                if (rowDimMeta == null) {
                    rowDimMeta = new RowDimMeta();
                    rowDimMeta.setValue(value);
                    add = true;
                    rowDimRootMap.put(value, rowDimMeta);
                }
                buildRowDimChild(rowDimMeta, row, totalType, 1, add);
            }
            idx++;
        }

        for (Map.Entry<String, Set<String>> entry : colDimValues.entrySet()) {
            //if (AnalysisTotalType.NONE == totalType || AnalysisTotalType.ROW_TOTAL == totalType) {
            String code = entry.getKey();
            Set<String> values = entry.getValue();
            values.add(BIUtil.nvl(row.get(code), ""));
            //}
        }
        return true;
    }

    public void buildRowDimTreeMeta() {
        int idx = 0;
        for (String code : rowDimCodes) {
            if (idx == 0) {
                RowDimMeta rowDimMeta = new RowDimMeta();
                rowDimMeta.setValue("");
                buildRowDimChild(rowDimMeta, Collections.emptyMap(), AnalysisTotalType.UNKNOW, 1, true);
            }
            idx++;
        }
    }

    //构建行维度树的子节点
    public void buildRowDimChild(RowDimMeta rowDimMeta, Map<String, Object> map, AnalysisTotalType totalType, int index, boolean add) {
        if (index == rowDimCodes.size()) {
            if (add) {
                //不重复添加
                rowDimLeafNodeList.add(rowDimMeta);
            }
            return;
        }

        String code = rowDimCodes.get(index);
        String value = nvl(map.get(code), "");
        //列小计处理
        if (AnalysisTotalType.COL_SUBTOTAL == totalType && StringUtils.startsWith(value, AnalysisCalcMode.COL_SUBTOTAL.getDesc())) {
            value = AnalysisTotalType.COL_SUBTOTAL.getCode();
        }

        RowDimMeta childOpt = rowDimMeta.getChildren().get(value);
        RowDimMeta child;
        if (childOpt != null) {
            child = childOpt;
        } else {
            child = new RowDimMeta();
            child.setValue(value);
            child.setParent(rowDimMeta);
            rowDimMeta.addChild(child);
        }

        index++;

        buildRowDimChild(child, map, totalType, index, childOpt == null);
    }

    //解析列维度
    public void buildColumnDimension(ResultDataSetColumn field) {
        Set<String> colValues = colDimValues.computeIfAbsent(field.getRawCode(), k -> new HashSet<>());
        //列维度的值为标题
        if (!colDimCodes.contains(field.getRawCode())) {
            //去重
            colDimCodes.add(field.getRawCode());
        }
//        if (isQueryPivot) {
//            if (BIConsts.ROW_TOTAL_COLUMN_CODE.equals(field.getCode())) {
//                colValues.add(field.getCode());
//            } else {
//                colValues.add(field.getTitle());
//            }
//        }
        dimCodeTitleMap.put(field.getRawCode(), field.getRawTitle());
    }

    //解析指标与同环比
    public void buildMeasure(ResultDataSetColumn field) {
        ResultDataSetColumn parent = columnFieldMap.get(field.getParentCode());
        AnalysisCalcMode calcMode = AnalysisCalcMode.get(field.getCalcMode());

        //父级为指标时，是同环比(排除需要特殊处理的行总计)
        if (parent != null && QueryArea.Measure.toString().equals(parent.getRawQueryArea()) && AnalysisCalcMode.ROW_TOTAL != calcMode) {
            //同环比 的唯一值 = 同环比类型+计算内容
            String thb_key = buildAnalysisKey(field);
            if (!analysisCols.containsKey(thb_key)) {
                analysisCols.put(thb_key, field);
            }
        } else {
            //整表总计
            if (AnalysisCalcMode.WHOLE_TABLE_TOTAL == calcMode) {
                WholeTableTotalMeta wholeTableTotalMeta = new WholeTableTotalMeta();
                wholeTableTotalMeta.setColumn(field);
                String value = "";

                //兼容数据为空的场景
                if(CollUtil.isNotEmpty(this.resultDataSet.getRows())){
                    value = BIUtil.nvl(this.resultDataSet.getRows().get(0).get(field.getCode()), "");
                }
                wholeTableTotalMeta.setValue(value);
                wholeTableTotalCols.put(field.getCode(), wholeTableTotalMeta);
            } else {
                //指标
                if (!measureCols.containsKey(field.getRawCode()) && AnalysisCalcMode.ROW_TOTAL != calcMode) {
                    measureCols.put(field.getRawCode(), field);
                }

            }
        }
    }

    //列总计元数据
    public void buildColTotalMeta() {
        if (Enabled.value(colTotalMeta.getIsActive())) {
            return;
        }

        colTotalMeta.setIsActive(Enabled.YES.getId());
        for (String colDimCode : colDimCodes) {
            colTotalMeta.getColDimCodeList().add(colDimCode);
        }
    }

    private int getColumnDept(ResultDataSetColumn column, int i) {
        if (column == null || CollUtil.isEmpty(column.getChildren())) {
            return i;
        } else {
            int max = 0;
            for (ResultDataSetColumn child : column.getChildren()) {
                max = Math.max(getColumnDept(child, i + 1), max);
            }
            return max;
        }
    }

    private int getColumnMaxDept(List<ResultDataSetColumn> columns) {
        if (BIUtil.isEmpty(columns)) {
            return 0;
        }
        int max = 0;
        for (ResultDataSetColumn column : columns) {
            max = Math.max(getColumnDept(column, 1), max);
        }
        return max;
    }

    //解析单元格，将【值】与 行、列、指标、同环比关联
    public void buildCell() {
        List<Map<String, Object>> rows = this.resultDataSet.getRows();

        long t1 = System.currentTimeMillis();

        //构建行维度树
        if (BIUtil.isEmpty(rows)) {
            buildRowDimTreeMeta();
        } else {
            boolean hasRowData = false;
            for (Map<String, Object> row : rows) {
                hasRowData = buildRowDimTreeMeta(row) || hasRowData;
            }
            if (!hasRowData) {
                buildRowDimTreeMeta();
            }
        }

        long t2 = System.currentTimeMillis();
        LOG.info("[行列转置]构建行维度树耗时：{} ms, sessionId: {}", t2 - t1, this.queryConfig.getSessionId());

        //获取输入数据列的最大深度
        int columnMaxDept = 0;
        if (pivotConfig.isMeasureOnRow()) {
            columnMaxDept = getColumnMaxDept(this.resultDataSet.getColumns());
        }

        //是否展示农历
        boolean isShowLunarDate = this.queryConfig.isShowLunarDate();

        //构建列叶子节点的唯一值
        //leafColumnUniqKeyMap: <key = column code, value = < column dim, column measure>>
        Map<String, Pair<String, String>> leafColumnUniqKeyMap = new LinkedHashMap<>(256);
        boolean isQueryPivot = this.isQueryPivot;
        for (Map.Entry<String, ResultDataSetColumn> e : leafColumnFieldMap.entrySet()) {
            List<String> leafDimColKeys = new ArrayList<>();
            List<String> leafMetricColKeys = new ArrayList<>();
            buildColumnKey(e.getValue(), leafDimColKeys, leafMetricColKeys, columnFieldMap, columnMaxDept, false);
            leafColumnUniqKeyMap.put(e.getKey(),
                    Pair.of(toListKey(leafDimColKeys), leafMetricColKeys.size() > 0 ? leafMetricColKeys.get(0) : ""));
        }

        long t3 = System.currentTimeMillis();
        LOG.info("[行列转置]构建行维度值的树耗时：{} ms, sessionId: {}", t3 - t2, this.queryConfig.getSessionId());

        //构造单元格信息
        initCellMap();
        String colDimCode = colDimCodes.isEmpty() ? null : colDimCodes.get(0);
        final int columnSize = getColumnSize();
        int rowDimSize = rowDimCodes.size();
        boolean isMeasureOnRow = pivotConfig.isMeasureOnRow();
        String cacheRowKey = null;
        Map<String, Object> cacheRowData = null;
        for (Map<String, Object> row : rows) {
            String rowKey = buildRowKey(rowDimSize, row);
            String colDimVal = colDimCode == null ? "" : nvl(row.get(colDimCode), "");
            for (Map.Entry<String, Pair<String, String>> entry : leafColumnUniqKeyMap.entrySet()) {
                Pair<String, String> oriColumnKeys = entry.getValue();
                String pivotColKey = oriColumnKeys.getLeft();
                String measureCode = oriColumnKeys.getRight();
                String pivotRowKey;
                if (isMeasureOnRow) {
                    if (StringUtils.isEmpty(rowKey)) {
                        pivotRowKey = measureCode;
                    } else {
                        pivotRowKey = rowKey + "," + measureCode;
                    }
                } else {
                    pivotRowKey = rowKey;
                    if (StringUtils.isEmpty(pivotColKey)) {
                        pivotColKey = measureCode;
                    } else {
                        pivotColKey = pivotColKey + "," + measureCode;
                    }
                }
                if (colDimCode != null) {
                    if (StringUtils.isEmpty(pivotColKey)) {
                        pivotColKey = colDimVal;
                    } else {
                        //if (isQueryPivot) {
                        //如果是查询转置了, 则不用拼接列维度，因为已经有了
                        //} else {
                        pivotColKey = pivotColKey + "," + colDimVal;
                        //}
                    }
                }

                Map<String, Object> rowData;
                if (Objects.equals(cacheRowKey, pivotRowKey)) {
                    rowData = cacheRowData;
                } else {
                    rowData = cellMap.computeIfAbsent(pivotRowKey, k -> new HashMap<>(columnSize));
                    cacheRowKey = pivotRowKey;
                    cacheRowData = rowData;
                }
                rowData.put(pivotColKey, row.get(entry.getKey()));

                //存 grouping key
                Object groupingKey = row.get(BIConsts.GROUPING_KEY);
                if (BIConsts.ROW_TOTAL_COLUMN_CODE.equals(colDimVal)) {
                    if (groupingKey != null) {
                        if (isQueryPivot) {
                            rowData.put(BIConsts.GROUPING_KEY, groupingKey);
                        } else {
                            rowData.put(BIConsts.GROUPING_KEY, Double.parseDouble(groupingKey.toString()) - 1);
                        }
                    } else {
                        rowData.put(BIConsts.GROUPING_KEY, null);
                    }
                } else {
                    rowData.put(BIConsts.GROUPING_KEY, groupingKey);
                }

                //农历日期原始值处理
                if(isShowLunarDate){
                    rowData.put(BIConsts.DATE_RAW_KEY, row.get(BIConsts.DATE_RAW_KEY));
                }
            }
        }

        long t4 = System.currentTimeMillis();
        LOG.info("[行列转置]构建单元格信息耗时：{} ms, sessionId: {}", t4 - t3, this.queryConfig.getSessionId());
    }

    //构造行维度的唯一值
    public String buildRowKey(int rowDimSize, Map<String, Object> map) {
        String _grp_v = nvl(map.get(BIConsts.GROUPING_VALUE), "");
        AnalysisTotalType totalType = AnalysisTotalType.get(_grp_v);
        List<String> keys = new ArrayList<>(rowDimSize);
        switch (totalType) {
            case NONE:
            case ROW_TOTAL:
                for (String dimCode : rowDimCodes) {
                    keys.add(nvl(map.get(dimCode), ""));
                }
                break;
            case COL_SUBTOTAL:
                for (String dimCode : rowDimCodes) {
                    String value = nvl(map.get(dimCode), "");
                    if (StringUtils.startsWith(value, AnalysisCalcMode.COL_SUBTOTAL.getDesc())) {
                        keys.add(totalType.getCode());
                    } else {
                        keys.add(nvl(map.get(dimCode), ""));
                    }
                }
                break;
            case COL_TOTAL:
                int i = 0;
                for (String dimCode : rowDimCodes) {
                    if (i == 0) {
                        keys.add(totalType.getCode());
                    } else {
                        keys.add(nvl(map.get(dimCode), ""));
                    }
                    i++;
                }
                //列总计元数据
                buildColTotalMeta();
                break;
        }
        return toListKey(keys);
    }

    private String nvl(Object o, String nullValue) {
        if (o == null) {
            return nullValue;
        }

        String res = o.toString();
        if (res.length() == 0) {
            return nullValue;
        }

        return res;
    }

    //向上递归寻找单元格的列的值
    public void buildColumnKey(ResultDataSetColumn column, List<String> keys, Map<String, ResultDataSetColumn> map) {
        buildColumnKey(column, keys, keys, map, 0, true);
    }

    private void buildColumnKey(ResultDataSetColumn column, List<String> dimKeys, List<String> metricKeys,
                                Map<String, ResultDataSetColumn> map, int columnDept, boolean withColDim) {
        QueryArea queryArea = QueryArea.get(column.getRawQueryArea());
        ResultDataSetColumn parent = map.get(column.getParentCode());
        switch (queryArea) {
            case ColumnDimension:
                //列维度的值为标题
                if (withColDim) {
                    dimKeys.add(column.getTitle());
                }
                break;
            case Measure:
                if (parent != null) {
                    AnalysisCalcMode calcMode = AnalysisCalcMode.get(column.getCalcMode());
                    //父级为指标时，是同环比
                    if (QueryArea.Measure.toString().equals(parent.getRawQueryArea())) {
                        //行总计特殊处理
                        if (BIConsts.ROW_TOTAL_COLUMN_CODE.equals(parent.getCode())) {
                            if (AnalysisCalcMode.UNKNOW == calcMode || AnalysisCalcMode.NONE == calcMode || AnalysisCalcMode.ROW_TOTAL == calcMode) {
                                //父级为行总计，本身是原始指标
                                metricKeys.add(column.getRawCode()
                                        .replaceAll("_" + AnalysisCalcMode.ROW_TOTAL.getCode(), "")
                                );
                            } else {
                                //父级为行总计，本身是带分析功能的指标，例如指标转置到行后的同环比等
                                dimKeys.add(buildAnalysisKey(column));
                            }
                        } else {
                            dimKeys.add(buildAnalysisKey(column));
                        }
                    } else if (AnalysisCalcMode.UNKNOW == calcMode || AnalysisCalcMode.NONE == calcMode) {
                        String code = column.getRawCode();
                        if (column.getTargetConfig() != null && column.getTargetConfig().isActive()) {
                            code = column.getTargetConfig().getTargetCode().substring(1);
                        }
                        //父级不是指标，本身是指标
                        metricKeys.add(code);
                    } else {
                        //父级不是指标，本身是带分析功能的指标，例如指标转置到行后的同环比等
                        dimKeys.add(buildAnalysisKey(column));
                    }
                } else {
                    //只有指标，没有同环比，并且指标在行上时，需要自动新增一个结果列。其值不参与单元格唯一值构建
                    if (!COL_CUR_CODE.equalsIgnoreCase(column.getCode())) {
                        if (BIConsts.ROW_TOTAL_COLUMN_CODE.equals(column.getCode())) {
                            dimKeys.add(column.getRawCode());
                        } else {
                            metricKeys.add(column.getRawCode());
                        }
                    }
                }
                break;
        }

        if (parent != null) {
            buildColumnKey(parent, dimKeys, metricKeys, map, columnDept - 1, withColDim);
        } else if (columnDept > 1) {
            //层级缺失，需要补齐层级(本期值), 因为遍历完成后，顺序和树是反的，故需加在第一个
            dimKeys.add(0, "_");
        }
    }

    //后去转置后一行的值
    public Map<String, Object> getRowData(String rowKey) {
        //这里使用remove,及时释放空间，避免内存溢出
        Map<String, Object> rowValues = cellMap.get(rowKey);
        return rowValues == null ? Collections.emptyMap() : rowValues;
    }

    /**
     * 构建集合的唯一值
     */
    public String toListKey(List<String> list) {
        return StringUtils.join(list, ",");
    }

    /**
     * 构建分析字段的唯一值
     */
    public String buildAnalysisKey(ResultDataSetColumn column) {
        String result = "";
        // 添加目标值的code构造逻辑
        if (column.getTargetConfig() != null && column.getTargetConfig().isActive()) {
            result = column.getTargetConfig().getTargetCode().substring(1);
        }
        AnalysisCalcMode analysisCalcMode = AnalysisCalcMode.get(column.getCalcMode());
        //自定义对比需要添加自定义对比的序号，来保证唯一
        if (AnalysisCalcMode.CUSTOM_COMPARE == analysisCalcMode) {
            result = String.format("%s_%s_%s", column.getCalcMode(), column.getCalcType(), column.getCompareIndex());
        } else if (AnalysisCalcMode.ROW_TOTAL == analysisCalcMode) {
            //去掉行总计当期值的calcMode,和其他同环比保持一致，避免后续寻找单元格的值时再进行特殊处理
            result = String.format("%s_%s", "", column.getCalcType());
        } else if (AnalysisCalcMode.CONTRIBUTION_RATE == analysisCalcMode) {
            result = String.format("%s_%s_%s", column.getCtrCalcMode(), column.getCalcMode(), column.getCalcType());
        } else if (AnalysisCalcMode.ZB_THB == analysisCalcMode) {
            result = String.format("%s_%s_%s", column.getZbThbConfig().getZbCalcMode(), column.getZbThbConfig().getThbCalcMode(), column.getCalcType());
        } else {
            if (column.getTargetConfig() != null && column.getTargetConfig().isActive()) {
                if (StringUtils.isNotEmpty(column.getCalcMode())) {
                    result = result + "_" + column.getCalcMode();
                }
                if (StringUtils.isNotEmpty(column.getCalcType())) {
                    result = result + "_" + column.getCalcType();
                }
            } else {
                //同环比的唯一值 = calcMode + calcType  例：环比实际值 hb_r_value
                result = String.format("%s_%s", column.getCalcMode(), column.getCalcType());
            }
        }
        return result;
    }

    public void initCellMap() {
        //初始化cellMap
        this.cellMap = new HashMap<>(getRowSize());
    }

    public int getColumnSize() {
        if (getPivotConfig().isMeasureOnRow()) {
            return new HashSet<>(getPivotConfig().getColDimValues()).size() *
                    getAnalysisCols().size() + getRowDimCodes().size() + 5/*grp_k*/;
        } else {
            return new HashSet<>(getPivotConfig().getColDimValues()).size() * getMeasureCols().size() *
                    getAnalysisCols().size() + getRowDimCodes().size() + 2/*grp_k*/;
        }
    }

    public int getRowSize() {
        if (getPivotConfig().isMeasureOnRow()) {
            return (getRowDimLeafNodeList().size() + 1) * getMeasureCols().size();
        } else {
            return getRowDimLeafNodeList().size() + 1;
        }
    }
}
