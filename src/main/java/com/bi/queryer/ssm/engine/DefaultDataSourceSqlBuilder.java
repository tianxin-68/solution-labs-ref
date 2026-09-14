package com.bi.queryer.ssm.engine;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.engine.acl.AclDataAuthExistsFilterField;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.FieldCategoryAggregationItem;
import com.bi.queryer.ssm.engine.config.field.FieldValue;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.function.FunctionManager;
import com.bi.queryer.ssm.engine.function.IFunction;
import com.bi.queryer.ssm.engine.model.QueryTable;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.ssm.engine.view.DataSourceView;
import com.bi.queryer.ssm.engine.view.DataSourceViewBuilderFactory;
import com.bi.queryer.ssm.engine.view.IDataSourceViewBuilder;
import com.bi.queryer.ssm.enums.*;
import com.bi.queryer.ssm.meta.*;
import com.bi.queryer.ssm.util.FieldUtil;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DataType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.StringUtil;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 10:38 2023-07-10
 * @Description 单个模型的数据源sql构建器：处理自定义维度、明细数据过滤，不做聚合
 **/
public class DefaultDataSourceSqlBuilder {
    protected StarModel model = null;

    protected QueryConfigure config = null;

    protected IFunction function = null;

    protected final String dsFactTableAlias = "vf";

    protected final String dsDimTableAliasPrefix = "vd";

    // 记录所有已被join的表id对应的表别名
    protected Map<String, String> joinedTableAlias = new HashMap<>();

    protected QueryContext cxt;

    /**
     * sql select片段
     */
    protected List<String> sqlSelectFragments = new ArrayList<>();

    /**
     * 公共日期字段的select表达式
     */
    protected String commonDateFieldSelectExpression = "";
    /**
     * 公共日期字段的原始select表达式
     */
    protected String commonDateFieldOriginalSelectExpression = "";

    public DefaultDataSourceSqlBuilder(StarModel model, QueryConfigure config, QueryContext cxt) {
        this.model = model;
        this.config = config;
        this.cxt = cxt;
        this.function = FunctionManager.getFunction();
        this.joinedTableAlias.clear();
    }

    public String build(){

        this.prepare();

        //查询条件,分析场景，需要把查询条件传入视图
        StringBuilder whereSqlBuilder  = buildWhereClause();

        // 先构建事实表视图
        IDataSourceViewBuilder builder = DataSourceViewBuilderFactory.create(model, config, cxt);
        if(builder != null) {
            cxt.setWhereSqlBuilder(whereSqlBuilder);
            DataSourceView view = builder.buildView(model, config, cxt);
            model.getFactTable().setView(view);
        }

        StringBuilder dsSql = new StringBuilder();
        dsSql.append(buildSelectClause());
        dsSql.append(buildFromClause());

        //使用视图时，外层不需要构建where条件
        if(builder == null){
            dsSql.append(whereSqlBuilder);
        }else{
            //使用视图时，
            dsSql.append(buildViewExtendWhereClause());
        }

        dsSql.append(buildGroupByClause());

        String sql = dsSql.toString();
        // 添加包装类
        DefaultDataSourceSqlBuilderAvgWrapper wrapper = this.createDefaultDataSourceSqlBuilderAvgWrapper(sql, model, config, cxt); //new DefaultDataSourceSqlBuilderAvgWrapper(sql, model, config, cxt);
        if(wrapper != null) {
            sql = wrapper.wrap();
        }

        this.post();

        return sql;
    }

    protected DefaultDataSourceSqlBuilderAvgWrapper createDefaultDataSourceSqlBuilderAvgWrapper(String datasourceSql, StarModel model, QueryConfigure config, QueryContext cxt){
        return new DefaultDataSourceSqlBuilderAvgWrapper(datasourceSql, model, config, cxt);
    }

    /**
     * 预处理
     */
    protected void prepare(){
        QueryTable factTable = model.getFactTable();
        if(factTable == null){
            return;
        }
        // 添加事实表别名
        this.joinedTableAlias.put(factTable.getId(), dsFactTableAlias);

        List<QueryTable> dimTables = model.getDimTables();
        int index = 1;
        for (QueryTable dimTable : dimTables) {
            String dimTableAlias = dsDimTableAliasPrefix + (index++);
            joinedTableAlias.put(dimTable.getId(), dimTableAlias);
        }
    }

    /**
     * 后置处理
     */
    protected void post(){
        model.getFactTable().setView(null);
    }


