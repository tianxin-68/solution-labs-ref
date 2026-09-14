package com.bi.queryer.ssm.engine.pivot.sql;

import com.bi.queryer.ssm.engine.*;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.lod.LodQueryEngine;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.ssm.engine.result.ResultDataSet;
import com.bi.queryer.ssm.engine.result.ResultDataSetColumn;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;
import com.bi.queryer.ssm.enums.QueryArea;
import com.bi.queryer.sys.db.DataType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author contributor
 * @Date 13:44 2025/6/24
 * @Description sql转置查询引擎
 **/
public class PivotQueryEngine extends QueryEngine {

    protected QueryEngine subQueryEngine = null;
    protected String subQuerySql = "";
    protected ResultDataSet subQueryDataSet = null;
    protected List<ResultDataSetColumn> leafColumns = new ArrayList<>();
    protected List<ResultDataSetColumn> rootColumns = new ArrayList<>();
    protected List<QueryField> colDimItemFields = new ArrayList<>();

    protected long t1 = 0;

    public PivotQueryEngine(QueryConfigure configure, QueryContext cxt) {
        super(configure, cxt);
        this.leafColumns.clear();
        this.rootColumns.clear();
        this.rowBuilder = new PivotResultDataSetRowBuilder(this);
    }

    /**
     * 重写创建模型：用于获取主+lod的所有模型，便于判断数据源
     * @return
     */
    @Override
    public List<StarModel> createModels() {
        if(this.config.isLodConfig()){
            LodQueryEngine lodQueryEngine = new LodQueryEngine(config,cxt);
            return lodQueryEngine.createModels();
        }else {
            return super.createModels();
        }
    }

    @Override
    public String buildSql() throws BIException {
        t1 = System.currentTimeMillis();

        boolean needSort =  this.config.getSettings().getNeedSort();

        // 子查询去掉排序：避免子查询在构建sql时调用PivotEngine转置，如lod的sql构建
        this.config.getSettings().setNeedSort(false);

        // 创建子查询查询引擎，用户获取子查询sql和空数据集
        this.subQueryEngine = this.createSubQueryEngine(this.config, cxt);
        this.subQuerySql = this.subQueryEngine.buildSql().replace(this.getSqlTips(), "");
        this.subQueryDataSet = this.getSubQueryEmptyDataSet(subQuerySql);

        // 还原是否需排序属性
        this.config.getSettings().setNeedSort(needSort);

        // 生成转置sql
        PivotSqlBuilder sqlBuilder = new PivotSqlBuilder(this);
        String sql = sqlBuilder.build(subQuerySql, subQueryDataSet);

        sql = this.getSqlTips() + " " + sql;

        this.leafColumns = sqlBuilder.getLeafColumns();
        this.rootColumns = sqlBuilder.getRootColumns();
        this.colDimItemFields = sqlBuilder.getColDimItemFields();
        return sql;
    }

    /**
     * 获取子查询空数据集，用于获取表头
     * @param subQuerySql
     * @return
     */
    protected ResultDataSet getSubQueryEmptyDataSet(String subQuerySql){
        ResultDataSet dataSet = new ResultDataSet();
        String noDataSql = subQuerySql.replace(this.getSqlTips(), "");
        // 去掉order by
        if (noDataSql.contains(BIConsts.ORDER_BY)) {
            noDataSql = noDataSql.substring(0, noDataSql.indexOf(BIConsts.ORDER_BY));
        }
        noDataSql = String.format("%s select _pivot_h.* from (%s) _pivot_h where 1=2", this.getSqlTips(), noDataSql);
        String sessionId = cxt.getUser().getName() + "_pivot_header_" + System.currentTimeMillis();

        dataSet = subQueryEngine.execute(noDataSql, new QueryExecuteParameter(-1, sessionId, subQueryEngine.buildColumns()));
        return dataSet;
    }

    @Override
    public ResultDataSet execute(String sql, QueryExecuteParameter executeParameter) {
        // 设置列信息为末级叶子信息，便于后期行数据
        executeParameter.setColumns(this.leafColumns);

        String sessionId = cxt.getUser().getName() + "_pivot_" + System.currentTimeMillis();
        executeParameter.setSessionId(sessionId);
        ResultDataSet dataSet = super.execute(sql, executeParameter);

        // 设置数据集列：此列信息含层级信息
        dataSet.setColumns(this.rootColumns);

        dataSet = this.unpivot(dataSet);

        long t2 = System.currentTimeMillis();
        // 重新记录耗时：表头查询 + 表体查询 + 反转置
        dataSet.getProperties().put("consumeTime", (t2 - t1));
        return dataSet;
    }

    protected QueryEngine createSubQueryEngine(QueryConfigure configure, QueryContext cxt){
        // 先禁用排序来创建子查询engine，最后在还原
        configure.getSettings().getTableStyle().getUpDownSortData().setIsActive(Enabled.NO.getId());
        QueryEngine engine = QueryFactory.createEngine(config, cxt);
        configure.getSettings().getTableStyle().getUpDownSortData().setIsActive(Enabled.YES.getId());
        return engine;
    }

