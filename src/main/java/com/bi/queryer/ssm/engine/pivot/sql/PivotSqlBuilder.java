package com.bi.queryer.ssm.engine.pivot.sql;

import cn.hutool.core.util.NumberUtil;
import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.analysis.AnalysisUtil;
import com.bi.queryer.ssm.engine.analysis.operator.OperatorFactory;
import com.bi.queryer.ssm.engine.analysis.operator.impl.TotalOperator;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.FieldValue;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.function.FunctionManager;
import com.bi.queryer.ssm.engine.function.IFunction;
import com.bi.queryer.ssm.engine.result.ResultDataSet;
import com.bi.queryer.ssm.engine.result.ResultDataSetColumn;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;
import com.bi.queryer.ssm.enums.AnalysisTotalType;
import com.bi.queryer.ssm.enums.FieldSortType;
import com.bi.queryer.ssm.enums.QueryArea;
import com.bi.queryer.ssm.util.FieldUtil;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.bi.queryer.util.BIUtil.isEmpty;

/**
 * @Author contributor
 * @Date 11:18 2023-08-17
 * @Description 交叉表sql构建器
 **/
public class PivotSqlBuilder{
    protected static final String tableAlias = BIConsts.PIVOT_TABLE_SHORT_ALIAS;

    protected String subQuerySql = "";

    protected List<ResultDataSetColumn> leafColumns = new ArrayList<>();

    protected List<ResultDataSetColumn> rootColumns = new ArrayList<>();

    protected QueryConfigure config;

    protected QueryContext cxt;

    protected QueryEngine engine;

    protected List<String> selectFragments = null;

    protected List<String> fromFragments = null;

    protected IFunction fx = null;

    protected ResultDataSet subQueryDataSet = null;

    protected List<QueryField> colDimItemFields = new ArrayList<>();

    protected Map<String, ResultDataSetColumn> subQueryAllColumns = new HashMap<>();

    public PivotSqlBuilder(QueryEngine engine) {
        this.engine = engine;
        this.config = engine.getConfig();
        this.cxt = engine.getCxt();
        this.leafColumns.clear();
    }