    public StringBuilder buildSelectClause() {
        StringBuilder selectSql = new StringBuilder();

        Set<QueryField> modelUsedFields = cxt.getModelUsedFields(model.getAlias());
        Set<String> calcAtomFieldCodes = modelUsedFields.stream().map(v-> CollUtil.isEmpty(v.getCalcAtomFields())? Collections.singleton(v) : v.getCalcAtomFields())
                .filter(Objects::nonNull).flatMap(Collection::stream).map(QueryField::getCode).collect(Collectors.toSet());
        // select
        List<QueryField> selectFields = this.getSelectFields();

        // 按查询配置的字段顺序排序：行维度+列维度+指标
        selectFields = selectFields.stream().sorted(Comparator.comparing(QueryField::isMeasure).thenComparing(QueryField::getCode)).collect(Collectors.toList());

        List<String> selectFragments = new ArrayList<>();
        for (QueryField field : selectFields) {

            // 指标的计算字段不此处添加，在外层处理
            if (field.isCustomMeasure() || (field.isMeasure() && field.isCalc())) {
                // 用户自定义指标放到最后外层sql中，此处不处理，
                continue;
            }

            MetaField meta = field.getMeta();
//            String fieldFullName = dsFactTableAlias + "." + meta.getName();
//            String fieldFullName = String.format("%s.%s", joinedTableAlias.get(cityField.getTable().getId()), meta.getName()) ;
            String fieldFullName = String.format("%s.%s", this.getTableAlias(field), meta.getName()) ;

            // 维度
            if (!Enabled.value(meta.getIsMeasure())) {

                // 计算字段，需对其计算表达式解析处理
                String calcExpr = getCalcFieldExpr(field);
                if (calcExpr != null) {
                    fieldFullName = calcExpr;
                }

                // 日期字段
                if (FieldUtil.isDateField(field)) {
                    // 处理日期字段或被扩展的字段：如："年月"由"日期"字段扩展
                    String formatStr = meta.getShowFormatExpression();

                    //是否是公共日期字段，公共日期通过前端日期控件的粒度格式化
                    if (field.isCommonDate()) {
                        formatStr = ShowFormatExpressionType.getFormatExpressionByDateGranularity(field.getQueryDateGranularity());
                    }

                    if (!StringUtil.isEmpty(formatStr)) {
                        fieldFullName = function.date2Char(fieldFullName, formatStr);
                    }

                    //公共日期字段记录select表达式,排除_com_d
                    if(field.isCommonDate() && !field.getCode().equalsIgnoreCase(BIConsts.MIN_DATE_GRANULARITY_CODE)){
                        commonDateFieldOriginalSelectExpression = String.format("%s.%s", this.getTableAlias(field), meta.getName()) ;
                        fieldFullName = buildRealTimeChartDateFieldExpression(fieldFullName);
                        commonDateFieldSelectExpression = fieldFullName;
                    }
                }
                /*
                FieldFilterType fieldFilterType = FieldUtil.getFilterType(cityField); //FieldFilterType.get(cityField.getMeta().getFilterShowType());

                if (FieldFilterType.BooleanSelect == fieldFilterType) {
                    if (DataType.getType(cityField.getMeta().getDataType()) == DataType.String) {
                        fieldFullName = "CASE WHEN " + fieldFullName + " = '0' THEN '否' WHEN " + fieldFullName + " = '1' THEN '是' ELSE '其他' END";
                    } else {
                        fieldFullName = "CASE WHEN " + fieldFullName + " = 0 THEN '否' WHEN " + fieldFullName + " = 1 THEN '是' ELSE '其他' END";
                    }
                }
                 */

                if (field.isAppend()) {
                    // 注意：此处不做null处理，因为数据源sqlbuilder不负责聚合 2023-08-02 by contributor
                    // ------------//
                    // 是附加字段且是维度字段，不处理，避免出现在select子句，但再group by 子句中没有导致sql执行报错
                    // 场景：用户数=count(distinct user_id)，此处user_id是附加字段时，虚拟字段为null
                    //fieldFullName = "null";
                }

                /** 功能废弃
                if (cityField.isCategoryAggregation()) {
                    // 分类聚合
                    // 场景：日期时间段分类统计，如：上周=2022-11-01~2022-11-07 本周=2022-11-08~2022-11-14
                    fieldFullName = this.buildCategoryAggregationExpression(cityField, fieldFullName);
                }*/

                //boolean筛选和公共日期不处理
                if(!field.isCommonDate()) {
                    //不在行维度中，不处理。避免处理后，count(distinct XXXXX) 将"(null)" 计数
                    if(config.getResult().getRowDimensions().contains(field)) {
                        //处理维度NUlL值，避免后续分析功能的总计/小计出现总计null后关联重复的问题
                        // 将null/'' -> (null)
                        fieldFullName = function.castToString(fieldFullName);
                        /**
                        String castToString = function.formatString("%s", fieldFullName);
                        fieldFullName = function.ifExpression(fieldFullName + " is null", "'" + BIConsts.NULL_VALUE + "'", castToString);
                         */
                    }
                }
            }

            if (field.isVirtual()) {
                fieldFullName = "null"; // 虚拟字段
            }
            selectFragments.add(String.format("%s as %s", fieldFullName, field.getCode()));
        }

        if (BIUtil.isEmpty(selectFragments)) {
            return selectSql;
        }
        // 去重
        selectFragments = selectFragments.stream().distinct().collect(Collectors.toList());

        this.sqlSelectFragments = selectFragments;

        String selectFieldStr = BIUtil.listToStr(selectFragments, ",");
        selectSql.append(String.format(" select %s ", selectFieldStr));
        return selectSql;
    }

    protected List<QueryField> getSelectFields(){
        // select
        List<QueryField> selectFields = model.getFields().stream().filter(f -> f.getIsResult() && !f.isTargetValue()).collect(Collectors.toList());
        Set<String> selectCodes = selectFields.stream().map(f->f.getCode()).collect(Collectors.toSet());

        // 添加过滤条件中的结果过滤指标
        List<QueryField> resultFilterFields = model.getFields().stream().filter(
                f->f.getIsFilter()
                && FieldFilterMode.agg == FieldFilterMode.get(f.getFilterValueMode())
                && !selectCodes.contains(f.getCode())
        ).collect(Collectors.toList());

        selectFields.addAll(resultFilterFields);

        // 添加公共日期字段
        QueryField commonDateField = null;
        for(QueryField f : selectFields){
            if(f.isCommonDate()){
                commonDateField = f;
                break;
            }
        }

        // 添加用于日均值计算的日期字段
        commonDateField = commonDateField == null ? config.getFilterCommonDateField() : commonDateField;

        if(commonDateField == null){
            throw new BIException("公共日期字段为空，查询失败，请联系数据产品技术支持");
        }

        QueryField copy = commonDateField.clone();
        copy.setCode(BIConsts.MIN_DATE_GRANULARITY_CODE);
        copy.setQueryDateGranularity(DateGranularity.DAY.getCode());
        selectFields.add(copy);

        // 按查询配置的字段顺序排序：行维度+列维度+指标
        selectFields = selectFields.stream().sorted(Comparator.comparing(QueryField::isMeasure).thenComparing(QueryField::getCode)).collect(Collectors.toList());

        return selectFields;
    }

    /**
     * 构建实时趋势图的时间字段表达式
     * @return
     */
    public String buildRealTimeChartDateFieldExpression(String fieldFullName){

        if(!config.isRtDatasetChartQuery()){
            return fieldFullName;
        }

        //获取实时表的切片字段
        TableDataSliceCfg tableDataSliceCfg = model.getFactTable().getMeta().getDataSliceCfgObj();
        if(tableDataSliceCfg == null){
            return fieldFullName;
        }

        String expression = String.format(" substring(%s.%s, 1, 5)", dsFactTableAlias, tableDataSliceCfg.getSliceField());
        return expression;
    }

