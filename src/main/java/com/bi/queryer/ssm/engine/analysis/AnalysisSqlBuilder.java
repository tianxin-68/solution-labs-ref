package com.bi.queryer.ssm.engine.analysis;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.SingleModelSqlBuilder;
import com.bi.queryer.ssm.engine.analysis.cfg.AnalysisItemConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.total.AnalysisTotalConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.total.AnalysisTotalItemConfig;
import com.bi.queryer.ssm.engine.analysis.dataset.*;
import com.bi.queryer.ssm.engine.analysis.operator.BaseOperator;
import com.bi.queryer.ssm.engine.analysis.operator.OperatorContext;
import com.bi.queryer.ssm.engine.analysis.operator.OperatorFactory;
import com.bi.queryer.ssm.engine.analysis.operator.impl.TotalOperator;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.FieldValue;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.cross.CrossDimensionQueryEngine;
import com.bi.queryer.ssm.engine.function.FunctionManager;
import com.bi.queryer.ssm.engine.function.IFunction;
import com.bi.queryer.ssm.engine.result.ResultDataSetColumn;
import com.bi.queryer.ssm.enums.*;
import com.bi.queryer.ssm.util.FieldUtil;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.env.EnvVariableManager;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.StringUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 17:44 2023-07-10
 * @Description 所有分析场景的sql构建器
 **/
public class AnalysisSqlBuilder {

    // 源查询sql
    protected String querySql = "";

//    protected String orderBySql = "";

    protected final String ds1 = BIConsts.ANALYSIS_CURRENT_DATASET; //"ds1";

    //数据源前缀
    protected final String dsPrefix = "ds_";

    protected Map<String, AnalysisDataSet> analysisDataSetMap = new LinkedHashMap<>();

    protected AnalysisDataSet currentDataSet = null;

    protected QueryConfigure config = null;

    protected QueryContext cxt = null;

    protected QueryEngine queryEngine = null;

    protected List<QueryField> measureFields = null;

    protected IFunction fx = null;

    private List<String> selectFragments = null;

    private List<String> fromFragments = null;

    // 列总计分组值：用于列总计排序到最前面
    private Integer colTotalGroupingValue = 0;

    //记录计算字段的表达式 和 指标编码的映射关系
    private Map<String,String> customMeasureCodeExpressionMap = new HashMap<>();

    //grp_v表达式
    private String grpValueExpression = "";

    public AnalysisSqlBuilder(QueryConfigure config, QueryContext cxt, QueryEngine queryEngine) {
        this.config = config;
        this.cxt = cxt;
        this.queryEngine = queryEngine;
        fx = FunctionManager.getFunction();
    }

    public String build() throws BIException {
        // 从主配置中移除含分析指标的计算字段
        List<QueryField> analysisCalcFields = new ArrayList<>(8);
        for (QueryField f : config.getResult().getMeasures()) {
            if (f.isAnalysisCalc()) {
                analysisCalcFields.add(f);
            }
        }

        // 获取原始查询sql
        AnalysisSingleModelSqlBuilder singleModelSqlBuilder = this.createAnalysisSingleModeSqlBuilder(config, cxt); //new AnalysisSingleModelSqlBuilder(config, cxt);

        // 支持总计/小计的分组统计
        singleModelSqlBuilder.setBuildTotalGrouping(true);

        AnalysisSingleModelSqlBuilderAvgWrapper avgWrapper = this.createAnalysisSingleModeSqlBuilderAvgWrapper(singleModelSqlBuilder);
        this.querySql = this.getQuerySql(avgWrapper); //this.getQuerySql(singleModelSqlBuilder);
//        this.querySql = sqlArray[0];
//        this.orderBySql = sqlArray[1];

        // 构建分析数据集
        this.analysisDataSetMap = this.buildAnalysisDataSet();

        // 查询指标字段
        this.measureFields = this.getSelectMeasureFields();

        // 当前数据集
        this.currentDataSet = this.analysisDataSetMap.get(AnalysisCalcMode.NONE.getCode());

        StringBuilder sql = new StringBuilder();

        StringBuilder withSql = this.buildWithClause();
        sql.append(withSql);

        StringBuilder selectSql = this.buildSelectClause(analysisCalcFields);
        sql.append(selectSql);

        //构造计算指标的code与表达式的映射关系
        customMeasureCodeExpressionMap = SSDUtil.buildCustomMeasureCodeExpressionMap(selectFragments,config);

        StringBuilder fromSql = this.buildFromClause();
        sql.append(fromSql);

        StringBuilder whereSql = this.buildWhereClause();
        sql.append(whereSql);

        if(config.getSettings().getSortMode() == SortMode.NORMAL) {
            StringBuilder orderBySql = this.buildOrderByClause();
            sql.append(orderBySql);
        }
        return sql.toString();
    }

    /**
     * 获取查询sql
     * @return
     */
    protected String getQuerySql(SingleModelSqlBuilder singleModelSqlBuilder){
        this.queryEngine.getSqlBuilder().setSingleModelSQLBuilder(singleModelSqlBuilder);

        // 子查询不需要排序
        boolean needSort = this.queryEngine.getConfig().getSettings().getNeedSort();
        this.queryEngine.getConfig().getSettings().setNeedSort(false);
        String rawSql = this.queryEngine.buildSql();
        this.queryEngine.getConfig().getSettings().setNeedSort(needSort);
        // 去掉tips
        String tips = queryEngine.getSqlTips();
        rawSql = rawSql.replace(tips, "");

        return rawSql;
    }

