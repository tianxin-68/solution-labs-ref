package com.bi.queryer.ssm.engine.lod;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.engine.*;
import com.bi.queryer.ssm.engine.analysis.AnalysisUtil;
import com.bi.queryer.ssm.engine.analysis.cfg.AnalysisItemConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.ctr.AnalysisContributionRateItemConfig;
import com.bi.queryer.ssm.engine.analysis.operator.BaseOperator;
import com.bi.queryer.ssm.engine.analysis.operator.OperatorContext;
import com.bi.queryer.ssm.engine.analysis.operator.OperatorFactory;
import com.bi.queryer.ssm.engine.analysis.operator.impl.ContributionRateOperator;
import com.bi.queryer.ssm.engine.analysis.operator.impl.TotalOperator;
import com.bi.queryer.ssm.engine.analysis.operator.impl.ZbThbOperator;
import com.bi.queryer.ssm.engine.config.field.FieldValue;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.function.FunctionManager;
import com.bi.queryer.ssm.engine.function.IFunction;
import com.bi.queryer.ssm.engine.lod.calc.LodCalcManager;
import com.bi.queryer.ssm.enums.*;
import com.bi.queryer.ssm.util.AnalysisTotalUtil;
import com.bi.queryer.ssm.util.FieldUtil;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.sys.db.DataType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.env.EnvVariableManager;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.StringUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 11:04 2023-12-19
 * @Description lod查询sql构建器
 **/
public class LodQuerySqlBuilder {
    private LodQueryConfigure lodQueryConfigure = null;
    private QueryContext cxt = null;
    private List<String> selectFragments = null;

    private LodQueryConfigureItem mainItem = null;

    private IFunction fx = null;

    private List<LodQueryConfigureItem> subItems = new ArrayList<>();

    protected LodCalcManager lodCalcMgr = null;

    //记录计算字段的表达式 和 指标编码的映射关系
    private Map<String,String> customMeasureCodeExpressionMap = new HashMap<>();

    //grp_v表达式
    private String grpValueExpression = "";

    public LodQuerySqlBuilder(LodQueryConfigure lodQueryConfigure, QueryContext cxt, LodCalcManager lodCalcMgr) {
        this.lodQueryConfigure = lodQueryConfigure;
        this.cxt = cxt;
        this.fx = FunctionManager.getFunction();
        this.lodCalcMgr = lodCalcMgr;
    }

    public String build(){
        List<LodQueryConfigureItem> items = lodQueryConfigure.getItems();
        subItems = new ArrayList<>();
        List<String> tips = new ArrayList<>();

        for(LodQueryConfigureItem item : items){
            QueryEngine engine = QueryFactory.createEngine(item.getConfig(), cxt);
            if(!item.isMainConfig()) {
                //LodSingleModelSqlBuilder singleModelSqlBuilder = new LodSingleModelSqlBuilder(item.getConfig(), this.cxt, item);

                LodSingleModelSqlBuilderAvgWrapper singleModelSqlBuilder = new LodSingleModelSqlBuilderAvgWrapper(new LodSingleModelSqlBuilder(item.getConfig(), this.cxt, item));

                engine.getSqlBuilder().setSingleModelSQLBuilder(singleModelSqlBuilder);
            }

            if(engine.getType() == QueryEngineType.Analysis) {
                AnalysisEngineLodWrapper analysisEngineLodWrapper = new AnalysisEngineLodWrapper(item.getConfig(), cxt, item);
                engine = analysisEngineLodWrapper;
            }
            String itemSql = engine.buildSql();

            item.setSql(itemSql);
            item.setFragments(engine.getSqlFragments());

            tips.add(engine.getSqlTips());

            if(item.isMainConfig()) {
                mainItem = item;
            }else {
                subItems.add(item);
            }
        }

        StringBuilder sql = new StringBuilder();
        sql.append(this.buildSelectClause());

        //构造计算指标的code与表达式的映射关系
        customMeasureCodeExpressionMap = SSDUtil.buildCustomMeasureCodeExpressionMap(selectFragments,lodQueryConfigure.getRawConfig());

        sql.append(this.buildFromClause());

        sql.append(this.buildWhereClause());

        sql.append(this.buildOrderByClause());

        String finalSql = sql.toString();
        for(String tip : tips) {
            finalSql = finalSql.replace(tip, "");
        }

        return finalSql;
    }

