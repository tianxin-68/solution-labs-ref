package com.bi.queryer.ssm.engine;

import cn.hutool.core.collection.CollUtil;
import com.bi.queryer.ssm.engine.aggregation.AggregatorContext;
import com.bi.queryer.ssm.engine.aggregation.AggregatorFactory;
import com.bi.queryer.ssm.engine.aggregation.DefaultAggregator;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.FieldValue;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.function.FunctionManager;
import com.bi.queryer.ssm.engine.function.IFunction;
import com.bi.queryer.ssm.engine.model.QueryTable;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.ssm.engine.promotion.PromotionConsts;
import com.bi.queryer.ssm.enums.FieldDataType;
import com.bi.queryer.ssm.enums.FieldFilterMode;
import com.bi.queryer.ssm.enums.FieldFilterType;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.util.FieldUtil;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.env.EnvVariableManager;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 15:50 2022-10-21
 * @Description 单模型sql构建器
 **/
public class SingleModelSqlBuilder {

    protected QueryConfigure config = null;

    protected QueryContext cxt = null;

    protected List<StarModel> models = null;

    protected IFunction fx = null;

    public SingleModelSqlBuilder(QueryConfigure config, QueryContext cxt) {
        this.cxt = cxt;
        this.config = config;
        this.fx = FunctionManager.getFunction();
    }

    public String build(StarModel model) {

        StringBuilder sql = new StringBuilder();

        /**select*/
        StringBuilder selectClause = buildSelectClause(model);
        sql.append(selectClause);

        /**from*/
        StringBuilder fromClause = buildFromClause(model);
        sql.append(fromClause);

        /**where*/
        StringBuilder whereClause = buildWhereClause(model);
        sql.append(whereClause);

        /**group by*/
        StringBuilder groupByClause = buildGroupByClause(model);
        sql.append(groupByClause);

        /**having*/
//        StringBuilder havingClause = buildHavingClause(model);
//        sql.append(havingClause);

        return sql.toString();
    }

    /**
     * 获取查询字段
     * @param model
     * @return
     */
    protected List<QueryField> getSelectFields(StarModel model) {

        //指标结果过滤需要放在最外层处理，此处需要将过滤条件中的指标字段查出来
        List<QueryField> selectFields = model.getFields().stream().filter(f -> f.getIsResult() || SSDUtil.isAggFilter(f)).collect(Collectors.toList());
        selectFields = FieldUtil.sortByShowOrder(config, selectFields);

        // 按查询配置的字段顺序排序：行维度+列维度+指标
        /*
        selectFields = selectFields.stream().sorted(Comparator.comparing(QueryField::isMeasure).thenComparing(QueryField::getCode)).collect(Collectors.toList());
        List<QueryField> rowFields = new ArrayList<>();
        List<QueryField> colFields = new ArrayList<>();
        List<QueryField> measures = new ArrayList<>();

        for (QueryField f : selectFields) {
            if (f.getQueryArea() == QueryArea.RowDimension) {
                rowFields.add(f);
            } else if (f.getQueryArea() == QueryArea.ColumnDimension) {
                colFields.add(f);
            } else {
                measures.add(f);
            }
        }
        Collections.sort(rowFields);
        Collections.sort(colFields);
        Collections.sort(measures);

        selectFields.clear();

        selectFields.addAll(rowFields);
        selectFields.addAll(colFields);
        selectFields.addAll(measures);
         */

        return selectFields;
    }

    /**
     * 构建Select子句
     *
     * @returnb
     */
    protected StringBuilder buildSelectClause(StarModel model) {
        StringBuilder selectSQL = new StringBuilder();
        List<String> selectFragments = this.buildSelectFragments(model);
        String selectFieldStr = BIUtil.listToStr(selectFragments, ",");
        selectSQL.append(" select ").append(selectFieldStr);
        return selectSQL;
    }