    public String build(String subQuerySql, ResultDataSet subQueryDataSet) throws BIException {
        this.subQuerySql = subQuerySql;
        this.subQueryDataSet = subQueryDataSet;
        this.leafColumns.clear();
        this.fx = FunctionManager.getFunction();

        this.colDimItemFields = this.createColumnDimensionItemFields();

        // 容错处理：无行维度且无指标时，直接返回源sql
        if(BIUtil.isEmpty(this.config.getResult().getRowDimensions()) && BIUtil.isEmpty(this.config.getResult().getMeasures())){
            return subQuerySql;
        }
        // 容错处理：若行维度只有日期且汇总，则不需要转置，直接返回源sql
        QueryField commonDateField = this.config.getResultCommonDateField();
        if(commonDateField != null && commonDateField.isAggQuery() && this.config.getResult().getRowDimensions().size() == 1){
            return subQuerySql;
        }
        /**列维度项*/
        if (BIUtil.isEmpty(colDimItemFields)) {
            return subQuerySql;
        }

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

    /**
     * 通过子查询的列构建select子句 + 最终数据集列
     * @return
     */
    protected List<String> buildSelectFragments(){
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
        /** 行维度 */
        List<String> selectFragments = new ArrayList<>();
        for (ResultDataSetColumn column : rowDimColumns) {
            String selectFieldName = column.getCode();
            StringBuilder selectFragment = new StringBuilder();
            selectFragment.append(selectFieldName);
            selectFragments.add(selectFragment.toString());
            this.leafColumns.add(column);
            this.rootColumns.add(column);
        }

        /** 列维度项 + 指标 */
        for (int i = 0; i < colDimItemFields.size(); i++) {
            QueryField colItemField = colDimItemFields.get(i);

            ResultDataSetColumn itemRootColumn = this.engine.createDataSetColumn(colItemField);
            itemRootColumn.setRawQueryArea(colItemField.getQueryArea().toString());
            itemRootColumn.setRawTitle(colItemField.isCommonDate() ? "日期" : colItemField.getSourceField().getTitle());
            rootColumns.add(itemRootColumn);

            for (int j = 0; j < measureColumns.size(); j++) {
                ResultDataSetColumn measureColumn = measureColumns.get(j);

                String measureExpression = this.buildSelectMeasureExpression(measureColumn.getCode(), colItemField);
                String fieldAlias = this.getMeasureAlias(measureColumn, colItemField); // measureField.getCode() + colItemField.getCode();

                String selectFragment = String.format("%s as %s", measureExpression,  fieldAlias);

                // 避免重复
                if(selectFragments.contains(selectFragment)){
                    continue;
                }
                selectFragments.add(selectFragment);

                // 重新调整子查询列的上下级关系：暂时只支持子查询的2层结构
                ResultDataSetColumn newMeasureColumn = measureColumn.clone();
                newMeasureColumn.setCode(fieldAlias);
                ResultDataSetColumn measureParentColumn = subQueryAllColumns.get(newMeasureColumn.getParentCode());
                if(measureParentColumn != null) {
                    // 当前指标列的上级
                    String parentCode = measureParentColumn.getCode();
                    if(parentCode.contains("/")){
                        // 避免重复添加"/"
                        parentCode = itemRootColumn.getCode() + parentCode;
                    }else{
                        parentCode = itemRootColumn.getCode() + "/" + parentCode;
                    }

                    // 优先从root中获取当前指标的上级，避免重复创建，否则对原始父列clone后再修改
                    ResultDataSetColumn newParentColumn = itemRootColumn.getChild(parentCode);
                    if(newParentColumn == null) {
                        newParentColumn = measureParentColumn.clone();
                        newParentColumn.getChildren().clear();

                        newParentColumn.setId(parentCode);
                        newParentColumn.setCode(parentCode);
                        newParentColumn.setParentCode(itemRootColumn.getCode());
                    }

                    // 当前指标列：修改其上级列code
                    newMeasureColumn.setParentCode(String.format("%s/%s", itemRootColumn.getCode(), parentCode));

                    // 父列添加子列
                    newParentColumn.addChild(newMeasureColumn);

                    // 添加root列
                    itemRootColumn.addChild(newParentColumn);
                }else{
                    newMeasureColumn.setParentCode(itemRootColumn.getCode());
                    itemRootColumn.addChild(newMeasureColumn);
                }
                leafColumns.add(newMeasureColumn);
            }
        }

        if(config.hasAnalysis()) {
            // 【废弃】注意：此处需要使用min，不要使用max：解决列总计、列小计、行总计、整表总计同时存在时grouping值丢失grp_v=0
            // 不能取min，在使用行总计的场景，如果维度没有值，grp_k取min不对，会多1，需要重新计算

            selectFragments.add(String.format("min(%s) as %s", BIConsts.GROUPING_VALUE, BIConsts.GROUPING_VALUE));

            List<String> rowDimCodeList = getRowDimCodeList();
            String groupingExpression = SSDUtil.calcPivotGroupingKey(rowDimCodeList);
            selectFragments.add(String.format("%s as %s", groupingExpression, BIConsts.GROUPING_KEY));

        }


        //去重
        selectFragments = selectFragments.stream().distinct().collect(Collectors.toList());
        return selectFragments;
    }

    /**
     * 获取行维度的编码集合
     * @return
     */
    public List<String> getRowDimCodeList() {
        List<String> rowDimCodeList = new ArrayList<>();
        List<QueryField> dimFields = config.getResult().getRowDimensions();
        for (QueryField selectField : dimFields) {
            if (selectField.isAppend()) {
                continue;
            }
            String selectFieldName = selectField.getCode();
            StringBuilder selectFragment = new StringBuilder();
            selectFragment.append(selectFieldName);
            rowDimCodeList.add(selectFragment.toString());
        }

        return rowDimCodeList;
    }

    /**
     * 构建指标表达式
     * @param measureField
     * @param colItemField
     * @return
     */
    protected String buildSelectMeasureExpression(String fieldCode, QueryField colItemField){
        List<QueryField> colItemPathFields = this.getDimItemFieldPath(colItemField);
        String charStr = "'";
        List<String> conditions = new ArrayList<>();
        for (QueryField f : colItemPathFields) {
            conditions.add(
                    tableAlias + "." + f.getSourceField().getCode() + " = " + charStr + f.getValues().get(0).getId() + charStr
            );
        }
        String condition = BIUtil.listToStr(conditions, " and ");
        String ifExpression = fx.ifExpression(condition, tableAlias + "." + fieldCode, "null");

        String expression = fx.max(ifExpression);
        return expression;
    }

    /**
     * 获取指标别名
     * @param measureField
     * @param colItemField
     * @return
     */
    protected String getMeasureAlias(ResultDataSetColumn column, QueryField colItemField){
        String fieldCode = column.getCode();
        String itemCode = colItemField.getCode();
        String fieldAlias = fieldCode + colItemField.getCode();
        // 行总计
        if(BIConsts.ROW_TOTAL_COLUMN_CODE.equalsIgnoreCase(itemCode)){
            // 若计算方式不为空，则通过其父code获取其原始指标code（不含同环比的code）
            if(BIUtil.isNotEmpty(column.getCalcMode())){
                // 同环比、占比、自定义对比等时，其code格式=原始指标code_r_total_同环占比标识
                String parentCode = column.getParentCode().replace("/", "");
                String replacement = String.format("%s_%s_", parentCode, AnalysisCalcMode.ROW_TOTAL.getCode());
                fieldAlias = fieldCode.replace(parentCode + "_", replacement);
            }else{
                fieldAlias = String.format("%s_%s", fieldCode, AnalysisCalcMode.ROW_TOTAL.getCode());
            }
            /*
            fieldAlias = fieldCode + BIConsts.ROW_TOTAL_COLUMN_CODE;
            fieldAlias = fieldAlias.replace(BIConsts.ROW_TOTAL_COLUMN_CODE, "_" + AnalysisCalcMode.ROW_TOTAL.getCode());
             */
        }
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
        // 去掉order by
        String sourceSql = subQuerySql;
        if(sourceSql.contains(BIConsts.ORDER_BY)){
            sourceSql = sourceSql.substring(0, sourceSql.indexOf(BIConsts.ORDER_BY));
        }
        sql.append(" (").append(sourceSql).append(") ").append(tableAlias);
        return sql;
    }

    /**
     * 添加过滤空行逻辑
     * @return
     */
    protected StringBuilder buildWhereClause() {
        StringBuilder sql = new StringBuilder();
        // 是否过滤空行
        if(!config.getSettings().getFilterEmptyRow()){
            return sql;
        }
        List<ResultDataSetColumn> subQueryLeafColumns = this.getSubQueryLeafColumns();
        List<ResultDataSetColumn> subQueryMeasureColumns = subQueryLeafColumns.stream()
                                        .filter(c->QueryArea.get(c.getRawQueryArea()) == QueryArea.Measure).collect(Collectors.toList());
        if(BIUtil.isEmpty(subQueryMeasureColumns)){
            return sql;
        }
        List<String> fragments = new ArrayList<>();
        for(ResultDataSetColumn column : subQueryMeasureColumns){
            String expr = fx.coalesce(column.getCode(), "0");
            fragments.add(expr);
        }
        if(BIUtil.isNotEmpty(fragments)){
            sql.append(" where ");
            sql.append(String.format(" %s <> 0 ", BIUtil.listToStr(fragments, "+")));
        }
        return sql;
    }

    protected StringBuilder buildGroupByClause() {
        StringBuilder sql = new StringBuilder();
        /** 行维度 */
        List<String> groupByFragments = getRowDimCodeList();

        /*
        if(config.hasAnalysis()) {
            groupByFragments.add(BIConsts.GROUPING_VALUE);
            groupByFragments.add(BIConsts.GROUPING_KEY);
        }
         */

        if (BIUtil.isEmpty(groupByFragments)) {
            return sql;
        }
        sql.append(" group by ");
        sql.append(BIUtil.listToStr(groupByFragments));
        return sql;
    }

    protected StringBuilder buildOrderByClause() {
        StringBuilder orderBySQL = new StringBuilder();
        if (!config.getSettings().getNeedSort()) {
            return orderBySQL;
        }

//        不从子查询中获取排序，重新构建排序
//        int sortByIndex = this.subQuerySql.indexOf(BIConsts.ORDER_BY);
//        String subQueryOrderBySql = this.subQuerySql.substring(sortByIndex);
//        sql.append(" ").append(subQueryOrderBySql);

        List<String> orderByFragments = new ArrayList<String>();

        List<String> resultFieldCodes = new ArrayList<>();
        for (ResultDataSetColumn column : this.leafColumns) {
            resultFieldCodes.add(column.getCode());
        }

        orderByFragments.addAll(FieldUtil.getOrderByFragments(config, resultFieldCodes));
        List<String> sortFieldCodes = config.getSettings().getQuerySortFieldCodes(config);

        List<QueryField> orderByFields = new ArrayList<>();
        AnalysisTotalType totalType = AnalysisUtil.getOrderByTotalType(config);
        if (totalType.isActive()) {
            TotalOperator totalOperator = OperatorFactory.getTotalOperator(totalType);
            orderByFields = totalOperator.orderByFields(config);
        }

        boolean hasGroupValueField = false;
        for (QueryField field : orderByFields) {

            //已排序，不再处理
            if (sortFieldCodes.contains(field.getCode())) {
                continue;
            }

            FieldSortType sortType = field.getSortType();
            if (sortType == FieldSortType.NONE) {
                sortType = FieldSortType.ASC;
            }

            String orderByExpression = FieldUtil.getFieldValueSortExpression(field.getCode(), field);
            orderByExpression = String.format("%s %s nulls first", orderByExpression, sortType.getCode());
            orderByFragments.add(orderByExpression);

            hasGroupValueField = hasGroupValueField || field.getCode().equals(BIConsts.GROUPING_VALUE);
        }

        // 设置列总计排到最前面
        if (hasGroupValueField) {

            List<QueryField> dimFields = config.getResult().getRowDimensions().stream().filter(f -> !f.isAppend()).collect(Collectors.toList());
            Long colDimCount = config.getResult().getFields().stream().filter(f -> !f.isAppend()).filter(f -> f.getRawQueryArea() == QueryArea.ColumnDimension).count();

            String fieldGroupingBit = StringUtils.rightPad("", 0, "0");
            String groupingBit = StringUtils.rightPad(fieldGroupingBit, dimFields.size(), "1") + StringUtils.rightPad("", colDimCount.intValue(), "0");
            int groupingValue = NumberUtil.binaryToInt(groupingBit);

            String groupingValueField = BIConsts.GROUPING_KEY;
            String colTotalOrderByFragment = String.format("if(%s=%s, 1, 0) desc nulls last", groupingValueField, groupingValue);
            if (colDimCount > 0) {
                colTotalOrderByFragment = String.format("if(%s in (%s,%s), 1, 0) desc nulls last", groupingValueField, groupingValue, groupingValue + 1);
            }

            orderByFragments.add(0, colTotalOrderByFragment);
        }

        if (orderByFragments.isEmpty()) {
            return orderBySQL;
        }

        orderBySQL.append(" ").append(BIConsts.ORDER_BY).append(" ");
        orderBySQL.append(BIUtil.listToStr(orderByFragments, ","));

        return orderBySQL;
    }

    /**
     * 创建维度item字段
     * @return
     */
    protected List<QueryField> createColumnDimensionItemFields(){
        List<QueryField> itemFields = new ArrayList<>();
        List<String> itemValues = config.getResult().getPivotConfig().getColDimValues().stream().distinct().collect(Collectors.toList());
        List<QueryField> columnFields = config.getResult().getPivotConfig().getColDimensions();
        // 仅支持1个列维度
        if(BIUtil.isEmpty(itemValues) || BIUtil.isEmpty(columnFields)){
            return itemFields;
        }
        QueryField columnField = columnFields.get(0);
        for(int i = 0; i < itemValues.size(); i++){
            String itemValue = itemValues.get(i);
            QueryField itemField = new QueryField(columnField.getMeta());
            String itemCode = BIConsts.COLUMN_DIM_FIELD_SUFFIX + "0_" + i;
            itemField.setId(itemValue + BIConsts.SEPARATOR);
            itemField.setName(itemCode);
            itemField.setQueryDateGranularity(columnField.getQueryDateGranularity());

            String title = FieldUtil.getPivotItemFieldTitle(itemField, itemValue, this.engine);
            // 行总计
            if(BIConsts.ROW_TOTAL_COLUMN_CODE.equalsIgnoreCase(itemValue)){
                title = BIConsts.ROW_TOTAL_COLUMN_TITLE;
                itemField.setCode(BIConsts.ROW_TOTAL_COLUMN_CODE);
            }else{
                itemField.setCode(itemCode);
            }
            itemField.setTitle(title + "");
            itemField.setDisplayTitle(isEmpty(title) ? " " : title);

            itemField.getValues().add(new FieldValue(itemValue, itemValue));
            itemField.setRawCode(columnField.getCode());
            itemField.setQueryArea(QueryArea.ColumnDimension);

            // 设置来源字段
            itemField.setSourceField(columnField);

            itemFields.add(itemField);
        }
        return itemFields;
    }

    /**
     * 获取子查询的root列
     * @return
     */
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
            subQueryAllColumns.put(parent.getCode(), parent);
            List<ResultDataSetColumn> children = parent.getChildren();
            // 无下级，则未末级节点
            if(BIUtil.isEmpty(children)){
                leafColumns.add(parent);
            }else{
                getSubQueryLeafColumnsCascade(children, leafColumns);
            }
        }
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

    public List<ResultDataSetColumn> getRootColumns() {
        return rootColumns;
    }

    public void setRootColumns(List<ResultDataSetColumn> rootColumns) {
        this.rootColumns = rootColumns;
    }
}

