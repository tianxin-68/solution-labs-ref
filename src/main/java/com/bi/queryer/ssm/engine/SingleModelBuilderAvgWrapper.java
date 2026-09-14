package com.bi.queryer.ssm.engine;

import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.FieldValue;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.ssm.engine.promotion.PromotionConsts;
import com.bi.queryer.ssm.enums.*;
import com.bi.queryer.ssm.util.FieldUtil;
import com.bi.queryer.sys.env.EnvVariableManager;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.StringUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 20:48 2023-09-25
 * @Description 日均单模型sql构建器
 **/
public class SingleModelBuilderAvgWrapper extends SingleModelSqlBuilder{
    protected SingleModelSqlBuilder singleModelSqlBuilder = null;

    protected List<QueryField> distinctFields = new ArrayList<>(); // 记录需要日均的去重指标
    protected List<QueryField> commonFields = new ArrayList<>();// 记录正常指标
    protected List<QueryField> havingFilterFields = new ArrayList<>(); // 聚合后过滤指标字段

    protected StarModel model = null;

    protected String commonAlias = "";
    protected String distinctAlias = "";

    public SingleModelBuilderAvgWrapper(QueryConfigure config, QueryContext cxt) {
        super(config, cxt);
    }

    public SingleModelBuilderAvgWrapper(SingleModelSqlBuilder singleModelSqlBuilder){
        super(singleModelSqlBuilder.config, singleModelSqlBuilder.cxt);
        this.singleModelSqlBuilder = singleModelSqlBuilder;
    }

    public String build(StarModel model){
        this.distinctFields.clear();
        this.commonFields.clear();
        this.model = model;

        this.commonAlias = model.getAlias() + "_Com";
        this.distinctAlias = model.getAlias() + "_Dst";


        this.prepare();

        if(BIUtil.isEmpty(distinctFields)){
            return this.singleModelSqlBuilder.build(model);
        }

        // 禁用having构建
        this.disableBuildHavingClause();

        StringBuilder sql = new StringBuilder();

        // sum指标 + 无日均指标
        String normalSql = this.buildCommonAggSql();

        String distinctSql = this.buildDistinctAggSql();

        this.restore();

        sql.append(this.buildSelectClause(model));
        sql.append(this.buildFromClause(normalSql, distinctSql));
        sql.append(this.buildWhereClause(model));

        return sql.toString();
    }


    protected void prepare(){
        List<QueryField> resultMeasureFields = model.getFields().stream().filter(f->f.getIsResult() && f.isMeasure()).collect(Collectors.toList());

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
        for(QueryField f : resultMeasureFields ){
            // 前端设置的聚合方式
            AggExpressionType aggType = AggExpressionType.get(f.getAggExpressionType());

            String aggExpression = f.getMeta().getAggExpression(); // 元信息中配置的聚合表达式
            //if(AggExpressionType.get(aggExpression) == AggExpressionType.Count_Distinct || aggExpression.toLowerCase().contains(BIConsts.COUNT_DISTINCT_FLAG)) {
            if(BIUtil.isCountDistinctAggExpression(aggExpression)){
                if(aggType == AggExpressionType.Avg_By_Day || aggType == AggExpressionType.Avg_By_Day_Real) {
                    if(!f.isVirtual()) {
                        distinctFields.add(f);
                        continue;
                    }
                }
            }

            if(!f.isVirtual()){
                commonFields.add(f);
            }
        }

        List<QueryField> filterMeasureFields = model.getFields().stream().filter(f->f.getIsResult() && f.isMeasure()).collect(Collectors.toList());
        for (QueryField field : filterMeasureFields) {
            if (!model.isExistsByMetaCode(field)) {
                continue;
            }
            if (!field.isActive()) {
                continue;
            }
            //不是度量字段直接跳过
            if (!field.isMeasure()) {
                continue;
            }

            //由于模板可能存在没有FilterValueMode的情况
            if (FieldFilterMode.detail == FieldFilterMode.get(field.getFilterValueMode())) {
                continue;
            }

            if (FieldFilterMode.agg == FieldFilterMode.get(field.getFilterValueMode())) {
                continue;
            }

            //计算指标的过滤在此处不处理
            if(FieldType.CUSTOM_MEASURE == FieldType.get(field.getFieldType())){
                continue;
            }

            //************正式开始装配sql****************
            List<FieldValue> values = field.getValues();
            if (values == null || values.isEmpty()) {
                continue;
            }
            havingFilterFields.add(field);
        }


    }

    /**
     * // 还原字段虚拟属性
     */
    protected void restore(){

        commonFields.forEach(f->this.setFieldVirtual(f,false));
        distinctFields.forEach(f->this.setFieldVirtual(f,false));
        havingFilterFields.forEach(f->f.setFilter(true));
    }