    protected List<String> buildSelectFragments(StarModel model){
        List<String> selectFragments = new ArrayList<String>();
        List<QueryField> selectFields = this.getSelectFields(model);

        // 查询用户是否有敏感字段权限
        List<String> aclSensitiveCodes = new ArrayList<>(); // 敏感字段权限列表
        List<String> sensitiveApplyFields = new ArrayList<>(); // 解密码敏感字段申请
        Integer decryptSensitiveField = 0; //个人敏感数据是否要解密
        if (cxt != null) {
            aclSensitiveCodes = cxt.getAclSensitiveFields();
            sensitiveApplyFields = cxt.getSensitiveApplyFields();
            decryptSensitiveField = cxt.getDecryptSensitiveField();
        }

        for (QueryField field : selectFields) {
            if (field.isAppend() && !field.isCalc()) {
                // 附加字段不显示且不是计算字段
                // 是附加字段且是计算字段的场景：用户自定义字段中引用了后台定义的计算字段，如：用户自定义字段=[订单金额]/[用户数]，其中[用户数]=(count(distinct case when user_id is null then 0 else 1 end)
                // continue;
            }

            if (field.isAnalysisCalc()) {
                continue;
            }

            if (field.isTargetValue()) {
                continue;
            }

            if(field.isAppend() && field.isDimension()){
                // 若是附加维度，在select子句中不能出现，附加指标需要，避免group by时有附加维度，导致指标计算错误
                continue;
            }
            if(field.isCustomMeasure()) {
                // 用户自定义指标放到最后外层sql中，此处不处理，
                continue;
            }
            // 敏感字段处理
            String fieldFullName = getSelectFieldFullName(field);

            /**
             * 敏感数据都可查询明文：下载时走审批：20250904 by contributor
            //解密码敏感字段申请如果包含就可以不加密直接跳过
            //新增个人敏感数据是否要解密decryptSensitiveField
            if (sensitiveApplyFields.contains(field.getCode()) || Enabled.value(decryptSensitiveField)) {
                //包含解密字段，或者需要解密，就直接跳过！
            } else {
                if (Enabled.value(field.getMeta().getIsSensitive()) && !aclSensitiveCodes.contains(field.getCode())) {
                    fieldFullName = field.isMeasure() ? "'***'" : fx.mask(fieldFullName, "******");
                }
            }*/

            // 权限不在sql层处理，在最后结果输出时做掩码，避免后续sql构建时数据类型不一致导致sql查询错误
            /*
            if (aclCodes != null && !aclCodes.contains(field.getCode())) {
                fieldFullName = "'***'";
            }
             */

            if(field.isVirtual()) {
                fieldFullName = "null"; // 虚拟字段
            } else {
                cxt.addModelUsedFields(model.getAlias(), field);
            }

            // 兜底逻辑:保证字段所属表别名一致
            fieldFullName = fieldFullName.replaceAll("F\\d\\.", model.getFactTable().getAlias() + ".");
            selectFragments.add(fieldFullName + " AS " + field.getCode());

            // 添加到上下文，便于日志统计
            cxt.addUsedField(field);
        }

        //业务日历添加持续天数、时间进度字段
        if(config.getSettings().isBusinessCalendar()){
            selectFragments.add(String.format("%s as %s", this.buildSelectDurationDaysFragment(model), PromotionConsts.PROMOTION_DURATION_DAYS_CODE));
            selectFragments.add(String.format("%s as %s", this.buildSelectDateRangeProgressFragment(model), PromotionConsts.PROMOTION_DATE_RANGE_PROGRESS_CODE));
        }

        // 添加总计/小计维度标识
        String groupingExpression = String.format("%s as %s", this.buildSelectGroupingFragment(model), BIConsts.GROUPING_VALUE);
        selectFragments.add(groupingExpression);

        //去重
        selectFragments = selectFragments.stream().distinct().collect(Collectors.toList());
        return selectFragments;
    }


    /**
     * 构建持续天数字段
     * @param model
     * @return
     */
    protected String buildSelectDurationDaysFragment(StarModel model) {
        String durationDaysExpression = String.format("max(%s.%s) ",model.getFactTable().getAlias(),
                PromotionConsts.PROMOTION_DURATION_DAYS_CODE);
        return durationDaysExpression;
    }

