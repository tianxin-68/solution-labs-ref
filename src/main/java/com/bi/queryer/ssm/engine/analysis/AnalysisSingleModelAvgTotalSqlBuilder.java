package com.bi.queryer.ssm.engine.analysis;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.aggregation.AggregatorContext;
import com.bi.queryer.ssm.engine.aggregation.AnalysisAverageByDayAggregator;
import com.bi.queryer.ssm.engine.aggregation.AnalysisAverageByRealDayAggregator;
import com.bi.queryer.ssm.engine.analysis.operator.BaseOperator;
import com.bi.queryer.ssm.engine.analysis.operator.OperatorFactory;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.ssm.enums.AggExpressionType;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;
import com.bi.queryer.ssm.enums.AnalysisTotalType;
import com.bi.queryer.ssm.enums.QueryArea;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.StringUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 18:51 2023-07-10
 * @Description
 * 日均小计总计sql构建器
 * 场景1：用于去重日均小计总计单独计算逻辑
 * 场景2：用于日粒度+日期不汇总查询+行列汇总值需要计算日均
 **/
public class AnalysisSingleModelAvgTotalSqlBuilder extends AnalysisSingleModelSqlBuilder {
    protected boolean hasTotal = false;
    public AnalysisSingleModelAvgTotalSqlBuilder(QueryConfigure config, QueryContext cxt) {
        super(config, cxt);
        hasTotal = config.getAnalysis().getTotal().isActive();
    }

    /**
     * 单个模型构建select
     * 若有小计/总计，grouping值从子查询中获取（子查询中作为一个维度）
     * @param model
     * @return
     */
    @Override
    public List<String> buildSelectFragments(StarModel model) {
        List<String> selectFragments = super.buildSelectFragments(model);
        if(!hasTotal){
            return selectFragments;
        }
        // 删除原因的grouping value
        String groupingValueFragment = "";
        for(String fragment : selectFragments){
            if(fragment.contains(BIConsts.GROUPING_VALUE)){
                groupingValueFragment = fragment;
            }
        }
        selectFragments.remove(groupingValueFragment);

        // 添加子查询中的grouping value维度
        selectFragments.add(String.format("%s.%s", model.getFactTable().getAlias(), BIConsts.GROUPING_VALUE));

        return selectFragments;
    }

    /**
     * 过滤掉明细数据：只计算汇总数据
     * @param model
     * @return
     */
    @Override
    protected List<String> buildWhereFragments(StarModel model) {
        List<String> fragments = super.buildWhereFragments(model);
        fragments.add(String.format("%s.%s > 0 ", model.getFactTable().getAlias(), BIConsts.GROUPING_VALUE));
        return fragments;
    }

    /**
     * 只保留第一个grouping set,同时将grouping value作为一个维度添加到groping sets中
     * @param model
     * @return
     */
    @Override
    protected Set<String> buildGroupingSets(StarModel model) {
        Set<String> groupingSets = super.buildGroupingSets(model);
        List<String> groupByFieldList = new ArrayList<>();
        if(BIUtil.isNotEmpty(groupingSets)){
            String groupingSet = groupingSets.iterator().next();
            String[] groupByFields = groupingSet.replace("(", "").replace(")", "").split(",");
            groupByFieldList = Arrays.stream(groupByFields).collect(Collectors.toList());
        }
        // 添加grouping value维度
        groupByFieldList.add(String.format("%s.%s", model.getFactTable().getAlias(), BIConsts.GROUPING_VALUE));
        Set<String> newGroupingSets = new LinkedHashSet<>();
        newGroupingSets.add(String.format("(%s)", BIUtil.listToStr(groupByFieldList)));
        return newGroupingSets;
    }