    public StringBuilder buildFromClause() {
        StringBuilder fromSQL = new StringBuilder();
        QueryTable factTable = model.getFactTable();
        DataSourceView view = factTable.getView();
        if (view != null) {
            // 优先走视图sql，因为视图sql中已经做了维度表的关联
            fromSQL.append(String.format(" from (%s) %s", view.getSql(), dsFactTableAlias));
            return fromSQL;
        }

        String factTableExpression = factTable.getMeta().getFullName();
        fromSQL.append(String.format(" from %s %s", factTableExpression, dsFactTableAlias));

        Set<String> joinedTables = new HashSet<>();
        List<QueryTable> dimTables = model.getDimTables();
        for (QueryTable dimTable : dimTables) {
            if (!dimTable.isActive()) {
                continue;
            }
            if (joinedTables.contains(dimTable.getId())) {
                continue;
            }

            String dimTableAlias = joinedTableAlias.get(dimTable.getId());

            List<MetaTableRelation> relations = SSDMetaCacheManager.getRelations(model.getModelId(), dimTable.getId());
            if (factTable.getVirtual()) {
                fromSQL.append(String.format(" inner join %s %s on 1=1", dimTable.getMeta().getFullName(), dimTableAlias));
            } else if (CollUtil.isNotEmpty(relations)) {
                String joinExpression = relations.get(0).getJoinExpression();
                fromSQL.append(String.format(" %s %s %s on ", joinExpression, dimTable.getMeta().getFullName(), dimTableAlias));
                int index = 0;
                for (MetaTableRelation relation : relations) {
                    String joinField = dsFactTableAlias + "." + relation.getPrimaryFieldName();

                    Optional<MetaField> optionalQueryField = model.getFactTable().getMeta().getFields().stream()
                            .filter(f -> f.getId().equalsIgnoreCase(relation.getPrimaryFieldId()))
                            .findAny();

                    if (optionalQueryField.isPresent()) {
                        MetaField metaField = optionalQueryField.get();
                        QueryField queryField = new QueryField(metaField);
                        // 关联键支持计算字段
                        if (queryField.isCalc()) {

                            for (MetaField mf : metaField.getCalcAtomFields()) {
                                QueryField qf = new QueryField(mf);
                                queryField.getCalcAtomFields().add(qf);
                            }

                            joinField = getCalcFieldExpr(queryField);
                        }
                    }

                    String joinField1 = dimTableAlias + "." + relation.getSubFieldName();
                    if (index > 0) {
                        fromSQL.append(" and ");
                    }
                    String joinField2 = dimTableAlias + "." + relation.getSubFieldName2();
                    fromSQL.append(buildJoinCondition(relation, joinField, joinField1, joinField2));
                    index++;
                }
            }
            // 添加标示
            joinedTables.add(dimTable.getId());
        }
        return fromSQL;
    }

    // 构建join on的条件
    private String buildJoinCondition(MetaTableRelation relation, String expression, String expression1, String expression2) {
        JoinOnType joinOnType = JoinOnType.codeOf(relation.getJoinOnType());
        if (JoinOnType.BETWEEN.equals(joinOnType)) {
            return expression + " between " + expression1 + " and " + expression2;
        } else {
            return expression + " = " + expression1;
        }
    }

    /**
     * 构建分类聚合表达式
     *
     * @param field
     * @return
     */
    protected String buildCategoryAggregationExpression(QueryField field, String fieldFullName) {
        if (!field.isCategoryAggregation()) {
            return fieldFullName;
        }
        StringBuilder expression = new StringBuilder();
        // 日期类型
        MetaField meta = field.getMeta();
        // 对日期格式的条件值排升序
        FieldFilterType fieldFilterType = FieldUtil.getFilterType(field); //FieldFilterType.get(meta.getFilterShowType());
        if (fieldFilterType.isDateRange()) {
            expression = expression.append(" case ");
            List<FieldCategoryAggregationItem> categories = field.getCategoryAggregation().getCategories();
            for (FieldCategoryAggregationItem ctg : categories) {
                List<FieldValue> values = ctg.getValues();
                if (BIUtil.isEmpty(values)) {
                    continue;
                }
                FieldValue v1 = values.get(0);
                FieldValue v2 = (values.size() == 1) ? v1 : values.get(1);

                if (v1.getId().contains(BIConsts.Filter_Value_Is_Null)) {
                    String ctgName = BIUtil.isEmpty(ctg.getName()) ? v1.getId() : ctg.getName();
                    expression.append(" when ").append("COALESCE( ").append(fieldFullName).append(", '' ) = '' ")
                            .append(" then '").append(ctgName).append("' ");
                } else if (v1.getId().contains(BIConsts.Filter_Value_Is_Not_Null)) {
                    String ctgName = BIUtil.isEmpty(ctg.getName()) ? v1.getId() : ctg.getName();
                    expression.append(" when ").append("COALESCE( ").append(fieldFullName).append(", '' ) != '' ")
                            .append(" then '").append(ctgName).append("' ");
                } else {
                    String ctgName = BIUtil.isEmpty(ctg.getName()) ? (v1.getId() + "~" + v2.getId()) : ctg.getName();
                    expression.append(" when ").append(fieldFullName).append(" between '").append(v1.getId()).append("' and ").append("'").append(v2.getId()).append("' ")
                            .append(" then '").append(ctgName).append("' ");
                }
            }
            expression.append(" else '").append("未分类").append("' end ");
        }
        return expression.toString();
    }

    protected StringBuilder buildWhereClause() {
        List<QueryField> filterFields = model.getFields().stream().filter(f -> {
            return f.getIsFilter() && !f.isVirtual();
        }).collect(Collectors.toList());
        StringBuilder whereSQL = new StringBuilder();
        List<String> whereFragments = new ArrayList<>();

        // 常规过滤字段
        List<QueryField> commonFilterFields = new ArrayList<>();

        whereFragments.addAll(buildWhereFragments(filterFields));
        whereFragments.addAll(buildExtendWhereFragments(filterFields));

        // 追加行级权限 exists 过滤 SQL 片段
        whereFragments.addAll(buildRowAclExistsWhereFragments());

        if(BIUtil.isEmpty(whereFragments)) {
            return whereSQL;
        }
        whereSQL.append(" WHERE ");
        whereSQL.append(BIUtil.listToStr(whereFragments, " AND "));
        return whereSQL;
    }

    /**
     * 构建视图扩展的where子句
     * @return
     */
    public StringBuilder buildViewExtendWhereClause() {
        StringBuilder whereSQL = new StringBuilder();
        return whereSQL;
    }

    /**
     * 构建扩展的where子句
     * @return
     */
    public List<String> buildExtendWhereFragments(List<QueryField> filterFields) {
        //在实时的场景，同环比需要过滤分钟
        List<String> whereFragments = buildRtMinuteFilter();
        return whereFragments;
    }

    /**
     * 在实时数据集下，过滤分钟数据
     */
    public List<String> buildRtMinuteFilter () {

        List<String> whereFragments = new ArrayList<>();
        //1 判断是不是实时数据集
        if (!config.getSettings().isRtDataset()) {
            return whereFragments;
        }

        //2 趋势图同环比要查全天的趋势
        if (QueryConfigureType.Chart == config.getType()) {
            return whereFragments;
        }

        //3. 判断公共日期过滤区间是否包含今天（含多段区间）
        QueryField commonDateField = config.getFilterCommonDateField();
        if (commonDateField == null || CollUtil.isEmpty(commonDateField.getValues())) {
            return whereFragments;
        }

        //2026-05-06实时数据集调整为可选择时间区间。
        //如果时间范围包含今天，则需要限制 时分秒
        if (!isCommonDateFilterRangeContainsToday(commonDateField)) {
            return whereFragments;
        }

        String dataUpdateTime = model.getDataUpdateTime();
        if (StrUtil.isEmpty(dataUpdateTime)) {
            return whereFragments;
        }

        dataUpdateTime = DateUtil.format(DateUtil.parse(dataUpdateTime), "HH:mm:ss");
        QueryTable queryTable = model.getFactTable();
        String rtMinuteExpression = String.format("SUBSTRING(%s.%s,12,8) <= '%s' "
                , dsFactTableAlias
                , queryTable.getMeta().getDataUpdateTimeField()
                , dataUpdateTime);
        whereFragments.add(rtMinuteExpression);

        return whereFragments;
    }