    protected String buildWithClause(){
        StringBuilder sql = new StringBuilder();

        return sql.toString();
    }

    protected String buildSelectClause(){
        StringBuilder sql = new StringBuilder();
        sql.append(" select ");

        selectFragments = new ArrayList<>();

        /**主视图*/
        List<String> mainSelectFragments = mainItem.getFragments().getSelectFragments();
        if(CollUtil.isNotEmpty(mainSelectFragments)){

            for(String itemSelectFragment : mainSelectFragments){
                String fieldAlias = "";
                if(itemSelectFragment.toLowerCase().contains(" as ")) {
                    fieldAlias = itemSelectFragment.split(" as | AS ")[1].trim();
                }else if(itemSelectFragment.toLowerCase().contains(".")){
                    fieldAlias = itemSelectFragment.split("\\.")[1].trim();
                }else {
                    fieldAlias = itemSelectFragment.trim();
                }

                String selectFragment = String.format("%s.%s", LodConsts.CONFIG_CODE_MAIN, fieldAlias);

                //记录grp_k的表达式
                if(BIConsts.GROUPING_KEY.equalsIgnoreCase(fieldAlias)){
                    grpValueExpression = selectFragment;
                }

                selectFragments.add(selectFragment);
            }

        } else {
            selectFragments.add(String.format("%s.%s", LodConsts.CONFIG_CODE_MAIN, "*"));
        }

        // 获取分组值和partition by字段
        Function<String, String> fieldSqlSupplier = field -> FieldUtil.getTableFiledExpression(Collections.singletonList(BIConsts.MAIN_TABLE_ALIAS), null, field);
        Map<Integer, List<String>> groupingPartitionMap = AnalysisTotalUtil.getGroupingPartitionMap(lodQueryConfigure.getRawConfig(), fieldSqlSupplier);

        /**子视图*/
        Map<String,String> expressionMap = new HashMap<>(16);
        for(LodQueryConfigureItem item : subItems){
            if(item.getCode().equalsIgnoreCase(LodConsts.CONFIG_CODE_MAIN)){
                continue;
            }
            QuerySqlFragments fragments = item.getFragments();
            if(fragments == null || fragments.getSelectFragments() == null) {
                continue;
            }

            List<String> dimCodes = item.getConfig().getResult().getRowDimensions().stream().map(QueryField::getCode).collect(Collectors.toList());
            //添加列维度
            dimCodes.addAll(item.getConfig().getResult().getColDimensions().stream().map(QueryField::getCode).collect(Collectors.toList()));

            List<String> itemSelectFragments = fragments.getSelectFragments();
            for(String itemSelectFragment : itemSelectFragments){
                String fieldAlias = "";
                if(itemSelectFragment.toLowerCase().contains(" as ")) {
                    fieldAlias = itemSelectFragment.toLowerCase().split(" as ")[1].trim();
                }else if(itemSelectFragment.toLowerCase().contains(".")){
                    fieldAlias = itemSelectFragment.toLowerCase().split("\\.")[1].trim();
                }else {
                    fieldAlias = itemSelectFragment.trim();
                }

                // 排除子视图的维度字段，避免重复
                String fa = fieldAlias;
                Optional<String> opt = dimCodes.stream().filter(f->f.equalsIgnoreCase(fa)).findAny();
                if(opt.isPresent()){
                    continue;
                }

                // 剔除常量：因主视图已存在
                if(BIConsts.GROUPING_VALUE.equalsIgnoreCase(fieldAlias)
                        || BIConsts.GROUPING_KEY.equalsIgnoreCase(fieldAlias)
                        || BIConsts.ROW_NUMBER_KEY.equalsIgnoreCase(fieldAlias)) {
                    continue;
                }


                QueryField lodQueryField = item.getLodField();
                QueryField queryField = item.getConfig().getResult().getFieldByCode(fieldAlias);
                String expression = item.getCode() + "." + fieldAlias;
                if (!AnalysisTotalAggType.isDefault(lodQueryField.getTotalAggType())) {
                    if (queryField != null && queryField.getAnalysisConfig() != null && (
                            AnalysisCalcType.RATIO == AnalysisCalcType.get(queryField.getAnalysisConfig().getCalcType())
                                    || AnalysisCalcType.VALUE == AnalysisCalcType.get(queryField.getAnalysisConfig().getCalcType())
                    )) {
                        // 率值和差值
                        expression = calcLodTotal(lodQueryField, queryField, fx, groupingPartitionMap, item.getCode());
                    } else {
                        // 处理汇总指标的自定义聚合方式  由于是需要从明细直接汇总，所以需要在此包一层
                        expression = AnalysisTotalUtil.buildTotalMeasureAggExpression(lodQueryField, expression, groupingPartitionMap, fieldSqlSupplier, BIConsts.GROUPING_KEY);
                    }
                    expressionMap.put(fieldAlias, String.format("(%s)", expression));
                }

                if (queryField != null && queryField.isTargetValue() &&
                        AnalysisCalcMode.CONTRIBUTION_RATE.getCode().equals(queryField.getAnalysisConfig().getCalcMode())) {
                    // 目标差值的贡献率在下面统一算，在此不做处理
                    continue;
                }

                // 权限不在sql层处理，在最后结果输出时做掩码，避免后续sql构建时数据类型不一致导致sql查询错误
                selectFragments.add(String.format("%s as %s", expression, fieldAlias));
            }

            //lod的占比和贡献率的计算
            List<String> lodZbExpressions = getLodZbExpressions(item, groupingPartitionMap);
            if(CollUtil.isNotEmpty(lodZbExpressions)){
                selectFragments.addAll(lodZbExpressions);
            }
        }

        List<String> expressions = lodCalcMgr.convertQueryExpression(lodQueryConfigure, expressionMap, BIConsts.GROUPING_KEY);
        if(BIUtil.isNotEmpty(expressions)){
            selectFragments.addAll(expressions);
        }

        // 分析指标的四则运算
        List<QueryField> dimFields = mainItem.getConfig().getResult().getRowDimensions().stream().filter(f -> !f.isAppend()).collect(Collectors.toList());
        List<String> analysisCalcExpressions = FieldUtil.getAnalysisCalcExpressions(lodQueryConfigure.getAnalysisCalcFields(), dimFields,
                selectFragments, lodQueryConfigure.getRawConfig(), BIConsts.MAIN_TABLE_ALIAS, BIConsts.GROUPING_KEY);
        if(BIUtil.isNotEmpty(analysisCalcExpressions)) {
            selectFragments.addAll(analysisCalcExpressions);
        }

        sql.append(BIUtil.listToStr(selectFragments));
        return sql.toString();
    }