    /**
     * with子句
     * @return
     */
    protected StringBuilder buildWithClause() {
        StringBuilder sql = new StringBuilder();
        sql.append(String.format("with %s as (%s)", ds1, this.querySql));
        boolean hasThb = false;// 判断是否有同环比
        for (int j = 0; j < measureFields.size(); j++) {
            QueryField measureField = measureFields.get(j);
            // 分析字段
            if (Enabled.value(measureField.getIsAnalysis())) {
                AnalysisItemConfig cfg = measureField.getAnalysisConfig();
                AnalysisCalcMode calcMode = AnalysisCalcMode.get(cfg.getCalcMode());
                hasThb = hasThb || calcMode.isCompare() || AnalysisCalcMode.CONTRIBUTION_RATE == calcMode || AnalysisCalcMode.ZB_THB == calcMode;
            }
        }

        // 无同环比，直接返回
        if (!hasThb) {
            return sql;
        }

        //同环比与自定义对比 每个都拆分为单独数据源
        //存储数据源的key，避免重复添加
        Map<String, String> withClauseKeymap = new HashMap<>();
        for (String analysisDataSetKey : analysisDataSetMap.keySet()) {

            if (StrUtil.isEmpty(analysisDataSetKey)) {
                continue;
            }

            String dsKey = getWithClauseKey(analysisDataSetKey);

            if (withClauseKeymap.containsKey(dsKey)) {
                continue;
            }

            String calcMode = analysisDataSetKey.contains(AnalysisCalcMode.CUSTOM_COMPARE.getCode()) ? AnalysisCalcMode.CUSTOM_COMPARE.getCode() : analysisDataSetKey;

            // 对照源：需要添加分析所需相关数据
            AnalysisSingleModelSqlBuilder singleModelSqlBuilder =  this.createAnalysisSingleModeSqlBuilder(config, cxt);//new AnalysisSingleModelSqlBuilder(config, cxt);

            boolean isCustomCompare = AnalysisCalcMode.CUSTOM_COMPARE == AnalysisCalcMode.get(calcMode);
            singleModelSqlBuilder.setAnalysisCalcMode(AnalysisCalcMode.get(calcMode));

            if (isCustomCompare) {
                Integer customCompareIndex = Integer.parseInt(analysisDataSetKey.replace(AnalysisCalcMode.CUSTOM_COMPARE.getCode() + "_", ""));
                singleModelSqlBuilder.setCustomCompareIndex(customCompareIndex);
            }

            // 对照源需要添加过滤日期但不需要总计/小计(废弃）
            // 设置是否是数据对比
            singleModelSqlBuilder.setCompare(true);

            AnalysisTotalConfig totalConfig = config.getAnalysis().getTotal();
            boolean isNeedGrouping = false;
            if (totalConfig.isActive()) {
                for (AnalysisTotalItemConfig itemConfig : totalConfig.getItems()) {

                    //（列小计 || 列总计 || 行总计），需要Grouping
                    isNeedGrouping = (
                               itemConfig.getTotalType() == AnalysisTotalType.COL_SUBTOTAL
                            || itemConfig.getTotalType() == AnalysisTotalType.ROW_TOTAL
                            || itemConfig.getTotalType() == AnalysisTotalType.COL_TOTAL
                    );
                    if (isNeedGrouping) {
                        break;
                    }
                }
            }

            //日期汇总，需要Grouping
            if (config.isAggQuery()) {
                isNeedGrouping = true;
            }

            // 列小计、行总计支持同环比
            singleModelSqlBuilder.setBuildTotalGrouping(isNeedGrouping);

            AnalysisSingleModelSqlBuilderAvgWrapper avgWrapper = this.createAnalysisSingleModeSqlBuilderAvgWrapper(singleModelSqlBuilder);
            String singleModelSql = this.getQuerySql(avgWrapper);//this.getQuerySql(singleModelSqlBuilder);

            sql.append(String.format(",%s as (%s) ", dsKey, singleModelSql));
            withClauseKeymap.put(dsKey, dsKey);
        }

        return sql;
    }

    /**
     * 获取with子句的key
     * @param analysisDataSetKey
     * @return
     */
    public String getWithClauseKey(String analysisDataSetKey) {
        String dsKey = String.format("%s%s", dsPrefix, analysisDataSetKey);
        return dsKey;
    }