    /**
     * 构建时间进度字段
     * @param model
     * @return
     */
    protected String buildSelectDateRangeProgressFragment(StarModel model) {

        String dateRangeProgress = "";

        List<String> groupByFragments = this.buildGroupByFragments(model);
        if(CollUtil.isNotEmpty(groupByFragments)){

            //列总计，时间进度 = null
            List<String> expressionFragments = new ArrayList<>();
            for(String groupByFragment : groupByFragments){
                String expression = String.format("%s is null",
                        groupByFragment);
                expressionFragments.add( expression);
            }

            dateRangeProgress = String.format("if(%s,null,max(%s.%s)) ",
                    BIUtil.listToStr(expressionFragments," and "),
                    model.getFactTable().getAlias(),
                    PromotionConsts.PROMOTION_DATE_RANGE_PROGRESS_CODE);
        }else{
            dateRangeProgress = String.format("max(%s.%s) ",
                    model.getFactTable().getAlias(),
                    PromotionConsts.PROMOTION_DATE_RANGE_PROGRESS_CODE);
        }

        return dateRangeProgress;
    }

    /**
     * // 添加总计/小计维度标识
     * @return
     */
    protected String buildSelectGroupingFragment(StarModel model){
        String groupingExpression = String.format("0 as %s", BIConsts.GROUPING_VALUE);

        List<String> groupByFragments = this.buildGroupByFragments(model);
        if (BIUtil.isNotEmpty(groupByFragments)) {
            groupingExpression = fx.groupingId(groupByFragments); //String.format("grouping(%s)", BIUtil.listToStr(groupByFragments, ","));
        } else {
            groupingExpression =  "sum(0)"; // 此处需聚合常量，不使用常量，避免日均值在无行维度时计算结果重复。场景：日店均支付订单数（日均）、日期（汇总）
        }
        return groupingExpression;
    }

    protected StringBuilder buildFromClause(StarModel model) {
        //DefaultDataSourceSqlBuilder datasetSqlBuilder = new DefaultDataSourceSqlBuilder(model, config, cxt) ;
        DefaultDataSourceSqlBuilder datasetSqlBuilder = DefaultDataSourceSqlBuilderFactory.createDefaultDataSourceSqlBuilder(model, config, cxt);
        StringBuilder fromSql = new StringBuilder();
        String datasetSql = datasetSqlBuilder.build();
        fromSql.append(String.format(" from (%s) %s", datasetSql, model.getFactTable().getAlias()));
        return fromSql;
    }

    protected StringBuilder buildWhereClause(StarModel model) {
        StringBuilder whereSQL = new StringBuilder();
        List<String> fragments = buildWhereFragments(model);
        if(BIUtil.isEmpty(fragments)){
            return whereSQL;
        }
        whereSQL.append(String.format(" where %s", BIUtil.listToStr(fragments, " and ", "(", ")")));
        return whereSQL;
    }

    protected List<String> buildWhereFragments(StarModel model){
        return new ArrayList<>();
    }

    protected List<String> buildGroupByFragments(StarModel model){
        List<String> fragments = new ArrayList<String>();
        Map<String, QueryField> resultFieldMap = model.getFields().stream().filter(f -> f.getIsResult()).collect(Collectors.toMap(QueryField::getCode, QueryField->QueryField, (f1,f2)->f1));

        // 按config中行列维度的排序，保障后续的grouping的顺序和查询顺序一致
        List<QueryField> configResultFields = new ArrayList<>();

        configResultFields.addAll(config.getResult().getColDimensions());
        configResultFields.addAll(config.getResult().getRowDimensions());
        
        List<QueryField> resultFields = new ArrayList<>();
        for(QueryField f : configResultFields){
            if(resultFieldMap.containsKey(f.getCode())){
                resultFields.add(resultFieldMap.get(f.getCode()));
            }
        }

        for (QueryField field : resultFields) {
            if (field.isAppend()) { // 若是计算字段附带的结果字段，则不进行分组计算
                continue;
            }

            // 只取维度
            if (!field.isMeasure() && model.isExistsByMetaCode(field)) {
                String fieldName = getSelectFieldFullName(field);
                if(BIUtil.isNotEmpty(fieldName)) {
                    fragments.add(fieldName);
                }
            }
        }

        return fragments;
    }