    /**
     * 将列维度通过sql转置的数据集再转到行上，便于兼容后续流程（数据结果 + 元数据兼容）
     * @param dataSet
     * @return
     */
    protected ResultDataSet unpivot(ResultDataSet dataSet){
        List<Map<String, Object>> rows = dataSet.getRows();
        if(BIUtil.isEmpty(rows) || this.subQueryDataSet == null){
            return dataSet;
        }
        ResultDataSet unpivotDataSet = new ResultDataSet();

        List<ResultDataSetColumn> unpivotColumns = this.subQueryDataSet.getColumns();
        List<ResultDataSetColumn> subQueryLeafColumns = this.getSubQueryLeafColumns();
        List<ResultDataSetColumn> rowDimColumns = new ArrayList<>();
        List<ResultDataSetColumn> measureColumns = new ArrayList<>();
        for(ResultDataSetColumn col : subQueryLeafColumns){
            if(QueryArea.get(col.getRawQueryArea()) == QueryArea.RowDimension){
                rowDimColumns.add(col);
            }
            if(QueryArea.get(col.getRawQueryArea()) == QueryArea.Measure){
                measureColumns.add(col);
            }
        }
        for(Map<String, Object> row : rows){
            for(QueryField itemField : this.colDimItemFields) {
                Map<String, Object> newRow = new HashMap<>(32);

                // 指标
                Integer notNullMeasureCount = 0;
                for (String rowFieldCode : row.keySet()) {

                    String dimItemCodeFlag = itemField.getCode();

                    //行总计特殊处理
                    if (dimItemCodeFlag.equalsIgnoreCase(BIConsts.ROW_TOTAL_COLUMN_CODE)) {
                        dimItemCodeFlag = "_" + AnalysisCalcMode.ROW_TOTAL.getCode();
                    }
//                    if (rowFieldCode.endsWith(itemFieldCode)) {
                    if (rowFieldCode.contains(dimItemCodeFlag)) {
                        String measureCode = rowFieldCode.replace(dimItemCodeFlag, "");
                        if (measureCode.endsWith("_")) {
                            measureCode = measureCode.substring(0, measureCode.length() - 1);
                        }

                        Object value = row.get(rowFieldCode);
                        if (value != null) {
                            notNullMeasureCount++;
                        }

                        newRow.put(measureCode, value);
                    }
                }

                //没有指标的行，丢弃掉
                if (newRow.isEmpty() || notNullMeasureCount == 0) {
                    continue;
                }

                //兼容行总计的code
                String itemFieldTitle = itemField.getTitle();
                if(BIConsts.ROW_TOTAL_COLUMN_CODE.equalsIgnoreCase(itemField.getCode())){
                    itemFieldTitle = BIConsts.ROW_TOTAL_COLUMN_CODE;
                }
                if (BIConsts.DATE_CODE.equals(itemField.getRawCode())) {
                    itemFieldTitle = this.formatValue(itemFieldTitle, DataType.String.toString(), null, BIConsts.DATE_CODE) + "";
                }

                newRow.put(itemField.getRawCode(), itemFieldTitle);
                // 行维度
                for (ResultDataSetColumn rowDimColumn : rowDimColumns) {
                    newRow.put(rowDimColumn.getCode(), row.get(rowDimColumn.getCode()));
                }

                //添加_grp_v,_grp_k
                newRow.put(BIConsts.GROUPING_VALUE, row.get(BIConsts.GROUPING_VALUE));
                newRow.put(BIConsts.GROUPING_KEY, row.get(BIConsts.GROUPING_KEY));
                newRow.put(BIConsts.DATE_RAW_KEY, row.get(BIConsts.DATE_RAW_KEY));

                unpivotDataSet.addRow(newRow);
            }
        }

        unpivotDataSet.setProperties(dataSet.getProperties());
        unpivotDataSet.setColumns(unpivotColumns);
        return unpivotDataSet;
    }

    protected List<ResultDataSetColumn> getSubQueryRootColumns(){
        return this.subQueryDataSet.getColumns();
    }

    /**
     * 获取子查询的叶子列
     * @return
     */
    protected List<ResultDataSetColumn> getSubQueryLeafColumns(){
        List<ResultDataSetColumn> subQueryLeafColumns = new ArrayList<>();
        List<ResultDataSetColumn> roots = this.getSubQueryRootColumns();
        this.getSubQueryLeafColumnsCascade(roots, subQueryLeafColumns);
        return subQueryLeafColumns;
    }

    protected void getSubQueryLeafColumnsCascade(List<ResultDataSetColumn> parents, List<ResultDataSetColumn> leafColumns){
        for(ResultDataSetColumn parent : parents){
            List<ResultDataSetColumn> children = parent.getChildren();
            // 无下级，则未末级节点
            if(BIUtil.isEmpty(children)){
                leafColumns.add(parent);
            }else{
                getSubQueryLeafColumnsCascade(children, leafColumns);
            }
        }
    }

    @Override
    public int getLimitRow() {
        return super.getLimitRow();
    }

    @Override
    public QueryEngineType getType() {
        return QueryEngineType.Pivot;
    }
}