    /**
     * select子句：包含了同环占比所有字段
     * @return
     */
    protected StringBuilder buildSelectClause(List<QueryField> analysisCalcFields){
        StringBuilder sql = new StringBuilder();

        Map<String, QueryField> measureFieldMap = measureFields.stream().collect(Collectors.toMap(QueryField::getId, QueryField->QueryField, (f1, f2)->f1));
        Long colDimCount = config.getResult().getFields().stream().filter(f->!f.isAppend()).filter(f->f.getRawQueryArea() == QueryArea.ColumnDimension).count();
        /** 行维度 */
        selectFragments = new ArrayList<>();

        List<QueryField> dimFields = config.getResult().getRowDimensions().stream().filter(f->!f.isAppend()).collect(Collectors.toList());

        Map<Integer, String> groupingCodes = new LinkedHashMap<>();

        //处理列维度
        List<QueryField> colDimFields = config.getResult().getFields().stream().filter(f->!f.isAppend()).filter(f->f.getRawQueryArea() == QueryArea.ColumnDimension).collect(Collectors.toList());
        for(QueryField selectField : colDimFields){
            String selectFragment = this.getFullJoinFieldExpression(selectField);
            selectFragment = this.buildSelectColFieldExpression(selectFragment);

            selectFragment = String.format("%s as %s", selectFragment, selectField.getCode());
            selectFragments.add(selectFragment);
        }

        int index = -1;
        for (QueryField selectField : dimFields) {
            index++;
            String fieldGroupingBit = StringUtils.rightPad("", index, "0");
            String groupingBit = StringUtils.rightPad(fieldGroupingBit, dimFields.size(), "1") + StringUtils.rightPad("", colDimCount.intValue(), "0");
            int groupingValue = NumberUtil.binaryToInt(groupingBit);
            AnalysisTotalType totalType = (index == 0) ? AnalysisTotalType.COL_TOTAL : AnalysisTotalType.COL_SUBTOTAL;

            String selectFragment = this.getFullJoinFieldExpression(selectField);
            selectFragment = String.format("%s as %s", selectFragment, selectField.getCode());
            selectFragments.add(selectFragment);

            groupingCodes.put(groupingValue, totalType.getCode());
            if (totalType == AnalysisTotalType.COL_TOTAL) {
                colTotalGroupingValue = groupingValue;
            }
        }

        /** 指标（含同环占比/汇总） */
        for (int j = 0; j < measureFields.size(); j++) {
            QueryField measureField = measureFields.get(j);
            if (measureField.isAnalysisCalc()) {
                continue;
            }

            // 不是分析字段
            if(!Enabled.value(measureField.getIsAnalysis())) {
                if (measureField.isAppend()) {
                    continue;
                }
                selectFragments.add(String.format("%s.%s", currentDataSet.getName(), measureField.getCode()));
                continue;
            }

            // 分析字段
            AnalysisItemConfig cfg = measureField.getAnalysisConfig();
            String analysisDataSetKey = buildAnalysisDataSetKey(cfg);
            AnalysisDataSet analysisDataSet = analysisDataSetMap.get(analysisDataSetKey);
            AnalysisCalcMode calcMode = AnalysisCalcMode.get(cfg.getCalcMode());

            if (measureField.isTargetValue() && calcMode == AnalysisCalcMode.NONE) {
                // 普通的目标值指标，不需要计算，只有贡献率需要往下走
                selectFragments.add(String.format("%s.%s", currentDataSet.getName(), measureField.getCode()));
                continue;
            }

            // 原始指标
            QueryField rawMeasureField = measureFieldMap.get(cfg.getMeasureId());
            if(rawMeasureField == null){
                continue;
            }

            OperatorContext operatorContext = new OperatorContext(dimFields, measureFields, currentDataSet, analysisDataSet);
            operatorContext.setCrossDimensionQuery(this.isCrossDimensionQuery());
            operatorContext.setAnalysisItemConfig(cfg);
            operatorContext.setConfig(config);
            BaseOperator operator = OperatorFactory.getOperator(calcMode);
            if(operator != null){

                //lod指标，占比在最外层计算
                if(Enabled.value(config.getSettings().getIsLodQuery())) {
                    if (calcMode.isZb() || AnalysisCalcMode.CONTRIBUTION_RATE == calcMode || AnalysisCalcMode.ZB_THB == calcMode) {
                        if (measureField.isTargetValue() && AnalysisCalcMode.CONTRIBUTION_RATE == calcMode) {
                            // lod目标值的贡献率特殊处理, 在lod最外层算
                            selectFragments.add(String.format("%s.%s", currentDataSet.getName(), measureField.getCode()));
                        }
                        continue;
                    }
                }

                String calcExpression = operator.calc(operatorContext, measureField);
                selectFragments.add(String.format("%s as %s", calcExpression, measureField.getCode()));
            }
        }

        //添加包含分析指标的计算字段
        List<String> analysisCalcExpressions = FieldUtil.getAnalysisCalcExpressions(analysisCalcFields, dimFields, selectFragments, config, ds1, BIConsts.GROUPING_VALUE);
        if(BIUtil.isNotEmpty(analysisCalcExpressions)) {
            selectFragments.addAll(analysisCalcExpressions);
        }

        // 添加分组信息
        List<String> caseWhenList = new ArrayList<>();
        String groupingValueField = this.getFullJoinFieldExpression(BIConsts.GROUPING_VALUE);
        grpValueExpression = groupingValueField;

        for(Integer groupValue : groupingCodes.keySet()) {
            String caseWhenExpression = String.format(
                    "when %s then '%s'",
                    groupValue,
                    groupingCodes.get(groupValue)
            );
            caseWhenList.add(caseWhenExpression);
        }

        //行总计+列小计+列总计
        if(this.isCrossDimensionQuery()) {
            for (Integer groupValue : groupingCodes.keySet()) {
                String caseWhenExpression = String.format(
                        "when %s then '%s'",
                        groupValue + 1,
                        groupingCodes.get(groupValue)
                );
                caseWhenList.add(caseWhenExpression);
            }
        }

        String groupingExpression = "";
        if(BIUtil.isNotEmpty(caseWhenList)) {
            groupingExpression = String.format(" case %s %s else null end as %s", groupingValueField, BIUtil.listToStr(caseWhenList, " "), BIConsts.GROUPING_VALUE);
        }else {
            groupingExpression = String.format(" null as %s", BIConsts.GROUPING_VALUE);
        }
        selectFragments.add(groupingExpression);

        // 分组key值，用于给到前端渲染计算使用
        selectFragments.add(String.format("%s as %s", groupingValueField, BIConsts.GROUPING_KEY));

        //order by 在最外层处理
//        String orderByClause = this.buildOrderByClause().toString();
//        if(BIUtil.isNotEmpty(orderByClause) && config.getSettings().getSortMode() == SortMode.ROW_NUMBER){
//            selectFragments.add(String.format(String.format("row_number() over(%s) as %s", orderByClause, BIConsts.ROW_NUMBER_KEY)));
//        }

        sql.append(" select ");
        sql.append(BIUtil.listToStr(selectFragments));

        return sql;
    }

