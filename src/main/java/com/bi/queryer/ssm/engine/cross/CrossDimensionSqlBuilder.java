package com.bi.queryer.ssm.engine.cross;

import cn.hutool.core.collection.CollUtil;
import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.analysis.cross.AnalysisCrossDimensionSqlBuilder;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.function.FunctionManager;
import com.bi.queryer.ssm.engine.function.IFunction;
import com.bi.queryer.ssm.engine.result.ResultDataSetColumn;
import com.bi.queryer.ssm.enums.FieldSortType;
import com.bi.queryer.ssm.util.FieldUtil;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 11:18 2023-08-17
 * @Description 交叉表sql构建器
 **/
public class CrossDimensionSqlBuilder {


    protected List<QueryField> colDimItemFields = new ArrayList<>();

    protected static final String tableAlias = BIConsts.PIVOT_TABLE_ALIAS;

    protected String datasourceSql = "";

    protected List<ResultDataSetColumn> leafColumns = new ArrayList<>();

    /**列维度项*/
    protected CrossDimensionItemBuilder itemBuilder = null;

    protected QueryConfigure config;

    protected QueryContext cxt;

    protected QueryEngine engine;

    protected List<String> selectFragments = null;

    protected List<String> fromFragments = null;

    protected IFunction fx = null;

    public CrossDimensionSqlBuilder(QueryEngine engine) {
        this.engine = engine;
        this.config = engine.getConfig();
        this.cxt = engine.getCxt();
        this.leafColumns.clear();
        this.itemBuilder = CrossDimensionItemBuilderFactory.createItemBuilder(this.config, this.cxt, this.engine); //new CrossDimensionItemBuilder(this.config, this.cxt, engine.getSqlBuilder().getSingleModelSQLBuilder());
    }


    public String build(String datasourceSql) throws BIException {
        this.datasourceSql = datasourceSql;
        this.leafColumns.clear();
        this.fx = FunctionManager.getFunction();

        // 容错处理：无行维度且无指标时，直接返回源sql
        if(BIUtil.isEmpty(this.config.getResult().getRowDimensions()) && BIUtil.isEmpty(this.config.getResult().getMeasures())){
            return datasourceSql;
        }
        // 容错处理：若行维度只有日期且汇总，则不需要转置，直接返回源sql
        QueryField commonDateField = this.config.getResultCommonDateField();
        if(commonDateField != null && commonDateField.isAggQuery() && this.config.getResult().getRowDimensions().size() == 1){
            return datasourceSql;
        }

        /**列维度项*/
        this.colDimItemFields = itemBuilder.build();

        if (BIUtil.isEmpty(colDimItemFields)) {
            return datasourceSql;
        }

        //设置转置配置中的列维度值
        this.buildPivotColDimValues();

        StringBuilder sql = new StringBuilder();

        StringBuilder selectSql = this.buildSelectClause();
        sql.append(selectSql);

        StringBuilder fromSql = this.buildFromClause();
        sql.append(fromSql);

        StringBuilder whereSql = this.buildWhereClause();
        sql.append(whereSql);

        StringBuilder groupBySql = this.buildGroupByClause();
        sql.append(groupBySql);

        StringBuilder orderBySql = this.buildOrderByClause();
        sql.append(orderBySql);

        return sql.toString();
    }

    protected StringBuilder buildSelectClause() {
        StringBuilder sql = new StringBuilder();
        this.selectFragments = this.buildSelectFragments();
        sql.append(" select ");
        sql.append(BIUtil.listToStr(this.selectFragments));
        return sql;
    }

    protected List<String> buildSelectFragments(){
        List<QueryField> dimFields = config.getResult().getRowDimensions();
        //列维度
        List<QueryField> columnFields =  config.getResult().getColDimensions();
        List<QueryField> measureFields = config.getResult().getMeasures();

        // 去掉不显示的指标
        //measureFields = measureFields.stream().filter(f->Enabled.value(f.getIsShow())).collect(Collectors.toList());

        /** 行维度 */
        List<String> selectFragments = new ArrayList<>();

        List<QueryField> allDimFields = new ArrayList<>();
        allDimFields.addAll(dimFields);
        allDimFields.addAll(columnFields);

        for (QueryField selectField : allDimFields) {
            if (selectField.isAppend()) {
                continue;
            }
            String selectFieldName = selectField.getCode();
            StringBuilder selectFragment = new StringBuilder();
            selectFragment.append(selectFieldName).append(" as ").append(selectField.getCode());
            selectFragments.add(selectFragment.toString());
        }

        for (int j = 0; j < measureFields.size(); j++) {
            QueryField measureField = measureFields.get(j);
            if (measureField.isAppend() && !SSDUtil.isAggFilter(measureField) && !measureField.isTargetValue()) {
                continue;
            }

            if (measureField.isAnalysisCalc()) {
                continue;
            }

            String measureExpression = buildSelectMeasureExpression(measureField);
            String fieldAlias = getMeasureAlias(measureField);
            String selectFragment = String.format("%s as %s", measureExpression,  fieldAlias);

            // 避免重复
            if(selectFragments.contains(selectFragment)){
                continue;
            }

            selectFragments.add(selectFragment);
        }

        //去重
        selectFragments = selectFragments.stream().distinct().collect(Collectors.toList());
        return selectFragments;
    }

    /**
     * 构建指标表达式
     * @param measureField
     * @return
     */
    protected String buildSelectMeasureExpression(QueryField measureField){
        String expression = tableAlias + "." + measureField.getCode();
        return expression;
    }