    /** 构建where子句
     * @param filterFields
     * @return
     */
    public List<String> buildWhereFragments(List<QueryField> filterFields) {
        List<String> whereSqlFragments = new ArrayList<>();
        if (filterFields == null || filterFields.isEmpty()) {
            return whereSqlFragments;
        }
        for (QueryField field : filterFields) {
            if (!field.isActive()) {
                continue;
            }

            //20240812 增加对计算维度过滤的支持
            //判断如果字段是一个包含"["的计算字段，跳过
            //if (cityField.isCalc()) {
            //    continue;
            //}

            //判断是度量字段
            if (field.isMeasure()) {
                //且FilterValueMode不为detail，则where自动跳过，过滤模式=聚合后过滤，则在having子句中
                //直接使用模板可能导致没有传FilterValueMode所以还需要判断该字段本身是什么
                FieldFilterMode filterMode = FieldFilterMode.get(field.getFilterValueMode());
                if (FieldFilterMode.agg == filterMode) {
                    continue;
                }
            }

            // 添加到上下文，便于日志统计
            cxt.addUsedField(field);

            List<FieldValue> values = FieldUtil.getFilterRealValues(field);
            if (values == null || values.isEmpty()) {
                continue;
            }

            // 过滤类型
            FieldFilterType filterType = FieldUtil.getFilterType(field);

            // 字段数据类型
            FieldDataType dataType = FieldDataType.getType(field.getMeta().getDataType());

            //计算维度使用数字区间，将数据类型转为数字
            if (field.isDimension() && field.isCalc()&&FieldFilterType.DoubleRange == filterType) {
                dataType = FieldDataType.Double;
            }

            // String showType = cityField.getMeta().getFilterShowType();
            //  不包含操作符
            String notContainsOperation = FieldValueFilterType.isExclude(field.getFilterValueType()) ? " NOT " : "";

            StringBuilder whereSQL = new StringBuilder();
//            if (showType != null && showType.indexOf("-range") != -1) { // 范围选择使用between

            // 范围过滤
            if(filterType.isRange()){
                String charStr = ""; // SQL字符串
                if (values.size() == 1) {
                    whereSQL.append(getWhereFieldFullName(field)).append("=").append(charStr + values.get(0).getId() + charStr);
                } else {
                    if (dataType == FieldDataType.Date || dataType == FieldDataType.Datetime || filterType.isDateRange()) { // 日期需要调用日期格式化函数
                        whereSQL.append(this.buildDateRangeWhereSql(field, values));
                    } else if (dataType == FieldDataType.Integer || dataType == FieldDataType.Double){
                        whereSQL.append(this.buildDecimalRangeWhereSql(field, values));
                    }else {
                        whereSQL.append(this.buildStringRangeWhereSql(field, values));
                    }
                }
            }

            // 非范围过滤
            if(!filterType.isRange()){
                Boolean isLikeQuery = false;
                // 若是textarea 默认单记录模糊匹配，多条模糊查询，如果设置了查询规则，则按查询规则来
                //if ("textarea".equalsIgnoreCase(cityField.getMeta().getFilterShowType())) {
                if(filterType == FieldFilterType.Textarea){
                    FilterQueryRuleType ruleType = FilterQueryRuleType.getFilterQueryRule(field.getFilterQueryRule());
                    switch (ruleType) {
                        case Default:
                            if (values.size() == 1 && dataType == FieldDataType.String) {
                                isLikeQuery = true;
                            }
                            break;
                        case Like:
                            // 只有是文本类型时，走模糊匹配，如果是数字类型，则还是走精准匹配
                            if (dataType == FieldDataType.String) {
                                isLikeQuery = true;
                            }
                            break;
                        case Exact:
                            break;
                    }
                }
                if (isLikeQuery) {
                    buildLikeWhereSql(whereSQL, field, values, notContainsOperation);
                } else {
                    buildExactWhereSql(whereSQL, field, values, notContainsOperation);
                }
            }
            if(BIUtil.isNotEmpty(whereSQL.toString())) {
                String finalWhereSQL = whereSQL.toString();
                finalWhereSQL = handleCustomFilter(finalWhereSQL,field);
                whereSqlFragments.add(finalWhereSQL);
            }
        }
        return whereSqlFragments;
    }

    /**
     * 构建行级权限 exists 过滤的 where SQL 片段
     */
    protected List<String> buildRowAclExistsWhereFragments() {
        List<String> fragments = new ArrayList<>();
        //        if(!"true".equalsIgnoreCase(SC.v("ssm.data.auth.filter.by.exists.enable", "true"))){
//            return fragments;
//        }
//        String userName = cxt.getUser().getName();
//        List<String> grayUsers = Arrays.asList(SC.v("ssm.data.auth.filter.by.exists.gray.users").split(","));
//        if(!grayUsers.contains(userName)){
//            return fragments;
//        }
        List<AclDataAuthExistsFilterField> existsFilterFields = cxt.getDataAuthExistsFilterFields();
        for (AclDataAuthExistsFilterField tableExistsFilterField : existsFilterFields) {
            // 部分权限，追加 exists 过滤
            String existsSql = buildRowAclExistsSql(tableExistsFilterField);
            if (BIUtil.isNotEmpty(existsSql)) {
                fragments.add(existsSql);
            }
        }
        return fragments;
    }