    /**
     * 构建列维度表达式
     * @param colFieldExpression
     * @return
     */
    protected String buildSelectColFieldExpression(String colFieldExpression) {

        AnalysisTotalConfig totalConfig = this.config.getAnalysis().getTotal();
        if (!totalConfig.isActive()) {
            return colFieldExpression;
        }

        AnalysisCalcMode calcMode = null;

        String calcModeDisplayName = "";
        AnalysisTotalItemConfig analysisTotalItemConfig = totalConfig.getItem(AnalysisTotalType.ROW_TOTAL);
        if (analysisTotalItemConfig != null) {
            calcMode = AnalysisCalcMode.ROW_TOTAL;
            calcModeDisplayName = BIConsts.ROW_TOTAL_COLUMN_CODE;
        }

        if (calcMode == null) {
            return colFieldExpression;
        }

        BaseOperator operator = OperatorFactory.getOperator(calcMode);
        String expression = "null";
        if (operator != null) {
            List<Integer> groupingValues = operator.getGroupingValues(config);
            String conditionExpression = String.format(" %s in (%s)",this.getFullJoinFieldExpression(BIConsts.GROUPING_VALUE) , BIUtil.listToStr(groupingValues));
            expression = fx.ifExpression(conditionExpression, "'"+calcModeDisplayName+"'", colFieldExpression);
        }
        return expression;
    }

    /**
     * 获取查询指标字段（含列维度扩展后的）
     * @return
     */
    protected List<QueryField> getSelectMeasureFields(){
        List<QueryField> measureFields = new ArrayList<>();
        if(this.isCrossDimensionQuery()) {
            // 列维度：需要将维度项+指标组合后为创建新的指标
            CrossDimensionQueryEngine multiEngine = (CrossDimensionQueryEngine) this.queryEngine;
            List<ResultDataSetColumn> leftColumns = multiEngine.getLeafColumns();
            List<String> rowDimCodes = config.getResult().getRowDimensions().stream().map(QueryField::getCode).collect(Collectors.toList());
            for(ResultDataSetColumn column : leftColumns){
                if(rowDimCodes.contains(column.getCode())) {
                    // 排除行维度字段
                    continue;
                }
                QueryField rawMeasureField = config.getResult().getFieldByCode(column.getRawCode());
                if(rawMeasureField == null) {
                    continue;
                }
                QueryField newMeasureField = rawMeasureField.clone();
                newMeasureField.setId(column.getId());
                newMeasureField.setCode(column.getCode());
                newMeasureField.setTitle(column.getTitle());
                newMeasureField.setRawCode(column.getRawCode());
                if(Enabled.value(newMeasureField.getIsAnalysis()) && newMeasureField.getCode().contains(BIConsts.COLUMN_DIM_FIELD_SUFFIX)) {
                    // 分析字段：需将其分析的原始指标字段修改为组合后的指标code
                    String colItemSuffix = newMeasureField.getCode().split(BIConsts.COLUMN_DIM_FIELD_SUFFIX)[1];
                    String analysisMeasureCode = newMeasureField.getAnalysisConfig().getMeasureCode() + BIConsts.COLUMN_DIM_FIELD_SUFFIX + colItemSuffix;
                    newMeasureField.getAnalysisConfig().setMeasureCode(analysisMeasureCode);
                }
                measureFields.add(newMeasureField);
            }
        }

        if(BIUtil.isEmpty(measureFields)){
            measureFields = config.getResult().getMeasures();
        }

        return measureFields;
    }