    /**
     * // sum指标 + 无日均指标
     * @return
     */
    protected String buildCommonAggSql(){
        commonFields.forEach(f-> this.setFieldVirtual(f, false));
        distinctFields.forEach(f->this.setFieldVirtual(f,true));

        if(BIUtil.isEmpty(commonFields) && BIUtil.isEmpty(config.getResult().getRowDimensions()) && BIUtil.isEmpty(config.getResult().getColDimensions())) {
            return getEmptySql();
        }

        // 当后台配置的计算指标的原子指标，此原子指标同时在distinct列表中时，需要将此原子指标改Virtual=false，避免sql查询报错
        for(QueryField commonField : commonFields){
            Set<QueryField> calcAtomFields = commonField.getCalcAtomFields();
            if(BIUtil.isEmpty(calcAtomFields)){
                continue;
            }
            calcAtomFields.forEach(atomField -> {
                if(distinctFields.contains(atomField)){
                    atomField.setVirtual(false);
                }
                // 原子字段在distinct中的字段的原子字段列表中出现，也同时改为false
                // 避免场景：2C销售额B0-ARPU，同时设置原值+日均值，会导致在计算日均时将其原子字段设置为虚拟字段
                distinctFields.forEach(df -> {
                    if(df.getCalcAtomFields().contains(atomField)){
                        atomField.setVirtual(false);
                    }
                });
            });
        }

        SingleModelSqlBuilder singleModelSqlBuilder = new SingleModelSqlBuilder(config, cxt);
        String sql = singleModelSqlBuilder.build(model);
        return sql;
    }

    protected String getEmptySql(){
        return String.format("select %s as %s", "0", BIConsts.GROUPING_VALUE);
    }

    /**
     * 按日去重
     * @return
     */
    protected String buildDistinctAggSql(){
        commonFields.forEach(f->this.setFieldVirtual(f,true));
        distinctFields.forEach(f->this.setFieldVirtual(f,false));

        // 当前模型的所有字段
        Map<String, QueryField> modelAllFields = new HashMap<>();
        this.model.getTables().forEach(t -> {
            t.getFields().forEach(f->modelAllFields.put(f.getCode(), f));
        });

        // 当后台配置的计算指标的原子指标，此原子指标同时在common列表中时，需要将此原子指标改Virtual=false，避免sql查询报错
        for(QueryField distinctField : distinctFields){
            Set<QueryField> calcAtomFields = distinctField.getCalcAtomFields();
            if(BIUtil.isEmpty(calcAtomFields)){
                continue;
            }
            calcAtomFields.forEach(atomField -> {
                if(commonFields.contains(atomField)){
                    atomField.setVirtual(false);
                    // 同步更新模型对应字段：因为计算字段的原字段在日均值和非日均时是独立的，不是同一个对象
                    if(modelAllFields.containsKey(atomField.getCode())){
                        modelAllFields.get(atomField.getCode()).setVirtual(false);
                    }
                }

                // 原子字段在distinct中的字段的原子字段列表中出现，也同时改为false
                // 避免场景：2C销售额B0-ARPU，同时设置原值+日均值，会导致在计算日均时将其原子字段设置为虚拟字段
                commonFields.forEach(df -> {
                    if(df.getCalcAtomFields().contains(atomField)){
                        atomField.setVirtual(false);

                        // 同步更新模型对应字段：因为计算字段的原字段在日均值和非日均时是独立的，不是同一个对象
                        if(modelAllFields.containsKey(atomField.getCode())){
                            modelAllFields.get(atomField.getCode()).setVirtual(false);
                        }
                    }
                });
            });
        }

        SingleModelSqlBuilder singleModelSqlBuilder = new SingleModelSqlBuilder(config, cxt);
        String sql = singleModelSqlBuilder.build(model);
        return sql;
    }

    /**
     * 设置字段是否虚拟字段，若是计算字段且是附加字段，则也设置为虚拟字段
     * @param f
     * @param isVirtual
     */
    protected void setFieldVirtual(QueryField f, boolean isVirtual){
        f.setVirtual(isVirtual);
        if(BIUtil.isNotEmpty(f.getCalcAtomFields())){
            f.getCalcAtomFields().stream().filter(c->c.isAppend()).forEach(c->c.setVirtual(isVirtual));
        }
    }

    @Override
    protected StringBuilder buildSelectClause(StarModel model) {
        return super.buildSelectClause(model);
    }


    @Override
    protected String buildSelectDurationDaysFragment(StarModel model) {
        String durationDaysExpression = fx.coalesce(String.format("%s.%s", commonAlias, PromotionConsts.PROMOTION_DURATION_DAYS_CODE), String.format("%s.%s", distinctAlias, PromotionConsts.PROMOTION_DURATION_DAYS_CODE));
        return durationDaysExpression;
    }

    @Override
    protected String buildSelectDateRangeProgressFragment(StarModel model) {
        String selectDateRangeProgress = fx.coalesce(String.format("%s.%s", commonAlias, PromotionConsts.PROMOTION_DATE_RANGE_PROGRESS_CODE), String.format("%s.%s", distinctAlias, PromotionConsts.PROMOTION_DATE_RANGE_PROGRESS_CODE));
        return selectDateRangeProgress;
    }

