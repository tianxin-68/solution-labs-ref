package com.bi.queryer.ssm.engine;

import com.bi.queryer.ssm.engine.aggregation.AggregatorContext;
import com.bi.queryer.ssm.engine.aggregation.DefaultAggregator;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.function.FunctionManager;
import com.bi.queryer.ssm.engine.function.IFunction;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.ssm.engine.promotion.PromotionConsts;
import com.bi.queryer.ssm.enums.AggExpressionType;
import com.bi.queryer.ssm.enums.FieldFilterMode;
import com.bi.queryer.ssm.util.FieldUtil;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 17:52 2023-09-25
 * @Description 默认sql数据源平均值的包装类
 **/
public class DefaultDataSourceSqlBuilderAvgWrapper {
    protected final String dsTableAlias = "v2";
    protected String datasourceSql = "";
    protected QueryConfigure config;
    protected QueryContext cxt;
    protected StarModel model;
    protected IFunction fx = null;
    protected QueryField commonDateField = null;
    protected boolean isNeedWrap = false;

    public DefaultDataSourceSqlBuilderAvgWrapper(String datasourceSql, StarModel model, QueryConfigure config, QueryContext cxt){
        this.datasourceSql = datasourceSql;
        this.model = model;
        this.config = config;
        this.cxt = cxt;
        this.fx = FunctionManager.getFunction();
        this.commonDateField = config.getFilterCommonDateField();
    }

    /**
     * 针对有去重指标的场景且计算日均值时需要包装
     * @return
     */
    public String wrap(){
        this.prepare();

        if(!isNeedWrap){
            return datasourceSql;
        }
        if(commonDateField == null){
            return datasourceSql;
        }

        StringBuilder wrapSql = new StringBuilder();
        wrapSql.append(buildSelectClause());
        wrapSql.append(buildFromClause());
        wrapSql.append(buildGroupByClause());

        this.post();
        return wrapSql.toString();
    }

    /**
     * 前置处理：
     * 1、判断是否符合包装条件：有去重指标的场景且计算日均值时需要包装
     */
    protected void prepare(){
        List<QueryField> resultFields = this.getSelectFields();
        if(BIUtil.isEmpty(resultFields)){
            return;
        }

        // 日粒度且不汇总，则不处理
        QueryField commonDateField = config.getFilterCommonDateField();
        if(commonDateField == null){
            return;
        }
        /**
         * 废弃
         * 原因：日粒度不汇总，但有列总计时也需要计算日均
        if(!commonDateField.isAggQuery() && DateGranularity.DAY == DateGranularity.get(commonDateField.getQueryDateGranularity())){
            return;
        }
         */

        for(QueryField f : resultFields){
            if(!f.isMeasure() || f.isVirtual()){
                continue;
            }
            String aggExpression = f.getMeta().getAggExpression();
            //if(AggExpressionType.get(aggExpression) != AggExpressionType.Count_Distinct && !aggExpression.toLowerCase().contains(BIConsts.COUNT_DISTINCT_FLAG)) {
            if(!BIUtil.isCountDistinctAggExpression(aggExpression)){
                continue;
            }

            AggExpressionType aggExpressionType = AggExpressionType.get(f.getAggExpressionType());
            if(aggExpressionType == AggExpressionType.Avg_By_Day || aggExpressionType == AggExpressionType.Avg_By_Day_Real){
                isNeedWrap = true;
                break;
            }
        }
    }

    protected void post(){

    }

    public StringBuilder buildSelectClause() {
        StringBuilder selectSql = new StringBuilder();

        List<String> selectFragments = this.buildSelectFragments();
        if (BIUtil.isEmpty(selectFragments)) {
            return selectSql;
        }

        String selectFieldStr = BIUtil.listToStr(selectFragments, ",");
        selectSql.append(String.format(" select %s ", selectFieldStr));
        return selectSql;
    }