    /**
     * from子句：包含同环比
     * @return
     */
    protected StringBuilder buildFromClause(){
        StringBuilder sql = new StringBuilder();

        // 先构建当前期数据集
        AnalysisDataSet currentDatSet = analysisDataSetMap.get(AnalysisCalcMode.NONE.getCode());

        // 获取查询的行维度
        //List<QueryField> rowDimensionFields = config.getResult().getRowDimensions();
        List<QueryField> rowDimensionFields = config.getResult().getRowDimensions().stream().filter(f->!f.isVirtual()).collect(Collectors.toList());

        List<QueryField> colDimensionFields = config.getResult().getColDimensions().stream().filter(f->!f.isAppend()).collect(Collectors.toList());
        if(CollUtil.isNotEmpty(colDimensionFields)){
            rowDimensionFields.addAll(colDimensionFields);
        }

        sql.append(" from ").append(String.format("%s %s ", ds1, currentDatSet.getName()));

        String analysisDateFieldCode = this.getAnalysisDateFieldCode();
        if(BIUtil.isEmpty(analysisDateFieldCode)){
            return sql;
        }

        // 公共日期
        QueryField commonDateField = this.config.getResultCommonDateField();

        List<String> joinFragments = new ArrayList<>();
        Map<String, List<String>> joinDimensionFullFieldNames = new HashMap<>();
        for(String analysisDataSetKey : analysisDataSetMap.keySet()) {

            String calcMode = analysisDataSetKey.contains(AnalysisCalcMode.CUSTOM_COMPARE.getCode()) ? AnalysisCalcMode.CUSTOM_COMPARE.getCode() : analysisDataSetKey;
            if (!AnalysisCalcMode.get(calcMode).isCompare()) {
                continue;
            }
            AnalysisDataSet analysisDataSet = analysisDataSetMap.get(analysisDataSetKey);

            // 条件
            List<String> conditionFragments = new ArrayList<>();

            // 添加日期关联,汇总时不添加
            if (!config.isAggQuery() ) {

                List<String> fullNames = joinDimensionFullFieldNames.containsKey(commonDateField.getCode()) ?  joinDimensionFullFieldNames.get(commonDateField.getCode()) : new ArrayList<>();
                String currentFieldFullName = currentDatSet.getDateFieldFullName();
                if(!fullNames.contains(currentFieldFullName)){
                    fullNames.add(currentFieldFullName);
                }

                String compareFieldFullName = analysisDataSet.getName() + "." + commonDateField.getCode() + BIConsts.COMPARE_DATE_SUFFIX;

                List<String> left = new ArrayList<>();
                left.addAll(fullNames);
                left.add(String.format("'%s'", BIConsts.SSM_ALL));

                List<String> right = new ArrayList<>();
                right.add(compareFieldFullName);
                right.add(String.format("'%s'", BIConsts.SSM_ALL));

                conditionFragments.add(String.format("%s = %s", fx.coalesce(left), fx.coalesce(right)));

                if(!fullNames.contains(compareFieldFullName)){
                    fullNames.add(compareFieldFullName);
                }
                joinDimensionFullFieldNames.put(commonDateField.getCode(), fullNames);



                /** 改为full join后此处废弃
                String currentDateFieldExpression = currentDatSet.getDateFieldExpression();

                if(currentDatSet instanceof CurrentAnalysisDataSet){
                    currentDateFieldExpression = ((CurrentAnalysisDataSet)currentDatSet).getDateFieldExpression(AnalysisCalcMode.get(calcMode));
                }

                //conditionFragments.add(String.format("%s = %s", currentDateFieldExpression, analysisDataSet.getDateFieldExpression()));
                // 改为full join，日期关联调整为特定字段
                String compareDateFieldExpression = fx.coalesce(compareFieldFullName, String.format("'%s'", BIConsts.SSM_ALL)); //analysisDataSet.getDateFieldExpression();
                conditionFragments.add(String.format("%s = %s", currentDateFieldExpression, compareDateFieldExpression));
                 */
            }

            for (QueryField rowField : rowDimensionFields) {

                // 排除分析的日期字段：此字段已添加
                if (rowField.getCode().equalsIgnoreCase(currentDatSet.getDateField())) {
                    continue;
                }

                // 排除同源的其他日期粒度的字段：日期字段只需要按最细粒度关联即可，避免跨年月时无法关联的问题
                if (AnalysisUtil.isAnalysisDateField(rowField, analysisDateFieldCode)) {
                    continue;
                }

                List<String> fullNames = joinDimensionFullFieldNames.containsKey(rowField.getCode()) ? joinDimensionFullFieldNames.get(rowField.getCode()) : new ArrayList<>();
                String currentFieldFullName = currentDatSet.getName() + "." + rowField.getCode();
                if(!fullNames.contains(currentFieldFullName)){
                    fullNames.add(currentFieldFullName);
                }

                String compareFieldFullName = analysisDataSet.getName() + "." + rowField.getCode();

                List<String> left = new ArrayList<>();
                left.addAll(fullNames);
                left.add(String.format("'%s'", BIConsts.SSM_ALL));

                List<String> right = new ArrayList<>();
                right.add(compareFieldFullName);
                right.add(String.format("'%s'", BIConsts.SSM_ALL));

                conditionFragments.add(String.format("%s = %s", fx.coalesce(left), fx.coalesce(right)));

                if(!fullNames.contains(compareFieldFullName)){
                    fullNames.add(compareFieldFullName);
                }
                joinDimensionFullFieldNames.put(rowField.getCode(), fullNames);

                /** 改为full join后此处废弃
                String currentExpression = fx.coalesce(currentDatSet.getName() + "." + rowField.getCode(), "'" + BIConsts.SSM_ALL + "'");
                String analysisExpression = fx.coalesce(analysisDataSet.getName() + "." + rowField.getCode(), "'" + BIConsts.SSM_ALL + "'");
                conditionFragments.add(String.format("%s = %s ", currentExpression, analysisExpression));
                 */
            }

            //没有一个行维度处理。场景：自定义对比汇总，不选任何行维度
            if (CollUtil.isEmpty(conditionFragments)) {
                conditionFragments.add(" 1=1 ");
            }

            String dsKey = getWithClauseKey(analysisDataSetKey);
            joinFragments.add(String.format("full join %s %s on %s", dsKey, analysisDataSet.getName(), BIUtil.listToStr(conditionFragments, " and ")));
        }

        sql.append(BIUtil.listToStr(joinFragments, " "));

        return sql;
    }