    /**
     * 构建单个行级权限 exists SQL
     */
    protected String buildRowAclExistsSql(AclDataAuthExistsFilterField tableExistsFilterField) {
        Optional<MetaField> tableExistsFilterMetaFieldOptional = model.getFactTable().getMeta().getFields()
                .stream().filter(f -> tableExistsFilterField.getFieldCode().equalsIgnoreCase(f.getCode())).findAny();
        if(!tableExistsFilterMetaFieldOptional.isPresent()){
            return "";
        }
        MetaField tableExistsFilterMetaField = tableExistsFilterMetaFieldOptional.get();
        String fieldFullName = dsFactTableAlias + "." + tableExistsFilterMetaField.getName();
        String userName = StringUtil.optErrorSqlStr(cxt.getUser().getName());
        String aclExistsFilterDimCode = StringUtil.optErrorSqlStr(tableExistsFilterField.getDimCode());
        String existsFilterSql =  String.format(
                    "exists (select 1 from bi_olap.ssm_data_auth_exists_filter acl where acl.user_name = '%s' and acl.dim_code = '%s' and acl.item_code = %s and acl.dt = (select max(dt) as max_dt from bi_olap.ssm_data_auth_exists_filter ))",
                    userName,
                    aclExistsFilterDimCode,
                    fieldFullName
            );

        // 特殊逻辑：若是门店ID(AIK)，同时需要支持 or 保养活动分销门店ID(DMD)
        if("AIK".equalsIgnoreCase(aclExistsFilterDimCode)){
            //判断模型的事实表中是否有DMD字段
            Optional<MetaField> optional = model.getFactTable().getMeta().getFields()
                    .stream().filter(f -> "DMD".equalsIgnoreCase(f.getCode())).findAny();
            if(!optional.isPresent()){
                return existsFilterSql;
            }
            //暂不支持计算维度
            MetaField dmdField = optional.get();
            if(StrUtil.isNotEmpty(dmdField.getAggExpression()) && dmdField.getAggExpression().contains("[")) {
                return existsFilterSql;
            }
            //暂不支持计算维度
            String dmdFieldFullName = dsFactTableAlias + "." + dmdField.getName();
            String dmdFilterSql =  String.format(
                    "exists (select 1 from bi_olap.ssm_data_auth_exists_filter acl where acl.user_name = '%s' and acl.dim_code = '%s' and acl.item_code = %s and acl.dt = (select max(dt) as max_dt from bi_olap.ssm_data_auth_exists_filter ))",
                    userName,
                    aclExistsFilterDimCode,
                    dmdFieldFullName
            );
            existsFilterSql = String.format("( (%s) or (%s) )",  existsFilterSql, dmdFilterSql);
        }
        return existsFilterSql;
    }

    /**
     * 处理一些定制化逻辑
     */
    public String handleCustomFilter(String sql,QueryField field){
        String result =  filterCityAuth(sql,field);
        return result;
    }

    /**
     * 如果用户有城市行级权限管控
     * 过滤权限时需要兼容保养分销门店城市
     * AAA（城市） or DMG (保养分销门店城市)
     * https://ssd-admin.example.com/ssm/#/template/e84f1e9a63304ba38a3f8c2f063c5c8d?viewId=1c97cb1492f9406a848759f6af50f3b9
     * @param sql
     * @param field
     * @return
     */
    public String filterCityAuth (String sql,QueryField field) {
        if(BIUtil.isEmpty(sql)) {
            return sql;
        }
        boolean isEnableCityAuthSupportDMG = "true".equalsIgnoreCase(SC.v("ssm.city.auth.support.dmg.enable", "true"));
        if(!isEnableCityAuthSupportDMG){
            return sql;
        }

        // 用户没有城市行级权限管控，不处理
        if (!Enabled.value(cxt.getHasCityRowAuth())) {
            return sql;
        }

        // 不是城市字段过滤，不处理
        if (!BIConsts.CITY_ROW_AUTH_CODE.equalsIgnoreCase(field.getCode())) {
            return sql;
        }

        // 过滤值为空不处理
        if(CollUtil.isEmpty(field.getValues())){
            return sql;
        }
        /**
        //判断模型的事实表中是否有DMG字段
        Optional<MetaField> optional = model.getFactTable().getMeta().getFields()
                .stream().filter(f -> "DMG".equalsIgnoreCase(f.getCode())).findAny();

        if(!optional.isPresent()){
            return sql;
        }

        //暂不支持计算维度
        MetaField dmgField = optional.get();
        if(StrUtil.isNotEmpty(dmgField.getAggExpression()) &&dmgField.getAggExpression().contains("[")) {
            return sql;
        }

        List<String> cityNameList = field.getValues().stream().map(FieldValue::getId).collect(Collectors.toList());

        String dmgFilter = String.format("%s.%s in (%s) "
                ,dsFactTableAlias,
                dmgField.getName(),
                BIUtil.listToStr(cityNameList, ",","'")
                );

        String finalSql = String.format(" ( %s or (%s) ) ",
                sql, dmgFilter);
         **/
        String finalSql = "";
        List<String> filterSqlList = new ArrayList<>();
        filterSqlList.add(sql);

        String dmgFilterSql = this.buildFilterAppendCitySql(field, "DMG");
        if(BIUtil.isNotEmpty(dmgFilterSql)){
            filterSqlList.add(dmgFilterSql);
        }

//        String userName = cxt.getUser().getName();
//        List<String> grayUsers = Arrays.asList(SC.v("ssm.data.auth.filter.by.exists.gray.users").split(","));
//        if(grayUsers.contains(userName)){
        String cvtFilterSql = this.buildFilterAppendCitySql(field, "CVT");
        if(BIUtil.isNotEmpty(cvtFilterSql)){
            filterSqlList.add(cvtFilterSql);
        }
//        }
        finalSql = "(" + BIUtil.listToStr(filterSqlList, " or ", "(", ")") + ")";
        return finalSql;
    }

    /**
     * 构建附加的城市过滤：如：DMG、CVT
     * @param cityField
     * @return
     */
    protected String buildFilterAppendCitySql(QueryField cityField, String appendFieldCode){
        String sql = "";
        //判断模型的事实表中是否有附件字段
//        Optional<MetaField> optional = model.getFactTable().getMeta().getFields()
//                .stream().filter(f -> appendFieldCode.equalsIgnoreCase(f.getCode())).findAny();
//        if(!optional.isPresent()){
//            return sql;
//        }
        MetaField appendFilterField = null;

        // 优先走实时表
        List<QueryTable> queryTables = new ArrayList<>();
        queryTables.add(model.getFactTable());
        queryTables.addAll(model.getDimTables());
        QueryTable appendFilterTable = null;
        for(QueryTable queryTable : queryTables){
            Optional<MetaField> metaFieldOptional = queryTable.getMeta().getFields()
                    .stream().filter(f -> appendFieldCode.equalsIgnoreCase(f.getCode())).findAny();
            if(metaFieldOptional.isPresent()){
                appendFilterField = metaFieldOptional.get();
                appendFilterTable = queryTable;
                break;
            }
        }
        if(appendFilterField == null){
            return sql;
        }

        //暂不支持计算维度
        // MetaField appendFilterField = optional.get();
        if(StrUtil.isNotEmpty(appendFilterField.getAggExpression()) &&appendFilterField.getAggExpression().contains("[")) {
            return sql;
        }

        List<String> cityNameList = cityField.getValues().stream().map(FieldValue::getId).collect(Collectors.toList());
        String tableAlias = this.joinedTableAlias.get(appendFilterTable.getId());
        sql = String.format("%s.%s in (%s) "
                ,tableAlias,
                appendFilterField.getName(),
                BIUtil.listToStr(cityNameList, ",","'")
        );

        return sql;
    }