    /**
     * 日粒度且日期不汇总时，有行列总计需要取日均值
     * @param sourceSelectFragments
     * @return
     */
    public List<String> buildSelectFragmentsOnDayNotAggQuery(List<String> sourceSelectFragments, AggregatorContext aggregatorContext){
        if(!SSDUtil.hasTotalNeedToAvgOnDayNotAgg(config)){
            return sourceSelectFragments;
        }
        Map<String, String> fieldExprMap = new LinkedHashMap<>();
        int groupingFieldCount = 0;
        String groupingValueExpr = "";

        //日均的字段数量
        int avgByDayFieldCount = 0;
        for(String fragment : sourceSelectFragments) {
            if (BIUtil.isEmpty(fragment)) {
                continue;
            }
            String[] items = fragment.split("as | AS ");
            if (items.length == 0) {
                continue;
            }
            String expr = String.join(" as " , Arrays.asList(items).subList(0, items.length - 1));
            String alias = items[items.length - 1].trim();
            fieldExprMap.put(alias, expr);

            if (BIConsts.GROUPING_VALUE.equalsIgnoreCase(alias)) {
                groupingFieldCount = StringUtil.countMatches(expr.toLowerCase(), "if(");
                groupingValueExpr = expr;
            }

            if (alias.endsWith(AggExpressionType.Avg_By_Day.getCode()) || alias.endsWith(AggExpressionType.Avg_By_Day_Real.getCode())) {
                avgByDayFieldCount++;
            }
        }

        // 无行维度参与grouping sets
        if(groupingFieldCount == 0 || BIUtil.isEmpty(groupingValueExpr)){
            return sourceSelectFragments;
        }

        // 没有日均指标不处理
        if(avgByDayFieldCount == 0){
            return sourceSelectFragments;
        }

        QueryField commonDateField = config.getFilterCommonDateField();
        if(commonDateField == null){
            return sourceSelectFragments;
        }
        /**
         * 需要计算汇总日均的场景
         * 1、日期在行上时，列总计需要计算日均，否则不计算。若列上有其他维度，则GroupingValue = binaryToInt(所有维度) - 1
         * 2、日期在列上时，行总计需要计算日均，否则不计算。
         */
        List<String> newFragments = new ArrayList<>();
        List<Integer> totalGroupingValueList = getTotalGroupingValueList();

        for(String alias : fieldExprMap.keySet()){

            String expr = fieldExprMap.get(alias);
            String daysExpr = "";
            // 日均
            if(alias.endsWith(AggExpressionType.Avg_By_Day.getCode())){
                AnalysisAverageByDayAggregator aggregator = new AnalysisAverageByDayAggregator(aggregatorContext);
                daysExpr = aggregator.getFilterDaysExpression(commonDateField);
            }
            // 非空日均
            if(alias.endsWith(AggExpressionType.Avg_By_Day_Real.getCode())){
                AnalysisAverageByRealDayAggregator aggregator = new AnalysisAverageByRealDayAggregator(aggregatorContext);
                daysExpr = aggregator.getFilterDaysExpression(commonDateField);
            }
            if(BIUtil.isNotEmpty(daysExpr)) {
                if (CollUtil.isNotEmpty(totalGroupingValueList)) {
                    String newAvgExpr = String.format("(%s)/if((%s) in (%s), %s, 1)",
                            expr, groupingValueExpr
                            , BIUtil.listToStr(totalGroupingValueList), daysExpr);
                    expr = newAvgExpr;
                }
            }
            newFragments.add(String.format("%s as %s", expr, alias));
        }

        return newFragments;
    }

    /**
     * 获取总计的的grp_v
     * 场景1: 日期在行上，grp_v为列总计的值
     * 场景2: 日期在列上，grp_v为行总计的值
     * @return
     */
    public List<Integer> getTotalGroupingValueList() {

        List<Integer> totalGroupingValueList = new ArrayList<>();

        //日期是否在列上
        boolean isDateInCol = false;
        List<QueryField> colDimensions = config.getResult().getFields().stream()
                .filter(f -> !f.isAppend() && f.getRawQueryArea() == QueryArea.ColumnDimension)
                .collect(Collectors.toList());

        if (CollUtil.isNotEmpty(colDimensions)) {
            if (colDimensions.stream().filter(f -> f.isCommonDate()).count() > 0) {
                isDateInCol = true;
            }
        }

        //获取行总计的grp_v
        List<QueryField> dimFields = config.getResult().getFields().stream()
                .filter(f -> !f.isAppend() && f.getRawQueryArea() == QueryArea.RowDimension)
                .collect(Collectors.toList());

        Integer colDimCount = colDimensions.size();
        String fieldGroupingBit = StringUtils.rightPad("", -1, "0");
        String groupingBit = StringUtils.rightPad(fieldGroupingBit, dimFields.size(), "1") + StringUtils.rightPad("", colDimCount.intValue(), "0");

        if (StrUtil.isEmpty(groupingBit)) {
            return totalGroupingValueList;
        }

        int colTotalGroupingValue = NumberUtil.binaryToInt(groupingBit);

        //日期在列上，取行总计的grp_v
        if (isDateInCol) {

            BaseOperator operator = OperatorFactory.getOperator(AnalysisCalcMode.ROW_TOTAL);

            QueryConfigure rowTotalConfig = new QueryConfigure();

            if (CollUtil.isNotEmpty(dimFields)) {
                rowTotalConfig.getResult().getRowDimensions().addAll(dimFields);
                rowTotalConfig.getResult().getFields().addAll(dimFields);
            }

            if (CollUtil.isNotEmpty(colDimensions)) {
                rowTotalConfig.getResult().getColDimensions().addAll(colDimensions);
                rowTotalConfig.getResult().getFields().addAll(colDimensions);
            }

            rowTotalConfig.setAnalysis(config.getAnalysis());

            List<Integer> groupingValues = operator.getGroupingValues(rowTotalConfig);
            if (CollUtil.isNotEmpty(groupingValues)) {
                totalGroupingValueList.addAll(groupingValues);
            }

        } else {

            //日期在行上，取列总计的grp_v
            totalGroupingValueList.add(colTotalGroupingValue);
            if (colDimCount > 0) {
                totalGroupingValueList.add(colTotalGroupingValue + 1);
            }
        }

        return totalGroupingValueList;
    }

}
