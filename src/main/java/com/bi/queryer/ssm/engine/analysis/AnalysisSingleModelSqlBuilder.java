package com.bi.queryer.ssm.engine.analysis;

import com.bi.queryer.ssm.engine.DefaultDataSourceSqlBuilder;
import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.SingleModelSqlBuilder;
import com.bi.queryer.ssm.engine.aggregation.AggregatorContext;
import com.bi.queryer.ssm.engine.aggregation.AggregatorFactory;
import com.bi.queryer.ssm.engine.aggregation.DefaultAggregator;
import com.bi.queryer.ssm.engine.analysis.cfg.AnalysisDataSourceCfg;
import com.bi.queryer.ssm.engine.analysis.cfg.total.AnalysisTotalConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.total.AnalysisTotalItemConfig;
import com.bi.queryer.ssm.engine.analysis.operator.BaseOperator;
import com.bi.queryer.ssm.engine.analysis.operator.OperatorFactory;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;
import com.bi.queryer.ssm.util.FieldUtil;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIUtil;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 18:51 2023-07-10
 * @Description 分析模型sql构建器
 **/
public class AnalysisSingleModelSqlBuilder extends SingleModelSqlBuilder {
    /**
     * 是否是对比(同环比、自定义对比）
     */
    protected boolean isCompare = false;

    /**
     * 是否构建分组总计/小计
     */
    protected boolean buildTotalGrouping = false;

    /**
     * 分析计算方式
     */
    protected AnalysisCalcMode analysisCalcMode;

    /**
     * 自定义对比的索引
     */
    protected Integer customCompareIndex = -1;

    /**
     * 偏移的日期表达式
     */
    protected String offsetDateExpression;


    public AnalysisSingleModelSqlBuilder(QueryConfigure config, QueryContext cxt) {
        super(config, cxt);
    }

    /**
     * 单个模型构建select
     *
     * @param model
     * @return
     */
    @Override
    public List<String> buildSelectFragments(StarModel model) {
        List<String> selectFragments = super.buildSelectFragments(model);
        /**
        // 废弃：行列总计日均值特殊处理
         原因：日粒度不汇总，但有列总计时也需要计算日均。统一走日粒度的均值计算逻辑
        AnalysisSingleModelAvgTotalSqlBuilder totalSqlBuilder = new AnalysisSingleModelAvgTotalSqlBuilder(config, cxt);
        AggregatorContext aggregatorContext = new AggregatorContext(config, analysisCalcMode,customCompareIndex, isCompare);
        selectFragments = totalSqlBuilder.buildSelectFragmentsOnDayNotAggQuery(selectFragments, aggregatorContext);
         */
        return selectFragments;
    }

    /**
     * 重写：修改自定义对比的日均值计算方式
     * @param field
     * @return
     */
    @Override
    protected String getSelectFieldFullName(QueryField field) {
        // 维度直接返回
        if (Enabled.isFalse(field.getMeta().getIsMeasure())) {
            return super.getSelectFieldFullName(field);
        }

        // 不是同环比和自定义对比则返回
        /*
        if (AnalysisCalcMode.CUSTOM_COMPARE != analysisCalcMode && !compare) {
            return super.getSelectFieldFullName(field);
        }

        DefaultAggregator aggregator = AggregatorFactory.getAnalysisAggregator(field.getAggExpressionType());
        AggregatorContext aggregatorContext = new AggregatorContext(config, analysisCalcMode,customCompareIndex, compare);
        String aggregateExpression = aggregator.aggregate(field, aggregatorContext);
         */
        AggregatorContext aggregatorContext = new AggregatorContext(config, analysisCalcMode,customCompareIndex, isCompare);
        DefaultAggregator aggregator = AggregatorFactory.getAnalysisAggregator(field.getAggExpressionType(), aggregatorContext);
        String aggregateExpression = "";

        // 有对比
        if (isCompare) {
            aggregateExpression = aggregator.aggregate(field);
        }else {
            aggregateExpression = super.getSelectFieldFullName(field);
        }

        return aggregateExpression;
    }

    @Override
    protected StringBuilder buildFromClause(StarModel model) {
        /*
           // 去掉下面逻辑。原因：用于日均的小计总计时，需要对sql进行重写  2024-08-29 by contributor
        if (!appendThbFilterDate) {
            return super.buildFromClause(model);
        }*/
        //AnalysisDataSourceSqlBuilder datasourceSqlBuilder = new AnalysisDataSourceSqlBuilder(model, config, cxt);

        AnalysisDataSourceCfg cfg = new AnalysisDataSourceCfg();
        cfg.setAnalysisCalcMode(analysisCalcMode);
        cfg.setCustomCompareIndex(customCompareIndex);
        cfg.setOffsetDateExpression(offsetDateExpression);
        DefaultDataSourceSqlBuilder datasourceSqlBuilder = AnalysisDataSourceSqlBuilderFactory.createAnalysisDataSourceSqlBuilder(model, config, cxt,cfg);

        //datasourceSqlBuilder.setAnalysisCalcMode(analysisCalcMode);
        //datasourceSqlBuilder.setCustomCompareIndex(customCompareIndex);
        //datasourceSqlBuilder.setOffsetDateExpression(offsetDateExpression);

        StringBuilder fromSql = new StringBuilder();
        String datasetSql = datasourceSqlBuilder.build();

        final String compareTableAlias = model.getFactTable().getAlias();
        fromSql.append(String.format(" from (%s) %s", datasetSql, compareTableAlias));
        return fromSql;
    }