    @Override
    protected String buildSelectGroupingFragment(StarModel model) {
        String groupingExpression = fx.coalesce(String.format("%s.%s", commonAlias, BIConsts.GROUPING_VALUE), String.format("%s.%s", distinctAlias, BIConsts.GROUPING_VALUE));
        return groupingExpression;
    }

    @Override
    protected String getSelectFieldFullName(QueryField field) {
        String name = "";
        // 维度
        if (field.isDimension()) {
            String commonName = String.format("%s.%s", commonAlias, field.getCode());
            String distinctName = String.format("%s.%s", distinctAlias, field.getCode());
            name = fx.coalesce(commonName, distinctName);
        }

        // 度量
        if(field.isMeasure()) {
            if(commonFields.contains(field)) {
                name = String.format("%s.%s", commonAlias, field.getCode());
            }
            if(distinctFields.contains(field)){
                name = String.format("%s.%s", distinctAlias, field.getCode());
            }
        }

        return name;
    }

    protected String buildFromClause(String commonSql, String distinctSql){
        StringBuilder sql = new StringBuilder();

        List<String> joinExpressions = new ArrayList<>();
        List<QueryField> joinDimFields = this.getSelectFields(model).stream()
                .filter(f->f.isDimension())
                .filter(f->f.getIsResult())
                .filter(f->!f.isAppend())
                .collect(Collectors.toList());
        if(BIUtil.isEmpty(joinDimFields)){
            joinExpressions.add("1=1 ");
        }else {
            for(QueryField dim : joinDimFields){
                String alias = dim.getTable().getAlias();
                String commonName = String.format("%s.%s", commonAlias, dim.getCode());
                commonName = fx.coalesce(commonName, "'" + BIConsts.SSM_ALL + "'");

                String distinctName = String.format("%s.%s", distinctAlias, dim.getCode());
                distinctName = fx.coalesce(distinctName, "'" + BIConsts.SSM_ALL + "'");

                joinExpressions.add(String.format("%s=%s", commonName, distinctName));
            }
            // 最后添加gropingValue
            // 若有汇总分析：需要关联汇总分组值，避免多层总计/小计后null无法关联的问题
            if(config.getAnalysis().getTotal().isActive()){
                String groupingJoinExpression = String.format("%s.%s = %s.%s", commonAlias, BIConsts.GROUPING_VALUE , distinctAlias, BIConsts.GROUPING_VALUE);
                joinExpressions.add(groupingJoinExpression);
            }
        }

        String fromFragment = String.format(" from (%s) %s full join (%s) %s", commonSql, commonAlias, distinctSql, distinctAlias);
        String joinFragment = String.format(" on %s", BIUtil.listToStr(joinExpressions, " and "));
        sql.append(fromFragment).append(joinFragment);
        return sql.toString();
    }

    /**
     * 将子查询中having子句调整到where子句中
     * @param model
     * @return
     */
    protected StringBuilder buildWhereClause(StarModel model) {
        StringBuilder whereSql = new StringBuilder();

        List<String> whereFragments = new ArrayList<>();
        for (QueryField field : this.havingFilterFields) {
            // 添加到上下文，便于日志统计
            cxt.addUsedField(field);
            //************正式开始装配sql****************
            List<FieldValue> values = field.getValues();
            if (values == null || values.isEmpty()) {
                continue;
            }
            values.forEach(fv -> {
                fv.setId(EnvVariableManager.value(fv.getId()));
            });

            // 字段数据类型
            FieldDataType dataType = FieldDataType.getType(field.getMeta().getDataType());
//            String showType = field.getMeta().getFilterShowType();
            if (values.isEmpty()) {
                continue;
            }

            if(values.size() == 1){
                values.add(values.get(0));
            }
            FieldFilterType filterType = FieldUtil.getFilterType(field);
            String whereFieldName = String.format("%s.%s", commonFields.contains(field) ? commonAlias : distinctAlias, field.getCode());
//            if (showType != null && showType.indexOf("-range") != -1) {
            if(filterType.isRange()){// 范围选择使用between
                if (dataType == FieldDataType.Integer || dataType == FieldDataType.Double) {
                    if (StringUtil.isEmpty(values.get(0).getId())) {
                        values.get(0).setId("0");
                    }
                    if (StringUtil.isEmpty(values.get(1).getId())) {
                        values.get(1).setId("100000000");
                    }
                    String v1 = values.get(0).getId();
                    String v2 = values.get(1).getId();
                    whereFragments.add(String.format("%s between %s and %s", whereFieldName, v1, v2));
                }
            }

        }

        if(BIUtil.isEmpty(whereFragments)) {
            return whereSql;
        }

        whereSql.append(" where ").append(BIUtil.listToStr(whereFragments, " and ", "(", ")"));

        return whereSql;
    }

    /**
     * 禁止单模型构建having子句
     */
    protected void disableBuildHavingClause(){
        if(BIUtil.isEmpty(havingFilterFields)){
            return ;
        }
        havingFilterFields.forEach(f->f.setFilter(false));
    }
}