    // 构建模糊查询SQL
    private void buildLikeWhereSql(StringBuilder whereSQL, QueryField field, List<FieldValue> values, String notContainsOperation) {
        String fieldFullName = getWhereFieldFullName(field);
        whereSQL.append("(");

        List<String> nullValues = ImmutableList.of(SSDUtil.Null_String, BIConsts.NULL_VALUE, "");
        boolean hasNullValue = values.stream().anyMatch(v -> nullValues.contains(StringUtils.lowerCase(v.getId()))); //values.stream().filter(v -> SSDUtil.Null_String.equalsIgnoreCase(v.getId())).count() > 0;
        StringBuilder likeSql = new StringBuilder();
        values.forEach(item -> {
            //if (!SSDUtil.Null_String.equalsIgnoreCase(item.getId())) {
            if(!nullValues.contains(StringUtils.lowerCase(item.getId()))) {
                if (StrUtil.isNotEmpty(likeSql)) {
                    likeSql.append("".equalsIgnoreCase(notContainsOperation) ? " OR " : " AND ");
                }
                likeSql.append(fieldFullName);
                likeSql.append(notContainsOperation);
                likeSql.append(" LIKE '%");
                likeSql.append(item.getId());
                likeSql.append("%'");
            }
        });
        if (hasNullValue) {
            if (StrUtil.isNotEmpty(likeSql)) {
                likeSql.append("".equalsIgnoreCase(notContainsOperation) ? " OR " : " AND ");
            }
            likeSql.append("(");
            if ("".equals(notContainsOperation)) { // 包含
                likeSql.append(fieldFullName).append(" IS NULL ");
                likeSql.append(" OR ");
                likeSql.append(fieldFullName).append(" = ").append("''");
            } else {
                likeSql.append(fieldFullName).append(" IS NOT NULL ");
                likeSql.append(" AND ");
                likeSql.append(fieldFullName).append(" <> ").append("''");
            }
            likeSql.append(")");
        }else {
            // 若是不包含，则需要将null添加到不包含过滤中
            if(!"".equals(notContainsOperation)) {
                likeSql.append(String.format(" OR %s is null ", fieldFullName));
            }
        }
        whereSQL.append(likeSql);
        whereSQL.append(")");
    }

    // 构建精准查询的where语句
    private void buildExactWhereSql(StringBuilder whereSQL, QueryField field, List<FieldValue> values, String notContainsOperation) {
        int inMaxCount = 1000;
        if (values.size() <= inMaxCount) {
            buildWhereNotRangeValues(whereSQL, notContainsOperation, field, values);
        } else {

            String operator = " OR ";
            if (!"".equalsIgnoreCase(notContainsOperation)) {
                operator = " AND ";
            }

            // 超过最大值时，分段in + or
            whereSQL.append("(");
            List<List<FieldValue>> valuesList = BIUtil.splitList(values, inMaxCount);
            for (int j = 0; j < valuesList.size(); j++) {
                if (j != 0) {
                    whereSQL.append(operator);
                }
                buildWhereNotRangeValues(whereSQL, notContainsOperation, field, valuesList.get(j));
            }
            whereSQL.append(")");
        }
    }

    /**
     * 获取where子句字段全名
     *
     * @param field
     * @return
     */
    protected String getWhereFieldFullName(QueryField field) {
        String fullName;
        if (field.isCalc()) {
            //计算字段，解析表达式作为where条件的一部分
            fullName = getCalcFieldExpr(field);
        } else {
            String fieldName = field.getMeta().getName();
            // 若过滤条件为输入(textarea)则过滤字段自身，不用过滤其key列
            MetaField meta = field.getMeta();
            //String fullName = dsFactTableAlias + "." + fieldName;
            String tableAlias = this.getTableAlias(field); //joinedTableAlias.get(cityField.getTable().getId());
            fullName = String.format("%s.%s", tableAlias, fieldName);
            // 字符串过滤时，大小写不敏感
            FieldFilterType filterType = FieldUtil.getFilterType(field); //meta.getFilterShowType();
            if (!Enabled.value(meta.getIsCaseSensitive())
                    && DataType.String == DataType.getType(meta.getDataType())
                    && FieldFilterType.Textarea == filterType) {
                fullName = function.lower(fullName);
            }
        }
        return fullName;
    }

    /**
     * 构建数值范围wheresql
     * @param field
     * @param values
     * @return
     */
    protected StringBuilder buildDecimalRangeWhereSql(QueryField field, List<FieldValue> values){
        if (StringUtil.isEmpty(values.get(0).getId())) {
            values.get(0).setId("0");
        }
        if (StringUtil.isEmpty(values.get(1).getId())) {
            values.get(1).setId("100000000");
        }
        StringBuilder whereSql = new StringBuilder();
        // 支持多段区间查询
        FieldUtil.sortRangeValues(field, values);

        String whereFieldFullName = getWhereFieldFullName(field);
        String sqlStr = "( %s BETWEEN %s AND %s )";
        FieldValue v1 = values.get(0);
        FieldValue v2 = values.get(1);
        whereSql.append(String.format(sqlStr, whereFieldFullName, v1.getId(), v2.getId()));

        return whereSql;
    }

    /**
     * 构建数值范围wheresql
     * @param field
     * @param values
     * @return
     */
    protected StringBuilder buildStringRangeWhereSql(QueryField field, List<FieldValue> values){
        StringBuilder whereSql = new StringBuilder();
        String whereFieldFullName = getWhereFieldFullName(field);

        String sqlStr = "( %s BETWEEN '%s' AND '%s' )";
        FieldValue v1 = values.get(0);
        FieldValue v2 = values.get(1);
        whereSql.append(String.format(sqlStr, whereFieldFullName, v1.getId(), v2.getId()));

        return whereSql;
    }


    /**
     * 构建日期范围wheresql
     * @param field
     * @param values
     * @return
     */
    protected StringBuilder buildDateRangeWhereSql(QueryField field, List<FieldValue> values){
        StringBuilder whereSql = new StringBuilder();

        //将不同日历类型的时间统一转化为日粒度
        values = normalizeToDay(values);

        // 支持多段区间查询
        FieldUtil.sortRangeValues(field, values);
        String whereFieldFullName = getWhereFieldFullName(field);

        // 通用日期统一使用原字段名过滤，提升查询性能
        // 其他日期需要格式化后过滤，兼容处理
        if(!field.isCommonDate() ||
                // 通用日期，元数据=非日粒度，需格式化后过滤，适配如保有量等场景
                (field.isCommonDate() && DateGranularity.DAY != DateGranularity.get(field.getMeta().getDateGranularity()))) {
            whereFieldFullName = this.dateFormat(field, whereFieldFullName);
        }

        List<FieldValue> fieldValues = this.convertDateFilterByDay(field, values); //FieldUtil.getDateFieldFilterValues(cityField, values); //FieldUtil.getFilterRealValues(cityField);
        FieldValue v1 = fieldValues.get(0);
        FieldValue v2 = fieldValues.get(values.size() - 1);
        String expression = String.format(" %s between '%s' and '%s' ", whereFieldFullName, v1.getId(), v2.getId());
        whereSql.append(expression);

        return whereSql;
    }