    protected StringBuilder buildWhereClause() {
        StringBuilder sql = new StringBuilder();
        List<String> whereSegments = new ArrayList<>();

        Long rowDimCount = config.getResult().getFields().stream().filter(f -> f.getRawQueryArea() == QueryArea.RowDimension).count();
        Long colDimCount = config.getResult().getFields().stream().filter(f -> f.getRawQueryArea() == QueryArea.ColumnDimension).count();
        // 过滤掉整表总计的最后一行
        AnalysisTotalItemConfig itemConfig = config.getAnalysis().getTotal().getItem(AnalysisTotalType.WHOLE_TABLE);
        if (rowDimCount > 0 && colDimCount > 0 && itemConfig != null) {
            int groupingValue = NumberUtil.binaryToInt(StringUtils.rightPad("", new Long(rowDimCount + colDimCount).intValue(), "1"));
            whereSegments.add(String.format("%s.%s not in(%s)", currentDataSet.getName(), BIConsts.GROUPING_VALUE, groupingValue));
        }

        // 若有自定义对比，确保日期是查询日期，需要去掉当前期为空的数据
        boolean hasCustomCompare = false;
        for (AnalysisDataSet ds : this.analysisDataSetMap.values()) {
            if (ds.getCalcMode() == AnalysisCalcMode.CUSTOM_COMPARE) {
                hasCustomCompare = true;
                break;
            }
        }
        if (hasCustomCompare && !config.getSettings().isQueryAllDate() && !config.isRtDatasetChartQuery()) {
            QueryField commonDateField = this.config.getFilterCommonDateField();
            // 日期聚合（不查询）、或在列维度时不处理
            if (!commonDateField.isAggQuery() && commonDateField.getRawQueryArea() != QueryArea.ColumnDimension) {
                // TODO 自定义对比时长 > 基准时长时，需要去掉full join多出来的自定义对比日期同时需要保留小计、总计信息
                String dateExpression = this.getFullJoinFieldExpression(commonDateField);
                String startDate = commonDateField.getValues().get(0).getId();
                String endDate = commonDateField.getValues().get(1).getId();
                String dateWhereFragment = String.format("(%s between '%s' and '%s' or %s is null)", dateExpression, startDate, endDate, dateExpression);
                whereSegments.add(dateWhereFragment);
            }
            //whereSegments.add(String.format("%s.%s is not null", currentDataSet.getName(), BIConsts.GROUPING_VALUE));
        }

        //结果过滤
        List<QueryField> filterFields = config.getFilter().getFields();
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

            values.forEach(fv -> {
                fv.setId(EnvVariableManager.value(fv.getId()));
            });
            // 字段数据类型
            FieldDataType dataType = FieldDataType.getType(field.getMeta().getDataType());
            if (values.isEmpty()) {
                continue;
            }

            String whereFieldName = String.format("%s.%s", currentDataSet.getName(), field.getCode());
            //计算指标的表达式从映射关系中获取
            if (FieldType.CUSTOM_MEASURE == FieldType.get(field.getFieldType())) {
                String expression = customMeasureCodeExpressionMap.get(field.getCode());
                if (StrUtil.isEmpty(expression)) {
                    continue;
                }

                whereFieldName = String.format("(%s)", expression);
            }

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
                        whereSegments.add(String.format(" ( %s between %s and %s )  or  ( %s != 0 ) ", whereFieldName, v1, v2, grpValueExpression));
                    } else {
                        whereSegments.add(String.format("%s between %s and %s", whereFieldName, v1, v2));
                    }
                }
            }

        }

        if (BIUtil.isNotEmpty(whereSegments)) {
            sql.append(" where ").append(BIUtil.listToStr(whereSegments, " AND ", "(", ")"));
        }

        return sql;
    }

    protected List<String> buildOrderByFragments() {
        AnalysisTotalType totalType = AnalysisUtil.getOrderByTotalType(config);

        List<String> orderByFragments = new ArrayList<String>();
        List<QueryField> orderByFields = new ArrayList<>();

        List<String> resultFieldCodes = new ArrayList<>();
        resultFieldCodes.addAll(config.getResult().getFields().stream()
                .filter(f->Enabled.value(f.getIsShow())&&!f.isAppend())
                .map(QueryField::getCode).collect(Collectors.toList()));

        orderByFragments.addAll(FieldUtil.getOrderByFragments(config,resultFieldCodes));
        List<String> sortFieldCodes = this.config.getSettings().getQuerySortFieldCodes(this.config);

        // 获取排序字段
        List<QueryField> resultFields = config.getResult().getFields();
        if (totalType.isActive()) {
            TotalOperator totalOperator = OperatorFactory.getTotalOperator(totalType);
            orderByFields = totalOperator.orderByFields(config);
        } else {
            orderByFields = resultFields.stream().filter(f -> f.getSortType() != FieldSortType.NONE && f.getRawQueryArea() != QueryArea.ColumnDimension).collect(Collectors.toList());
        }

        // 是否有grouping value的排序字段
        boolean hasGroupValueField = false;
        for (QueryField field : orderByFields) {
            FieldSortType sortType = field.getSortType();
            if (sortType == FieldSortType.NONE) {
                sortType = FieldSortType.ASC;
            }

            //已排序，不再处理
            if(sortFieldCodes.contains(field.getCode())){
                continue;
            }

            String orderByExpression = "";

            if (Enabled.value(field.getIsAnalysis())) {
                // 对分析字段排序：分析字段为二次计算字段，故无所属数据集名
                orderByExpression = FieldUtil.getFieldValueSortExpression(field.getCode(), field);
                orderByExpression = String.format("%s %s nulls first", orderByExpression, sortType.getCode());
            } else {
                if (config.getSettings().getSortMode() == SortMode.ROW_NUMBER) {
                    // row_number排序，需要字段别名
                    orderByExpression = this.getFullJoinFieldExpression(field);
                    orderByExpression = FieldUtil.getFieldValueSortExpression(orderByExpression, field);
                } else {
                    orderByExpression = FieldUtil.getFieldValueSortExpression(field.getCode(), field);
                }
                orderByExpression = String.format("%s %s nulls first", orderByExpression, sortType.getCode());
            }

            orderByFragments.add(orderByExpression);

            hasGroupValueField = hasGroupValueField || field.getCode().equals(BIConsts.GROUPING_VALUE);
        }

        // 设置行总计排到最前面
        if (hasGroupValueField) {
            String groupingValueField = "";
            if (config.getSettings().getSortMode() == SortMode.ROW_NUMBER) {
                groupingValueField = this.getFullJoinFieldExpression(BIConsts.GROUPING_VALUE);
            } else {
                groupingValueField = BIConsts.GROUPING_KEY;
            }
            String colTotalOrderByFragment = String.format("if(%s=%s, 1, 0) desc nulls last", groupingValueField, colTotalGroupingValue);
            if (this.isCrossDimensionQuery()) {
                colTotalOrderByFragment = String.format("if(%s in (%s,%s), 1, 0) desc nulls last", groupingValueField, colTotalGroupingValue, colTotalGroupingValue + 1);
            }

            orderByFragments.add(0, colTotalOrderByFragment);
        }

        return orderByFragments;
    }

    protected StringBuilder buildOrderByClause(){
        StringBuilder orderBySQL = new StringBuilder();

        if(!config.getSettings().getNeedSort()){
            return orderBySQL;
        }

        List<String> orderByFragments = this.buildOrderByFragments();
        if (orderByFragments.isEmpty()) {
            return orderBySQL;
        }
        orderBySQL.append(" ").append(BIConsts.ORDER_BY).append(" ");
        orderBySQL.append(BIUtil.listToStr(orderByFragments, ","));
        return orderBySQL;
    }

    /**
     * 构建分析数据集
     * @return map<计算方式, 分析数据集>
     */
    protected Map<String, AnalysisDataSet> buildAnalysisDataSet() {
        Map<String, AnalysisDataSet> analysisDataSetMap = new LinkedHashMap<>();

        List<QueryField> analysisFields = this.getThbFields();

        // 先添加当前数据集
        String dateField = this.getAnalysisDateFieldCode();

        analysisDataSetMap.put(AnalysisCalcMode.NONE.getCode(), new CurrentAnalysisDataSet(dateField, AnalysisCalcMode.NONE));

        DateGranularity dateGranularity = this.getAnalysisDateGranularity();
        if (dateGranularity == null || BIUtil.isEmpty(analysisFields) || StringUtil.isEmpty(dateField)) {
            return analysisDataSetMap;
        }
        // 再添加同环比数据集
        for (QueryField qf : analysisFields) {
            AnalysisItemConfig cfg = qf.getAnalysisConfig();
            AnalysisCalcMode calcMode = cfg.getRawThbCalcMode();

            String analysisDataSetKey = buildAnalysisDataSetKey(cfg);
            if (analysisDataSetMap.containsKey(analysisDataSetKey)) {
                // 避免重复添加
                continue;
            }

            if (qf.isTargetValue()) {
                continue;
            }

            //自定义对比
            if (AnalysisCalcMode.CUSTOM_COMPARE == calcMode) {
                AnalysisDataSet dataSet = new CustomCompareAnalysisDataSet(cfg.getDateFieldCode(), calcMode, cfg);
                analysisDataSetMap.put(analysisDataSetKey, dataSet);
                continue;
            }

            AnalysisDataSet dataSet = null;
            switch (dateGranularity) {
                case YEAR:
                    dataSet = new YearAnalysisDataSet(cfg.getDateFieldCode(), calcMode);
                    break;
                case MONTH:
                    dataSet = new MonthAnalysisDataSet(cfg.getDateFieldCode(), calcMode);
                    break;
                case QUARTER:
                    dataSet = new QuarterAnalysisDataSet(cfg.getDateFieldCode(), calcMode);
                    break;
                case WEEK:
                    dataSet = new WeekAnalysisDataSet(cfg.getDateFieldCode(), calcMode);
                    break;
                case DAY:
                    dataSet = new DayAnalysisDataSet(cfg.getDateFieldCode(), calcMode);
                    dataSet.setConfig(this.config);
                default:
                    break;
            }

            analysisDataSetMap.put(analysisDataSetKey, dataSet);
        }

        return analysisDataSetMap;
    }

    /**
     * 构建分析数据集的key
     */
    public String buildAnalysisDataSetKey(AnalysisItemConfig cfg) {
        AnalysisCalcMode analysisCalcMode = cfg.getRawThbCalcMode();

        String key = analysisCalcMode.getCode();;
        // 自定义对比存在多段时间范围，需要添加索引数值后缀
        if (AnalysisCalcMode.CUSTOM_COMPARE == analysisCalcMode) {
            key = String.format("%s_%s", analysisCalcMode.getCode(), cfg.getCompareIndex());
        } else if (analysisCalcMode.isTarget()) {
            key = AnalysisCalcMode.NONE.getCode();
        }

        return key;
    }

    protected List<QueryField> getThbFields() {
        List<QueryField> analysisFields = config.getResult().getFields().stream()
                .filter(f -> Enabled.value(f.getIsAnalysis())
                        &&
                        (
                                AnalysisCalcMode.get(f.getAnalysisConfig().getCalcMode()).isCompare()
                                        ||
                                        //兼容只配置了贡献率的场景
                                        AnalysisCalcMode.CONTRIBUTION_RATE == AnalysisCalcMode.get(f.getAnalysisConfig().getCalcMode())
                                        ||
                                        AnalysisCalcMode.ZB_THB == AnalysisCalcMode.get(f.getAnalysisConfig().getCalcMode())
                        )
                )
                .collect(Collectors.toList());
        ;
        return analysisFields;
    }

    /**
     * 获取分析的日期字段
     * @return
     */
    protected String getAnalysisDateFieldCode() {

        List<QueryField> analysisFields = this.getThbFields();
        for (QueryField queryField : analysisFields) {
            String dateFieldCode = queryField.getAnalysisConfig().getDateFieldCode();
            if (BIUtil.isNotEmpty(dateFieldCode)) {
                if(config.getResult().getFieldByCode(dateFieldCode) != null || config.isAggQuery()){
                    return dateFieldCode;
                }
            }
        }
        return "";
    }

    /**
     * 获取分析的日期粒度
     * @return
     */
    protected DateGranularity getAnalysisDateGranularity(){
        List<QueryField> analysisFields = this.getThbFields();
        if(BIUtil.isEmpty(analysisFields)){
            return null;
        }
        QueryField analysisField = analysisFields.get(0);
        DateGranularity dateGranularity = DateGranularity.get(analysisField.getAnalysisConfig().getDateGranularity());
        return dateGranularity;
    }



    protected boolean isCrossDimensionQuery(){
        return this.queryEngine instanceof CrossDimensionQueryEngine;
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

    protected AnalysisSingleModelSqlBuilder createAnalysisSingleModeSqlBuilder(QueryConfigure cfg, QueryContext _cxt){
        AnalysisSingleModelSqlBuilder singleModelSqlBuilder = new AnalysisSingleModelSqlBuilder(cfg, _cxt);
        return singleModelSqlBuilder;
    }

    protected AnalysisSingleModelSqlBuilderAvgWrapper createAnalysisSingleModeSqlBuilderAvgWrapper(SingleModelSqlBuilder singleModelSqlBuilder){
        AnalysisSingleModelSqlBuilderAvgWrapper singleModelSqlBuilderAvgWrapper = new AnalysisSingleModelSqlBuilderAvgWrapper(singleModelSqlBuilder);
        return singleModelSqlBuilderAvgWrapper;
    }

    protected String getFullJoinFieldExpression(QueryField field){
        List<String> coalesceFieldNames = new ArrayList<>();
        for(AnalysisDataSet ds : this.analysisDataSetMap.values()){
            String dsName = ds.getName();
            String fieldCode = field.getCode();
            if(field.isCommonDate()) {
                if (!dsName.equals(currentDataSet.getName())) {
                    // 不是当前数据集的公共日期需要用对比日期
                    fieldCode = fieldCode + BIConsts.COMPARE_DATE_SUFFIX;
                }
            }
            coalesceFieldNames.add(String.format("%s.%s", dsName, fieldCode));
        }

        String expression = fx.coalesce(coalesceFieldNames);
        return expression;
    }

    protected String getFullJoinFieldExpression(String fieldCode){
        List<String> coalesceFieldNames = new ArrayList<>();
        this.analysisDataSetMap.values().stream().forEach(ds->{coalesceFieldNames.add(ds.getName() + "." + fieldCode);});
        String expression = fx.coalesce(coalesceFieldNames);
        return expression;
    }
}