    /**
     * 获取指标别名
     * @param measureField
     * @return
     */
    protected String getMeasureAlias(QueryField measureField){
        String fieldAlias = measureField.getCode();
        return fieldAlias;
    }

    /**
     * 获取列维度项路径：包括自己，及其所有上级维度字段
     *
     * @param itemField
     * @return
     */
    public List<QueryField> getDimItemFieldPath(QueryField itemField) {
        List<QueryField> path = new ArrayList<>();
        path.add(itemField);
        QueryField parent = itemField.getParent();
        while (parent != null) {
            path.add(parent);
            parent = parent.getParent();
        }
        return path;
    }

    protected StringBuilder buildFromClause() {
        StringBuilder sql = new StringBuilder();
        sql.append(" from ");
        sql.append(" (").append(datasourceSql).append(") ").append(tableAlias);
        return sql;
    }

    protected StringBuilder buildWhereClause() {
        StringBuilder sql = new StringBuilder();

        //过滤列维度
        List<String> filterValues = new ArrayList<>();
        for (QueryField qf : this.colDimItemFields) {
            filterValues.add("'"+qf.getValues().get(0).getId()+"'");
        }

        //暂时只支持一个列维度
        List<QueryField> columnFields = config.getResult().getColDimensions().stream().filter(f->!f.isAppend()).collect(Collectors.toList());

        sql.append(" where ");

        sql.append(" ( ");
        sql.append(String.format(" %s.%s in (%s) ", tableAlias, columnFields.get(0).getCode(), BIUtil.listToStr(filterValues, ",")));

        //兼容行总计
        sql.append(String.format(" or %s.%s is null", tableAlias, columnFields.get(0).getCode()));

        sql.append(" ) ");
        return sql;
    }

    protected StringBuilder buildGroupByClause() {
        StringBuilder sql = new StringBuilder();
        return sql;
    }

    protected StringBuilder buildOrderByClause() {
        StringBuilder sql = new StringBuilder();
        if(!config.getSettings().getNeedSort()) {
            return sql;
        }

        //分析的子查询不需要排序
        if(this instanceof AnalysisCrossDimensionSqlBuilder){
            return sql;
        }

        // 获取排序字段
        List<QueryField> rowDimFields = config.getResult().getRowDimensions();
        List<String> orderByFragments = new ArrayList<String>();

        List<String> resultFieldCodes = new ArrayList<>();
        resultFieldCodes.addAll(config.getResult().getFields().stream()
                .filter(f->Enabled.value(f.getIsShow())&&!f.isAppend())
                .map(QueryField::getCode).collect(Collectors.toList()));

        //sql转置，排序去掉列维度
        if(SSDUtil.isQueryPivot(config)) {
            List<QueryField> colDimFields = config.getResult().getColDimensions()
                    .stream().filter(f -> !f.isAppend()).collect(Collectors.toList());

            if (CollUtil.isNotEmpty(colDimFields)) {
                resultFieldCodes.remove(colDimFields.get(0).getCode());
            }

        }

        orderByFragments.addAll(FieldUtil.getOrderByFragments(config,resultFieldCodes));
        List<String> sortFieldCodes = this.config.getSettings().getQuerySortFieldCodes(this.config);

        for (QueryField field : rowDimFields) {
            if (field.getSortType() == FieldSortType.NONE) {
                continue;
            }

            //已排序，不再处理
            if(sortFieldCodes.contains(field.getCode())){
                continue;
            }

            String sortExpression = FieldUtil.getFieldValueSortExpression(field.getCode(), field);
            //orderByFragments.add(sortExpression + " " + field.getSortType().getCode());
            orderByFragments.add(String.format("%s %s nulls last", sortExpression, field.getSortType().getCode()));
        }

        if (orderByFragments.isEmpty()) {
            return sql;
        }

        sql.append(" ").append(BIConsts.ORDER_BY).append(" ");
        sql.append(BIUtil.listToStr(orderByFragments, ","));
        return sql;
    }

    /**
     * 构造转置配置的 列维度值
     */
    public void buildPivotColDimValues() {

        //转置 列维度有值，不再处理
        if(CollUtil.isNotEmpty(this.config.getResult().getPivotConfig().getColDimValues())) {
            return;
        }

        List<String> colDimValues = new ArrayList<>();
        for (QueryField qf : this.colDimItemFields) {
            colDimValues.add(qf.getValues().get(0).getId());
        }

        this.config.getResult().getPivotConfig().setColDimValues(colDimValues);
    }

    public static void main(String[] args) {
        String code = "total_sale" + BIConsts.COLUMN_DIM_FIELD_SUFFIX + "_0_1";
        String[] codes = code.split(BIConsts.COLUMN_DIM_FIELD_SUFFIX);
        for (String c : codes) {
            System.out.println(c);
        }
    }

    public List<ResultDataSetColumn> getLeafColumns() {
        return leafColumns;
    }

    public void setLeafColumns(List<ResultDataSetColumn> leafColumns) {
        this.leafColumns = leafColumns;
    }

    public List<QueryField> getColDimItemFields() {
        return colDimItemFields;
    }

    public void setColDimItemFields(List<QueryField> colDimItemFields) {
        this.colDimItemFields = colDimItemFields;
    }

    public List<String> getSelectFragments() {
        return selectFragments;
    }

    public void setSelectFragments(List<String> selectFragments) {
        this.selectFragments = selectFragments;
    }

    public List<String> getFromFragments() {
        return fromFragments;
    }

    public void setFromFragments(List<String> fromFragments) {
        this.fromFragments = fromFragments;
    }
}