    protected List<String> buildSelectFragments(){
        List<QueryField> selectFields = this.getSelectFields();

        // 按查询配置的字段顺序排序：行维度+列维度+指标
        // 废弃：获取的字段已排序
        // selectFields = selectFields.stream().sorted(Comparator.comparing(QueryField::isMeasure).thenComparing(QueryField::getCode)).collect(Collectors.toList());

        List<String> selectFragments = new ArrayList<>();
        for (QueryField field : selectFields) {

            // 用户自定义指标的计算字段不此处添加，在外层处理
            if (field.isCustomMeasure()) {
                // 用户自定义指标放到最后外层sql中，此处不处理，
                continue;
            }

            if(field.isAppend() && field.isDimension()){
                // 若是附加维度，在select子句中不能出现，附加指标需要，避免group by时有附加维度，导致指标计算错误
                continue;
            }

            String fieldFullName = dsTableAlias + "." + field.getCode();

            // 指标
            if(field.isMeasure()){
                DefaultAggregator aggregator = new DefaultAggregator(new AggregatorContext(config));
                // AggregatorContext aggCxt = new AggregatorContext(config);
                fieldFullName = aggregator.aggregate(field);
                fieldFullName = fieldFullName.replace(field.getTable().getAlias() + ".", dsTableAlias + ".");
            }
            if (field.isVirtual()) {
                fieldFullName = "null"; // 虚拟字段
            }
            selectFragments.add(String.format("%s as %s", fieldFullName, field.getCode()));
        }

        if (BIUtil.isEmpty(selectFragments)) {
            return selectFragments;
        }

        //业务日历添加持续天数、时间进度字段
        if(config.getSettings().isBusinessCalendar()){
            selectFragments.add(String.format("max(%s.%s) as %s",dsTableAlias,
                    PromotionConsts.PROMOTION_DURATION_DAYS_CODE,PromotionConsts.PROMOTION_DURATION_DAYS_CODE));

            selectFragments.add(String.format("max(%s.%s) as %s",dsTableAlias,
                    PromotionConsts.PROMOTION_DATE_RANGE_PROGRESS_CODE,PromotionConsts.PROMOTION_DATE_RANGE_PROGRESS_CODE));

        }

        // 添加最细粒度的日期
        selectFragments.add(String.format("%s.%s", dsTableAlias, BIConsts.MIN_DATE_GRANULARITY_CODE));

        return selectFragments;
    }

    protected List<QueryField> getSelectFields(){
        // select
        List<QueryField> selectFields = model.getFields().stream().filter(f -> f.getIsResult()).collect(Collectors.toList());
        Set<String> selectCodes = selectFields.stream().map(f->f.getCode()).collect(Collectors.toSet());

        // 添加过滤条件中的结果过滤指标
        List<QueryField> resultFilterFields = model.getFields().stream().filter(
                f->f.getIsFilter()
                        && f.isMeasure()
                        && FieldFilterMode.agg == FieldFilterMode.get(f.getFilterValueMode())
                        && BIUtil.isNotEmpty(f.getValues())
                        && !selectCodes.contains(f.getCode())
        ).collect(Collectors.toList());

        selectFields.addAll(resultFilterFields);

        // 按查询配置的字段顺序排序：行维度+列维度+指标
        //selectFields = selectFields.stream().sorted(Comparator.comparing(QueryField::isMeasure).thenComparing(QueryField::getCode)).collect(Collectors.toList());
        selectFields = FieldUtil.sortByShowOrder(config, selectFields);

        return selectFields;
    }

    public StringBuilder buildFromClause() {
        StringBuilder fromSql = new StringBuilder();
        fromSql.append(String.format(" from (%s) %s", datasourceSql, dsTableAlias));
        return fromSql;
    }

    /**
     * 构建group by
     *
     * @return
     */
    protected StringBuilder buildGroupByClause() {
        StringBuilder groupBySQL = new StringBuilder();
        List<String> fieldNameList = this.buildGroupByFragments();
        if (fieldNameList.isEmpty()) {
            return groupBySQL;
        }
        groupBySQL.append(" GROUP BY ");
        String groupByFieldStr = BIUtil.listToStr(fieldNameList, ",");
        groupBySQL.append(groupByFieldStr);
        return groupBySQL;
    }

    protected List<String> buildGroupByFragments(){
        List<String> fragments = new ArrayList<String>();

        List<QueryField> resultFields = this.getSelectFields();

        // 先添加最细粒度的日期：此处必须先添加，便于日均小计总计grouping value在union all时一致
        fragments.add(BIConsts.MIN_DATE_GRANULARITY_CODE);

        // 再添加具体字段
        for (QueryField field : resultFields) {
            if (field.isAppend()) { // 若是计算字段附带的结果字段，则不进行分组计算
                continue;
            }

            // 只取维度
            if (!field.isMeasure() && model.isExistsByMetaCode(field)) {
                String fieldName = field.getCode();// String.format("%s.%s", dsTableAlias, field.getCode());
                if(BIUtil.isNotEmpty(fieldName)) {
                    fragments.add(fieldName);
                }
            }
        }

        // 按config中行列维度的排序，保障后续的grouping的顺序和查询顺序一致
        /*
        List<QueryField> configResultFields = new ArrayList<>();
        configResultFields.addAll(config.getResult().getRowDimensions());
        configResultFields.addAll(config.getResult().getColDimensions());

        List<String> newFragments = new ArrayList<>();
        for(QueryField resultField : configResultFields){
            if(fragments.contains(resultField.getCode())){
                newFragments.add(resultField.getCode());
                fragments.remove(resultField.getCode());
            }
        }
        newFragments.addAll(fragments);

        fragments.clear();
         */

        List<String> newFragments = new ArrayList<>();
        for(String fieldName : fragments){
            newFragments.add(String.format("%s.%s", dsTableAlias, fieldName));
        }

        return newFragments;
    }

}