    /**
     * 将不同日历类型的时间统一转化为日粒度
     */
    public List<FieldValue> normalizeToDay(List<FieldValue> values){
        return values;
    }

    /**
     * 将不同日期粒度的过滤统一转为按天过滤：便于使用分区和处理月mtd日期问题
     * @param field
     * @param values
     * @return
     */
    protected List<FieldValue> convertDateFilterByDay(QueryField field, List<FieldValue> values){
        List<FieldValue> fieldValues = FieldUtil.getDateFieldFilterValues(field, values,this.config); //FieldUtil.getFilterRealValues(cityField);
        return fieldValues;
    }

    /**
     * 构建日期范围wheresql
     * @param field
     * @param values
     * @return
     */
    @Deprecated
    protected StringBuilder buildDateRangeWhereSql_bak(QueryField field, List<FieldValue> values){
        StringBuilder whereSql = new StringBuilder();
        // 支持多段区间查询
        FieldUtil.sortRangeValues(field, values);
        String whereFieldFullName = getWhereFieldFullName(field);
        whereFieldFullName = this.dateFormat(field, whereFieldFullName);

        List<String> filterFragments = new ArrayList<>();
        int valueCount = (values.size() / 2) * 2; // 只取2的倍数值
        for (int v = 0; v < valueCount; v = v + 2) {
            FieldValue v1 = values.get(v);
            FieldValue v2 = values.get(v + 1);
            String expression = "";
            List<String> coalesceParams = new ArrayList<>();
            coalesceParams.add(whereFieldFullName);
            coalesceParams.add("''");
            if (values.get(v).getId().contains(BIConsts.Filter_Value_Is_Null)) {
                expression = String.format("%s = '' ", function.coalesce(coalesceParams));
            } else if (values.get(v).getId().contains(BIConsts.Filter_Value_Is_Not_Null)) {
                expression = String.format("%s != '' ", function.coalesce(coalesceParams));
            } else {
                expression = String.format(" %s between '%s' and '%s' ", whereFieldFullName, v1.getId(), v2.getId());
            }

            filterFragments.add(expression);
        }
        whereSql.append("(").append(BIUtil.listToStr(filterFragments, " or ")).append(" )");

        return whereSql;
    }

    /**
     * 日期字段名称格式化
     *
     * @param field
     * @param fieldFullName
     * @return
     */
    protected String dateFormat(QueryField field, String fieldFullName) {

        MetaField meta = field.getMeta();
        String formatStr = meta.getShowFormatExpression();

        //是否是公共日期字段，公共日期通过前端日期控件的粒度格式化
        if(field.isCommonDate()) {
            formatStr = ShowFormatExpressionType.getFormatExpressionByDateGranularity(field.getQueryDateGranularity());
        }

        if(StrUtil.isEmpty(formatStr)){
            FieldFilterType fieldFilterType  = FieldUtil.getFilterType(field); //FieldFilterType.get(meta.getFilterShowType());
            switch (fieldFilterType){
                //起始月份兼容处理
                case MonthRange:
                    formatStr = IFunction.Format_Month;
                    break;
            }
        }

        if (!StringUtil.isEmpty(formatStr)) {
            IFunction dateFunction = FunctionManager.getFunction();
            fieldFullName = dateFunction.date2Char(fieldFullName, formatStr);
        }
        return fieldFullName;
    }

    /**
     * 构建非范围值的where条件
     */
    private void buildWhereNotRangeValues(StringBuilder whereSQL, String notContainsOperation, QueryField field, List<FieldValue> values) {
        String fieldFullName = getWhereFieldFullName(field);
        List<String> nulls = ImmutableList.of(SSDUtil.Null_String, BIConsts.NULL_VALUE, "");
        boolean hasNullValue = values.stream().anyMatch(v -> nulls.contains(StringUtils.lowerCase(v.getId())));
        String valueString = this.getFilterValues(values, field);
        boolean hasInFilter = false;
        if (BIUtil.isNotEmpty(valueString)) {
            if (hasNullValue) {
                whereSQL.append("(");
            }
            whereSQL.append("(");
            whereSQL.append(fieldFullName);
            whereSQL.append(notContainsOperation);
            whereSQL.append(" IN (");
            //whereSQL.append(getFilterValues(values, cityField));
            whereSQL.append(valueString);
            whereSQL.append(")");

            // 若是不包含且过滤值没有(null)，则需要将null添加到不包含过滤中
            if(!"".equalsIgnoreCase(notContainsOperation) && !hasNullValue ){
                whereSQL.append(String.format(" OR %s is null", fieldFullName));
            }
            whereSQL.append(")");

            hasInFilter = true;
        }

        FieldDataType dataType = FieldDataType.getType(field.getMeta().getDataType());
        if (hasNullValue) {
            if (hasInFilter) {
                if ("".equalsIgnoreCase(notContainsOperation)) {
                    whereSQL.append(" OR ");
                } else {
                    whereSQL.append(" AND ");
                }
            }
            whereSQL.append("(");
            whereSQL.append(fieldFullName).append(" IS ").append(notContainsOperation).append(" NULL ");
            if(dataType == FieldDataType.String){
                if ("".equalsIgnoreCase(notContainsOperation)) {
                    whereSQL.append(String.format(" OR %s = '' ", fieldFullName));
                }else {
                    whereSQL.append(String.format(" and %s not in('') ", fieldFullName));
                }
            }
            whereSQL.append(")");
            if (hasInFilter) {
                whereSQL.append(")");
            }
        }
    }

    protected String getCalcFieldExpr(QueryField field) {
        if (field == null || !field.isCalc()) {
            return null;
        }

        boolean isCustomDim = (!field.isMeasure() && field.isCalc()) || field.isCustomDimension();

        // 如果查询表为虚拟表，则直接返回计算维度的别名
        if (model.getFactTable().getView() != null && isCustomDim) {
            return String.format("%s.%s", dsFactTableAlias, field.getCode());
        }

        String expression = field.getMeta().getAggExpression();
        // 计算维度路由换表之后，下面这种方式取表达式的方式不对，注释掉
        //if(!cityField.isMeasure()&&!cityField.getCustomFieldConfigure().isEmpty()) {
        //    expression = cityField.getCustomFieldConfigure().getExpression();
        //}
        expression = rectifyDimCalcExpression(expression,field);

        Set<QueryField> atomFields = field.getCalcAtomFields();
        if (BIUtil.isNotEmpty(atomFields)) {
            for (QueryField atomField : atomFields) {
                if (BIUtil.isNotEmpty(expression) && expression.contains("[")) {
                    String tableAlias = this.getTableAlias(atomField);
                    String atomFieldFullName = tableAlias + "." + atomField.getMeta().getName();
                    if (atomField.isCalc()) {
                        atomFieldFullName = getCalcFieldExpr(atomField);
                    }

                    //使用replaceAll，第二个参数是替换模板，后者里如果出现了 $会抛 IllegalArgumentException: Illegal
                    if(atomFieldFullName.contains("$")){
                        expression = expression.replaceAll("\\[" + atomField.getCode() + "\\]", Matcher.quoteReplacement(atomFieldFullName));
                    }else{
                        expression = expression.replaceAll("\\[" + atomField.getCode() + "\\]", atomFieldFullName);
                    }


                    // 跨表表达式，示例：bi_test.ads_t1.biz_line
                    MetaTable metaTable = SSDMetaCacheManager.getTable(atomField.getMeta().getTableId());
                    if (metaTable != null) {
                        String replacement = metaTable.getFullName(true) + "." + atomField.getMeta().getName();
                        expression = expression.replaceAll("\\[" + replacement + "\\]", atomFieldFullName);
                    }

                    //兼容表达式使用字段id的场景
                    expression = expression.replaceAll("\\[" + atomField.getId() + "\\]", atomFieldFullName);
                }
            }
        }
        return function.tryCatch(expression);
    }