    protected StringBuilder buildWhereClause(StarModel model) {
        return super.buildWhereClause(model);
    }

    protected StringBuilder buildGroupByClause(StarModel model) {
        AnalysisTotalConfig totalCfg = config.getAnalysis().getTotal();
        if (!totalCfg.isActive() || !buildTotalGrouping) {
            return super.buildGroupByClause(model);
        }

        StringBuilder groupBySQL = new StringBuilder();

        Set<String> groupingSets = buildGroupingSets(model);
        if(BIUtil.isNotEmpty(groupingSets)) {
            groupBySQL.append(String.format(" group by grouping sets (%s) ", BIUtil.listToStr(groupingSets)));
        }

        return groupBySQL;
    }

    /**
     * 构建grouping sets
     * @param model
     * @return
     */
    protected Set<String> buildGroupingSets(StarModel model){
        Set<String> groupingSets = new LinkedHashSet<>();
        List<QueryField> resultFields = model.getFields().stream().filter(f -> f.getIsResult()).collect(Collectors.toList());
        if (resultFields == null || resultFields.size() == 0) {
            return groupingSets;
        }

        // 按config中行列维度的排序，保障后续的grouping的顺序和查询顺序一致
        resultFields = FieldUtil.sortByShowOrder(config, resultFields);

//        Collections.sort(resultFields);
        List<String> fieldNameList = new ArrayList<String>();
        for (QueryField field : resultFields) {
            if (field.isAppend()) { // 若是计算字段附带的结果字段，则不进行分组计算
                continue;
            }
            // 只取维度
            if (!field.isMeasure() && model.isExistsByMetaCode(field)) {
                String fieldName = this.getSelectFieldFullName(field);
                if (BIUtil.isNotEmpty(fieldName)) {
                    fieldNameList.add(fieldName);
                }
            }
        }

        if (fieldNameList.isEmpty()) {
            return groupingSets;
        }

        // 分组统计，等同 group by
        groupingSets.add(String.format("(%s)", BIUtil.listToStr(fieldNameList)));

        AnalysisTotalConfig totalCfg = config.getAnalysis().getTotal();
        List<AnalysisTotalItemConfig> itemConfigs = totalCfg.getItems();
        for (AnalysisTotalItemConfig itemConfig : itemConfigs) {
            BaseOperator totalOperator = OperatorFactory.getTotalOperator(itemConfig.getTotalType());
            if (totalOperator != null) {
                List<String> groupingSetTotalExpressions = totalOperator.groupingSets(config, model);
                groupingSets.addAll(groupingSetTotalExpressions);
            }
        }

        // 去重：避免重复计算导致记录重复
        groupingSets = AnalysisUtil.distinctGroupingSets(groupingSets);

        return groupingSets;
    }

    /**
     * 复制属性：将当前类属性值从指定类copy
     */
    protected void copyPropertiesFrom(AnalysisSingleModelSqlBuilder src){
        this.setCompare(src.isCompare);
        this.setBuildTotalGrouping(src.buildTotalGrouping);
        this.setAnalysisCalcMode(src.analysisCalcMode);
        this.setCustomCompareIndex(src.customCompareIndex);
        this.setOffsetDateExpression(src.offsetDateExpression);
    }


    public boolean isCompare() {
        return isCompare;
    }

    public void setCompare(boolean compare) {
        this.isCompare = compare;
    }

    public boolean isBuildTotalGrouping() {
        return buildTotalGrouping;
    }

    public void setBuildTotalGrouping(boolean buildTotalGrouping) {
        this.buildTotalGrouping = buildTotalGrouping;
    }

    public AnalysisCalcMode getAnalysisCalcMode() {
        return analysisCalcMode;
    }

    public void setAnalysisCalcMode(AnalysisCalcMode analysisCalcMode) {
        this.analysisCalcMode = analysisCalcMode;
    }

    public Integer getCustomCompareIndex() {
        return customCompareIndex;
    }

    public void setCustomCompareIndex(Integer customCompareIndex) {
        this.customCompareIndex = customCompareIndex;
    }

    public String getOffsetDateExpression() {
        return offsetDateExpression;
    }

    public void setOffsetDateExpression(String offsetDateExpression) {
        this.offsetDateExpression = offsetDateExpression;
    }
}