    public String buildFromClause(){
        StringBuilder sql = new StringBuilder();
        sql.append(" from ").append(String.format("(%s) %s ", mainItem.getSql(), mainItem.getCode()));
        for(LodQueryConfigureItem subItem : subItems){
            sql.append(String.format(" left join (%s) %s ", subItem.getSql(), subItem.getCode()));
            List<QueryField> onDimensions = subItem.getAggToMainLevelDimensions();
            List<String> onFragments = new ArrayList<>();
            if(BIUtil.isEmpty(onDimensions)) {
                sql.append(" on 1=1 ");
                continue;
            }
            for(QueryField joinDimension : onDimensions){
                String leftExpression = fx.coalesce(String.format("%s.%s", mainItem.getCode(), joinDimension.getCode()), String.format("'%s'", BIConsts.SSM_ALL));
                String rightExpression = fx.coalesce(String.format("%s.%s", subItem.getCode(), joinDimension.getCode()), String.format("'%s'", BIConsts.SSM_ALL));
                onFragments.add(String.format("%s = %s", leftExpression, rightExpression));
                // onFragments.add(String.format("%s.%s = %s.%s", mainItem.getCode(), joinDimension.getCode(), subItem.getCode(), joinDimension.getCode()));
            }
            sql.append(String.format(" on %s", BIUtil.listToStr(onFragments, " and ")));
        }
        return sql.toString();
    }