    /**
     * 构建group by
     *
     * @return
     */
    protected StringBuilder buildGroupByClause(StarModel model) {
        StringBuilder groupBySQL = new StringBuilder();
        List<String> fieldNameList = this.buildGroupByFragments(model);
        if (fieldNameList.isEmpty()) {
            return groupBySQL;
        }
        /*
        groupBySQL.append(" GROUP BY ");
        String groupByFieldStr = BIUtil.listToStr(fieldNameList, ",");
        groupBySQL.append(groupByFieldStr);
         */
        groupBySQL.append(String.format(" group by grouping sets ((%s))", BIUtil.listToStr(fieldNameList)));
        return groupBySQL;
    }

    /**
     * 构建having子句
     *
     * @return
     */
    protected StringBuilder buildHavingClause(StarModel model) {
        List<QueryField> resultFields = new ArrayList<>();
        List<QueryField> filterFields = new ArrayList<>();
        model.getFields().forEach(f -> {
            if (f.getIsResult()) {
                resultFields.add(f);
            }
            if (f.getIsFilter()) {
                filterFields.add(f);
            }
        });

        StringBuilder havingBySQL = new StringBuilder();
        if (resultFields == null || resultFields.size() == 0 || filterFields == null || filterFields.isEmpty()) {
            return havingBySQL;
        }
        List<String> havingFragments = new ArrayList<>();
        for (QueryField field : filterFields) {
            if (!model.isExistsByMetaCode(field)) {
                continue;
            }
            String havingFieldName = getHavingFieldFullName(model.getFactTable(), field);
            if (BIUtil.isEmpty(havingFieldName)) {
                continue;
            }

            if (!field.isActive()) {
                continue;
            }

            //不可度量字段直接跳过
            if (!field.isMeasure()) {
                continue;
            }

            //如果不包含"["，且FilterValueMode为detail,则退出
            if (field.isCalc() && FieldFilterMode.detail == FieldFilterMode.get(field.getFilterValueMode())) {
                continue;
            }

            //由于模板可能存在没有FilterValueMode的情况
            if (FieldFilterMode.detail == FieldFilterMode.get(field.getFilterValueMode())) {
                continue;
            }

            // 虚拟字段不参与having，避免查询时不确定数据类型，因为在子查询中为null，如：sum(f1) between 1 and 2 ，其中 f1为null
            if(field.isVirtual()){
                continue;
            }

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
            //String showType = field.getMeta().getFilterShowType();
            if (values.isEmpty()) {
                continue;
            }

            //havingBySQL.append(" AND ");
            FieldFilterType filterType = FieldUtil.getFilterType(field);
            if(filterType.isRange()){
                String charStr = ""; // SQL字符串
                if (dataType == FieldDataType.Integer || dataType == FieldDataType.Double) {
                    charStr = "";
                    if (StringUtil.isEmpty(values.get(0).getId())) {
                        values.get(0).setId("0");
                    }
                    if (StringUtil.isEmpty(values.get(values.size() - 1).getId())) {
                        values.get(values.size() - 1).setId("100000000");
                    }
                }
                if (BIUtil.isEmpty(havingFieldName)) {
                    continue;
                }

                if (values.size() == 1) {
                    //havingBySQL.append(havingFieldName).append("=").append(charStr + values.get(0).getId() + charStr);
                    havingFragments.add(String.format("%s=%s", havingFieldName, charStr + values.get(0).getId() + charStr));
                } else {
                    // 支持多段区间查询
                    FieldUtil.sortRangeValues(field, values);
//                    havingBySQL.append("  (");
                    int valueCount = (values.size() / 2) * 2; // 只取2的倍数值
                    List<String> betweenFragments = new ArrayList<>();
                    for (int v = 0; v < valueCount; v = v + 2) {
                        FieldValue v1 = values.get(v);
                        FieldValue v2 = values.get(v + 1);
                        betweenFragments.add(String.format(" between %s and %s", charStr + v1.getId() + charStr, charStr + v2.getId() + charStr));
                        /*
                        if (v > 0) {
                            havingBySQL.append(" OR ");
                        }
                        havingBySQL.append(" ").append(havingFieldName).append(" ");
                        havingBySQL.append(" BETWEEN ").append(charStr + v1.getId() + charStr);
                        havingBySQL.append(" AND ").append(charStr + v2.getId() + charStr);
                         */
                    }
//                    havingBySQL.append(" )");

                    havingFragments.add(String.format("(%s %s)", havingFieldName, BIUtil.listToStr(betweenFragments, " or ")));

                }
            }

        }
        /*
        if (havingBySQL.toString().contains("AND")) {
            String temp = havingBySQL.toString();
            temp = temp.replaceFirst(" AND ", " ");
            havingBySQL = new StringBuilder(temp);
            havingBySQL.insert(0, " HAVING ");
        }
         */

        if(BIUtil.isNotEmpty(havingFragments)){
            havingBySQL.append(String.format(" having %s", BIUtil.listToStr(havingFragments, " and ")));
        }

        return havingBySQL;
    }

