package com.bi.queryer.ssm.engine.analysis;

import com.bi.queryer.ssm.engine.DefaultDataSourceSqlBuilderAvgWrapper;
import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.analysis.cfg.total.AnalysisTotalItemConfig;
import com.bi.queryer.ssm.engine.analysis.operator.BaseOperator;
import com.bi.queryer.ssm.engine.analysis.operator.OperatorFactory;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @Author contributor
 * @Date 16:51 2024/8/29
 * 解决场景：日均去重指标的在小计、总计时需按维度计算日均
 * @Description 分析函数的数据源日均sql构建器包装类
 **/
public class AnalysisDataSourceSqlBuilderAvgWrapper extends DefaultDataSourceSqlBuilderAvgWrapper {

    public AnalysisDataSourceSqlBuilderAvgWrapper(String datasourceSql, StarModel model, QueryConfigure config, QueryContext cxt) {
        super(datasourceSql, model, config, cxt);
    }

    @Override
    protected List<String> buildSelectFragments() {
        List<String> fragments = super.buildSelectFragments();
        if(!this.isNeedWrap){
            return fragments;
        }
        // 添加小计总计标识
        String groupingExpression = "";
        List<String> groupByFragments = this.buildGroupByFragments();
        if (BIUtil.isNotEmpty(groupByFragments)) {
            groupingExpression = fx.groupingId(groupByFragments); //String.format("grouping(%s)", BIUtil.listToStr(groupByFragments, ","));
        } else {
            groupingExpression =  "sum(0)"; // 此处需聚合常量，不使用常量，避免日均值在无行维度时计算结果重复。场景：日店均支付订单数（日均）、日期（汇总）
        }
        fragments.add(String.format("%s as %s", groupingExpression, BIConsts.GROUPING_VALUE));
        return fragments;
    }

    @Override
    protected StringBuilder buildGroupByClause() {
        if(!this.isNeedWrap){
            return super.buildGroupByClause();
        }
        // 若需要被包装，则说明此处需要进行汇总计算
        StringBuilder groupBySQL = new StringBuilder();
        List<String> fieldNameList = this.buildGroupByFragments();
        if (fieldNameList.isEmpty()) {
            return groupBySQL;
        }

        Set<String> groupingSets = new LinkedHashSet<>();

        // 分组统计，等同 group by
        groupingSets.add(String.format("(%s)", BIUtil.listToStr(fieldNameList)));

        if(config.getAnalysis().getTotal().isActive()) {
            List<AnalysisTotalItemConfig> itemConfigs = config.getAnalysis().getTotal().getItems();
            for (AnalysisTotalItemConfig itemConfig : itemConfigs) {
                BaseOperator totalOperator = OperatorFactory.getTotalOperator(itemConfig.getTotalType());
                if (totalOperator != null) {
                    List<String> groupingSetTotalExpressions = totalOperator.groupingSets(config, model);
                    groupingSets.addAll(groupingSetTotalExpressions);
                }
            }
        }

        // 修正groping sets内容
        groupingSets = this.repairGroupingSets(groupingSets);

        // 去重：避免重复计算导致记录重复
        groupingSets = AnalysisUtil.distinctGroupingSets(groupingSets);

        groupBySQL.append(String.format(" group by grouping sets (%s) ", BIUtil.listToStr(groupingSets)));

        return groupBySQL;
    }

    /**
     * 1、替换grouping sets中的表别名
     * 2、若没有日期字段，则添加进去，因为需要要按日期去重
     * @param groupingSetTotalExpressions
     * @return
     */
    protected Set<String> repairGroupingSets(Set<String> groupingSetTotalExpressions){
        if(BIUtil.isEmpty(groupingSetTotalExpressions)){
            return groupingSetTotalExpressions;
        }
        Set<String> newExprList = new LinkedHashSet<>();
        for(String expr : groupingSetTotalExpressions){
            expr = expr.replace("(","").replace(")", "");
            String[] fieldFullNames = expr.split(",");
            List<String> groupingItemFields = new ArrayList<>();
            for(String fieldFullName : fieldFullNames){
                if(BIUtil.isEmpty(fieldFullName)){
                    continue;
                }
                String[] fieldNames = fieldFullName.split("\\.");
                String newFieldFullName = String.format("%s.%s", dsTableAlias, fieldNames[fieldNames.length - 1]);
                groupingItemFields.add(newFieldFullName);
            }

            // 不包含日粒度字段，则添加，用于按日去重
            boolean isContainsDayField = expr.contains(BIConsts.MIN_DATE_GRANULARITY_CODE);
            if(!isContainsDayField){
                groupingItemFields.add(String.format("%s.%s", dsTableAlias, BIConsts.MIN_DATE_GRANULARITY_CODE));
            }
            newExprList.add(String.format("(%s)", BIUtil.listToStr(groupingItemFields)));
        }
        return newExprList;
    }

    @Override
    protected List<String> buildGroupByFragments() {
        List<String> groupByFragments = super.buildGroupByFragments();
        return groupByFragments;
    }
}