    public String buildWhereClause(){

        StringBuilder sql = new StringBuilder();
        List<String> whereSegments = new ArrayList<>();

        //结果过滤
        List<QueryField> filterFields = lodQueryConfigure.getRawConfig().getFilter().getFields();
        for (QueryField field : filterFields) {

            if (!field.isActive()) {
                continue;
            }

            //不可度量字段直接跳过
            if (!field.isMeasure()) {
                continue;
            }

            //由于模板可能存在没有FilterValueMode的情况
            if (FieldFilterMode.detail == FieldFilterMode.get(field.getFilterValueMode())) {
                continue;
            }

            //************正式开始装配sql****************
            List<FieldValue> values = field.getValues();
            if (values == null || values.isEmpty()) {
                continue;
            }

            //计算指标的表达式从映射关系中获取
            if (FieldType.CUSTOM_MEASURE != FieldType.get(field.getFieldType())) {
                continue;
            }

            values.forEach(fv -> {
                fv.setId(EnvVariableManager.value(fv.getId()));
            });
            // 字段数据类型
            FieldDataType dataType = FieldDataType.getType(field.getMeta().getDataType());
            if (values.isEmpty()) {
                continue;
            }

            String expression = customMeasureCodeExpressionMap.get(field.getCode());
            if (StrUtil.isEmpty(expression)) {
                continue;
            }

            String whereFieldName = String.format("(%s)", expression);

            FieldFilterType filterType = FieldUtil.getFilterType(field);
            if (filterType.isRange()) {
                if (dataType == FieldDataType.Integer || dataType == FieldDataType.Double) {
                    if (StringUtil.isEmpty(values.get(0).getId())) {
                        values.get(0).setId("0");
                    }
                    if (StringUtil.isEmpty(values.get(1).getId())) {
                        values.get(1).setId("100000000");
                    }
                    String v1 = values.get(0).getId();
                    String v2 = values.get(1).getId();

                    //判断是否过滤汇总行
                    FieldFilterObjectType objectType = FieldFilterObjectType.get(field.getFilterObject());
                    if (FieldFilterObjectType.DETAIL == objectType && StrUtil.isNotEmpty(grpValueExpression)) {
                        whereSegments.add(String.format(" ( %s between %s and %s )  or ( %s != 0 ) ", whereFieldName, v1, v2, grpValueExpression));
                    } else {
                        whereSegments.add(String.format("%s between %s and %s", whereFieldName, v1, v2));
                    }

                }
            }

        }

        if (BIUtil.isNotEmpty(whereSegments)) {
            sql.append(" where ").append(BIUtil.listToStr(whereSegments, " AND ", "(", ")"));
        }

        return sql.toString();
    }