    protected String getHavingFieldFullName(QueryTable queryTable, QueryField field) {
        String fieldFullName = getSelectFieldFullName(field);
        return fieldFullName;

    }

    protected String getSelectFieldFullName(QueryField field) {
        MetaField meta = field.getMeta();
        String name = "";
        if (!field.isCalc()) {// 计算字段没有所属表
            name = field.getTable().getAlias() + "." + field.getCode();
        }

        // 维度
        if (!Enabled.value(meta.getIsMeasure())) {
            name = field.getTable().getAlias() + "." + field.getCode();
        }

        // 度量
        if (Enabled.value(meta.getIsMeasure())) {
            AggregatorContext aggCxt = new AggregatorContext(config);
            DefaultAggregator aggregator = AggregatorFactory.getAggregator(field.getAggExpressionType(), aggCxt);
            name = aggregator.aggregate(field);
            /*
            String aggExpression = meta.getAggExpression();
            if (StringUtil.isEmpty(aggExpression)) {
                throw new BIException(meta.getTitle() + "[" + meta.getName() + "]未配置聚合函数");
            }
            if(field.isCalc()) {// 计算字段，需对其计算表达式解析处理
                name = parseCalcExpression(field);
            } else {
                if ("count".equalsIgnoreCase(aggExpression)) {
                    name = aggExpression + "(DISTINCT " + name + ")";
                } else {
                    name = aggExpression + "(" + name + ")";
                }
            }
             */
        }
        return name;
    }
    public List<StarModel> getModels() {
        return models;
    }

    public void setModels(List<StarModel> models) {
        this.models = models;
    }

    public static void main(String[] args) {
        List<String> list1 = new ArrayList<>();
        list1.add("a");
        list1.add("b");
        list1.add("c");

        List<String> list2 = new ArrayList<>();
//        list2.add("d");
//        list2.add("a");
//        list2.add("d");

        list1.retainAll(list2);

        System.out.println(list1);

        String expression = "${ord_gmv1} + ${2} - ${sa}";

        String operator = expression.replaceAll("\\$\\{.*?\\}", "");

        boolean isCoalesce = (operator.contains("+") || operator.contains("-"))  && !operator.contains("*") && !operator.contains("/");

        System.out.println(isCoalesce);

        String sql = "select f3.f1 , f4.f2 from f4";
        sql  = sql.replaceAll("f\\d\\.", "f4.");
        System.out.println(sql);
    }
}