    /**
     * 矫正维度字段的计算表达式
     * @return
     */
    public String rectifyDimCalcExpression(String expression,QueryField field) {

        if (StrUtil.isEmpty(expression)) {
            return expression;
        }

        expression = expression.trim();

        if (expression.contains(BIConsts.BI_SHOP_OPEN_MONTH_FLAG)) {
            return getShopOpenMonthExpression(expression, field);
        }

        return expression;
    }

    /**
     * 表达式示例 bi_shop_open_month(substring(vf.dt, 1, 10), 'd', vd1.online_month)
     * @param expression
     * @param field
     * @return
     */
    public String getShopOpenMonthExpression(String expression, QueryField field){

        Set<QueryField> atomFieldSet = field.getCalcAtomFields();
        if (CollUtil.isEmpty(atomFieldSet)) {
            return expression;
        }

        List<QueryField> atomFieldsList = new ArrayList<>(atomFieldSet);

        //门店开业月份字段表达式，只支持开业年月使用相同的字段
        QueryField openMonth = atomFieldsList.get(0);
        String openMonthFieldAlias = getTableAlias(field);
        String openMonthFieldFullName = String.format("%s.%s", openMonthFieldAlias, openMonth.getMeta().getName());

        //统计时间字段表达式
        String statsDateExpression = buildShopOpenMonthDateFieldExpression();

        //获取时间粒度
        String dateGranularity = config.getSettings().getDateGranularity();

        String openMonthExpression = function.getShopOpenMonthExpression(statsDateExpression,dateGranularity,openMonthFieldFullName);

        //替换原始表达式
        openMonthExpression = expression.replaceAll(BIConsts.BI_SHOP_OPEN_MONTH_FLAG.replace("(","") + "\\(.*?\\)", openMonthExpression);

        return openMonthExpression;
    }

    /**
     * 获取门店开业月份统计时间表达式
     * @return
     */
    public String buildShopOpenMonthDateFieldExpression(){
        Optional<QueryField> optionalDateField =
                model.getFields().stream()
                        .filter(f -> "dt".equalsIgnoreCase(f.getCode()))
                        .findAny();

        //获取时间字段的表达式
        QueryField datefield = optionalDateField.get();
        String dateFieldAlias = getTableAlias(datefield);
        String statsDateExpression = FieldUtil.buildShopOpenMonthDateFieldExpression(datefield,dateFieldAlias);
        return statsDateExpression;
    }

    /**
     * 获取字段过滤值
     *
     * @param values
     * @return
     */
    protected String getFilterValues(List<FieldValue> values, QueryField field) {
        FieldDataType dataType = FieldDataType.getType(field.getMeta().getDataType());
        String result = "";
        List<String> valueStrList = new ArrayList<String>();
        List<String> nulls = ImmutableList.of(SSDUtil.Null_String, BIConsts.NULL_VALUE, "");
        for (FieldValue v : values) {
            if(nulls.contains(v.getId())){
                if (dataType == FieldDataType.String) {
                    valueStrList.add("");
                }
                continue;
            }
            /*
            if (SSDUtil.Null_String.equalsIgnoreCase(v.getId())) { // 排除空值
                if (dataType == FieldDataType.String) {
                    valueStrList.add("");
                }
                continue;
            } else if (BIConsts.NULL_VALUE.equalsIgnoreCase(v.getId())) { // 排除空值
                continue;
            }
             */
            valueStrList.add(v.getId());
        }
        switch (dataType) {
            case Integer:
            case Double:
                result = BIUtil.listToStr(valueStrList, ",");
                break;
            case String:
            default:
                result = BIUtil.listToStr(valueStrList, ",", "'");
                break;
        }
        return result;
    }

    /**
     * 获取字段所属表别名
     * @param field
     * @return
     */
    protected String getTableAlias(QueryField field) {
        if (field == null) {
            return null;
        }

        // 如果查询表为虚拟表，则直接返回事实表别名
        if (model.getFactTable().getView() != null) {
            return dsFactTableAlias;
        }

        String tableAlias = null;

        // 先重查询表id获取
        if (field.getTable() != null) {
            tableAlias = this.joinedTableAlias.get(field.getTable().getId());
            if (tableAlias != null) {
                return tableAlias;
            }
        }

        // 再重元配置表或
        if (field.getMeta() != null) {
            tableAlias = this.joinedTableAlias.get(field.getMeta().getTableId());
            if (tableAlias != null) {
                return tableAlias;
            }
        }

        // 最后兜底：从事实表获取
        if (tableAlias == null) {
            tableAlias = dsFactTableAlias;
        }
        return tableAlias;
    }

    /**
     * 与 {@link #buildDateRangeWhereSql} 一致的公共日期区间换算，判断过滤区间是否包含当天（多段区间任一包含即 true）。
     */
    private boolean isCommonDateFilterRangeContainsToday(QueryField commonDateField) {
        List<FieldValue> fieldValueList = commonDateField.getValues();

        List<String> dateList = new ArrayList<>();
        for (FieldValue v : fieldValueList) {
            dateList.add(v.getId());
        }

        Collections.sort(dateList);

        String today = DateUtil.today();
        Date d1 = DateUtil.parse(dateList.get(0));
        Date d2 = DateUtil.parse(dateList.get(dateList.size() - 1));
        boolean isContainToday = DateUtil.rangeToList(d1, d2, DateField.DAY_OF_YEAR)
                .stream().map(f -> f.toDateStr())
                .filter(d -> d.compareTo(today) >= 0).count() > 0;

        return isContainToday;
    }

    public StringBuilder buildGroupByClause() {
        StringBuilder groupBySql = new StringBuilder();
        return groupBySql;
    }
}