    public String buildOrderByClause() {
        StringBuilder orderBySQL = new StringBuilder();

        if(!mainItem.getConfig().getSettings().getNeedSort()) {
            return "";
        }

        List<String> orderByFragments = new ArrayList<String>();

        List<String> resultFieldCodes = new ArrayList<>();
        resultFieldCodes.addAll(mainItem.getConfig().getResult().getFields().stream()
                .filter(f->Enabled.value(f.getIsShow())&&!f.isAppend())
                .map(QueryField::getCode).collect(Collectors.toList()));

        for(String itemSelectFragment : selectFragments) {
            String fieldAlias = "";
            if (itemSelectFragment.toLowerCase().contains(" as ")) {
                fieldAlias = itemSelectFragment.toLowerCase().split(" as ")[1].trim();
                resultFieldCodes.add(fieldAlias);
            }
        }

        orderByFragments.addAll(FieldUtil.getOrderByFragments(mainItem.getConfig(), resultFieldCodes));
        List<String> sortFieldCodes = mainItem.getConfig().getSettings().getQuerySortFieldCodes(mainItem.getConfig());

//        if (sql.contains(" " + BIConsts.ROW_NUMBER_KEY)) {
//            orderByFragments.add(String.format(" %s ", BIConsts.ROW_NUMBER_KEY));
//        }

        List<QueryField> orderByFields = new ArrayList<>();
        AnalysisTotalType totalType = AnalysisUtil.getOrderByTotalType(mainItem.getConfig());
        if (totalType.isActive()) {
            TotalOperator totalOperator = OperatorFactory.getTotalOperator(totalType);
            orderByFields = totalOperator.orderByFields(mainItem.getConfig());
        }

        boolean hasGroupValueField = false;
        for (QueryField field : orderByFields) {

            //已排序，不再处理
            if(sortFieldCodes.contains(field.getCode())){
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

        List<QueryField> dimFields = mainItem.getConfig().getResult().getRowDimensions().stream().filter(f->!f.isAppend()).collect(Collectors.toList());
        // 设置行总计排到最前面
        if (hasGroupValueField && BIUtil.isNotEmpty(dimFields)) {

            Long colDimCount = mainItem.getConfig().getResult().getFields().stream().filter(f->!f.isAppend()).filter(f->f.getRawQueryArea() == QueryArea.ColumnDimension).count();

            String fieldGroupingBit = StringUtils.rightPad("", 0, "0");
            String groupingBit = StringUtils.rightPad(fieldGroupingBit, dimFields.size(), "1") + StringUtils.rightPad("", colDimCount.intValue(), "0");
            int groupingValue = NumberUtil.binaryToInt(groupingBit);

            String groupingValueField = BIConsts.GROUPING_KEY;
            String colTotalOrderByFragment = String.format("if(%s=%s, 1, 0) desc nulls last", groupingValueField, groupingValue);
            if (colDimCount>0) {
                colTotalOrderByFragment = String.format("if(%s in (%s,%s), 1, 0) desc nulls last", groupingValueField, groupingValue, groupingValue + 1);
            }

            orderByFragments.add(0, colTotalOrderByFragment);
        }

        if (orderByFragments.isEmpty()) {
            return "";
        }

        orderBySQL.append(" ").append(BIConsts.ORDER_BY).append(" ");
        orderBySQL.append(BIUtil.listToStr(orderByFragments, ","));

        return orderBySQL.toString();
    }

    protected String appendSortField(String sql){
        if(BIUtil.isEmpty(sql)){
            return sql;
        }
        if(!sql.contains(BIConsts.ORDER_BY)) {
            return sql;
        }
        String newSql = String.format(" select main_v.*, row_number() over() as %s from (%s) main_v ", BIConsts.ROW_NUMBER_KEY, sql);
        return newSql;
    }

    /**
     * 获取lod占比的表达式，在最外层计算
     * @return
     */
    public List<String> getLodZbExpressions(LodQueryConfigureItem item, Map<Integer, List<String>> groupingPartitionMap) {
        List<String> result = new ArrayList<>();

        QueryField lodField = item.getLodField();
        List<QueryField> analysisMeasures = item.getConfig().getResult().getMeasures().stream().filter(f-> Enabled.isTrue(f.getIsAnalysis())).collect(Collectors.toList());
        // 获取分组值和partition by字段
        Function<String, String> fieldSqlSupplier = field -> FieldUtil.getTableFiledExpression(Collections.singletonList(BIConsts.MAIN_TABLE_ALIAS), null, field);

        for(QueryField queryField : analysisMeasures) {

            //不是lod字段，不处理
            //lod字段前缀标识 = _ctm_m_lod_
            if(!queryField.getCode().startsWith(BIConsts.LOD_FIELD_CTM_SUFFIX)) {
                continue;
            }

            String analysisExpression = "";
            AnalysisItemConfig itemConfig = queryField.getAnalysisConfig();

            AnalysisCalcMode calcMode = AnalysisCalcMode.get(itemConfig.getCalcMode());
            if (!calcMode.isZb() && AnalysisCalcMode.CONTRIBUTION_RATE != calcMode && AnalysisCalcMode.ZB_THB != calcMode  ) {
                continue;
            }

            Map<String, QueryField> measureFieldMap = item.getConfig().getResult().getMeasures().stream().collect(Collectors.toMap(QueryField::getId, QueryField->QueryField, (f1, f2)->f1));
            QueryField rawMeasureField = measureFieldMap.get(itemConfig.getMeasureId());

            String ratioUnit = PercentFieldRatioUnitType.PERCENT.getCode();

            // 百分比指标
            if (FieldUtil.isPercentField(rawMeasureField)) {
                ratioUnit = itemConfig.getPercentFieldRatioUnit();
            }

            BaseOperator operator = OperatorFactory.getOperator(itemConfig.getCalcMode());
            if (operator != null) {
                OperatorContext operatorContext = new OperatorContext();
                operatorContext.setConfig(mainItem.getConfig());

                List<QueryField> dimFields = mainItem.getConfig().getResult().getRowDimensions().stream().filter(f -> !f.isAppend()).collect(Collectors.toList());
                operatorContext.setDimFields(dimFields);

                String measureCode = item.getLodField().getCode();

                //贡献度处理
                if (AnalysisCalcMode.CONTRIBUTION_RATE == AnalysisCalcMode.get(itemConfig.getCalcMode())) {
                    ContributionRateOperator ctrOperator = new ContributionRateOperator();

                    AnalysisContributionRateItemConfig ctrItemConfig = (AnalysisContributionRateItemConfig) itemConfig;
                    AnalysisCalcMode ctrCalcMode = AnalysisCalcMode.get(ctrItemConfig.getCtrCalcMode());
                    String compareRealValueExpression = "";
                    if (ctrCalcMode == AnalysisCalcMode.CUSTOM_COMPARE) {
                        compareRealValueExpression = String.format("%s_%s_%s_%s", measureCode, ctrCalcMode.getCode(), itemConfig.getCompareIndex(), AnalysisCalcType.REAL_VALUE.getCode());
                    } else {
                        compareRealValueExpression = String.format("%s_%s_%s", measureCode, ctrCalcMode.getCode(), AnalysisCalcType.REAL_VALUE.getCode());
                    }

                    //String diffValue = String.format("COALESCE(%s.%s,0) - COALESCE(%s.%s,0)",
                    //        item.getCode(), measureCode,
                    //        item.getCode(), compareRealValueExpression);

                    String diffValue;
                    if (queryField.isTargetValue()) {
                        // 计算目标值的贡献率
                        diffValue = String.format(" %s.%s_%s_ctr_ratio ",
                                item.getCode(),
                                measureCode,
                                itemConfig.getTargetConfig().getTargetCalcMode());
                    } else {
                        diffValue = String.format("COALESCE(%s,0) - COALESCE(%s,0)",
                                buildLodTotalFieldExpression(item.getCode(), measureCode, lodField, groupingPartitionMap, fieldSqlSupplier),
                                buildLodTotalFieldExpression(item.getCode(), compareRealValueExpression, lodField, groupingPartitionMap, fieldSqlSupplier)
                        );
                    }

                    analysisExpression = ctrOperator.getLodCtrExpression(operatorContext, diffValue);

                }else if(AnalysisCalcMode.ZB_THB == AnalysisCalcMode.get(itemConfig.getCalcMode())){
                    ZbThbOperator zbThbOperator = new ZbThbOperator();

                    String currentMeasureValue = buildLodTotalFieldExpression(item.getCode(), measureCode, lodField, groupingPartitionMap, fieldSqlSupplier);//String.format("%s.%s", item.getCode(), measureCode);
                    String rawThbCalcMode = queryField.getAnalysisConfig().getRawThbCalcMode().getCode();
                    String analysisMeasureCode;
                    if (AnalysisCalcMode.CUSTOM_COMPARE.getCode().equals(rawThbCalcMode)) {
                        analysisMeasureCode = String.format("%s_%s_%s_%s", measureCode, rawThbCalcMode, itemConfig.getCompareIndex(), AnalysisCalcType.REAL_VALUE.getCode());
                    } else {
                        analysisMeasureCode = String.format("%s_%s_%s", measureCode, rawThbCalcMode, AnalysisCalcType.REAL_VALUE.getCode());
                    }
                    String analysisMeasureValue = buildLodTotalFieldExpression(item.getCode(), analysisMeasureCode, lodField, groupingPartitionMap, fieldSqlSupplier); //String.format("%s.%s_%s_%s", item.getCode(),measureCode, rawThbCalcMode, AnalysisCalcType.REAL_VALUE.getCode());
                    analysisExpression = zbThbOperator.getLodCalcExpression(operatorContext, queryField,currentMeasureValue,analysisMeasureValue,ratioUnit);

                } else {

                    String measureName = buildLodTotalFieldExpression(item.getCode(), measureCode, lodField, groupingPartitionMap, fieldSqlSupplier);
                    analysisExpression = operator.lodCalc(operatorContext, measureName,ratioUnit);

                    //指标为文本类型，不出占比
                    if(queryField.getMeta() != null) {
                        DataType dataType = DataType.getType(queryField.getMeta().getDataType());
                        if(DataType.String == dataType){
                            analysisExpression = "null";
                        }
                    }
                }

            }

            //if (StringUtils.isNotEmpty(queryField.getTotalAggType())) {
            //    // 处理汇总指标的自定义聚合方式  由于是需要从明细直接汇总，所以需要在此包一层
            //    analysisExpression = AnalysisTotalUtil.buildTotalMeasureAggExpression(queryField, analysisExpression, groupingPartitionMap, fieldSqlSupplier, BIConsts.GROUPING_KEY);
            //}

            if (StrUtil.isNotEmpty(analysisExpression)) {
                result.add(String.format("%s as %s", analysisExpression, queryField.getCode()));
            }

        }

        return result;
    }

    // 处理lod汇总
    private String buildLodTotalFieldExpression(String alias, String measureCode, QueryField queryField,
                                                Map<Integer, List<String>> groupingPartitionMap,
                                                Function<String, String> fieldSqlSupplier) {
        String analysisExpression = String.format("%s.%s", alias, measureCode);
        if (AnalysisTotalAggType.isDefault(queryField.getTotalAggType())) {
            return analysisExpression;
        }

        // 处理汇总指标的自定义聚合方式  由于是需要从明细直接汇总，所以需要在此包一层
        return AnalysisTotalUtil.buildTotalMeasureAggExpression(queryField, analysisExpression, groupingPartitionMap,
                fieldSqlSupplier, BIConsts.GROUPING_KEY);
    }

    public String calcLodTotal(QueryField rawMeasureField, QueryField queryField, IFunction function, Map<Integer, List<String>> groupingPartitionMap, String alias) {
        AnalysisItemConfig cfg = queryField.getAnalysisConfig();
        String calcExpression = null;
        String formatString;
        String rawCode = rawMeasureField.getCode();

        // 获取分组值和partition by字段
        AnalysisCalcMode analysisCalcMode = AnalysisCalcMode.get(cfg.getCalcMode());
        Function<String, String> fieldSqlSupplier = field -> FieldUtil.getTableFiledExpression(Collections.singletonList(BIConsts.MAIN_TABLE_ALIAS), null, field);
        String analysisCode;
        String currentValue = AnalysisTotalUtil.buildTotalMeasureAggExpression(rawMeasureField, alias + "." + rawCode, groupingPartitionMap, fieldSqlSupplier, BIConsts.GROUPING_KEY);
        if (AnalysisCalcMode.CUSTOM_COMPARE == analysisCalcMode) {
            analysisCode = String.format("%s.%s_%s_%s_%s", alias, rawCode, cfg.getCalcMode(), cfg.getCompareIndex(), AnalysisCalcType.REAL_VALUE.getCode());
        } else {
            analysisCode = String.format("%s.%s_%s_%s", alias, rawCode, cfg.getCalcMode(), AnalysisCalcType.REAL_VALUE.getCode());
        }
        String analysisValue = AnalysisTotalUtil.buildTotalMeasureAggExpression(rawMeasureField, analysisCode, groupingPartitionMap, fieldSqlSupplier, BIConsts.GROUPING_KEY);

        if (AnalysisCalcType.VALUE == AnalysisCalcType.get(cfg.getCalcType())) {
            formatString = "(%s) - (%s)";
            String analysisExpression = String.format(
                    formatString,
                    currentValue,
                    analysisValue
            );
            calcExpression = function.tryCatch(analysisExpression);
        } else if (AnalysisCalcType.RATIO == AnalysisCalcType.get(cfg.getCalcType())) {
            PercentFieldRatioUnitType percentFieldRatioUnitType = PercentFieldRatioUnitType.get(cfg.getPercentFieldRatioUnit());

            if (FieldUtil.isPercentField(rawMeasureField)) {
                //按 % 计算
                if (PercentFieldRatioUnitType.PERCENT == percentFieldRatioUnitType) {
                    String numeratorSql = String.format("((%s) - (%s)) * %s",
                            currentValue,
                            analysisValue,
                            BIConsts.INT_TO_DOUBLE_PRECISION
                    );
                    String denominatorSql = analysisValue;

                    denominatorSql = function.abs(denominatorSql);
                    calcExpression = function.division(numeratorSql, denominatorSql, false);

                    calcExpression = function.tryCatch(calcExpression);
                } else {
                    formatString = "(%s) - (%s)";
                    String analysisExpression = String.format(
                            formatString,
                            currentValue,
                            analysisValue
                    );
                    calcExpression = function.tryCatch(analysisExpression);
                }
            } else {
                String numeratorSql = String.format("((%s) - (%s)) * %s",
                        currentValue,
                        analysisValue,
                        BIConsts.INT_TO_DOUBLE_PRECISION
                );
                String denominatorSql = analysisValue;

                denominatorSql = function.abs(denominatorSql);
                calcExpression = function.division(numeratorSql, denominatorSql, false);

                calcExpression = function.tryCatch(calcExpression);
            }
        }
        return calcExpression;
    }

}
