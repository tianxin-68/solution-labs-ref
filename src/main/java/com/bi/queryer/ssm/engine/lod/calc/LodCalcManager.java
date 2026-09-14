package com.bi.queryer.ssm.engine.lod.calc;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.custom.CustomFieldConfigure;
import com.bi.queryer.ssm.custom.CustomFieldParser;
import com.bi.queryer.ssm.custom.CustomFieldType;
import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.analysis.cfg.AnalysisItemConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.compare.AnalysisCompareConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.compare.AnalysisCompareItemConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.ctr.AnalysisContributionRateItemConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.thb.AnalysisThbCalcSettingConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.thb.AnalysisThbConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.thb.AnalysisThbItemConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.thb.AnalysisZbThbItemConfig;
import com.bi.queryer.ssm.engine.analysis.operator.BaseOperator;
import com.bi.queryer.ssm.engine.analysis.operator.OperatorContext;
import com.bi.queryer.ssm.engine.analysis.operator.OperatorFactory;
import com.bi.queryer.ssm.engine.analysis.operator.impl.ContributionRateOperator;
import com.bi.queryer.ssm.engine.analysis.operator.impl.ZbThbOperator;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.function.FunctionManager;
import com.bi.queryer.ssm.engine.function.IFunction;
import com.bi.queryer.ssm.engine.lod.LodConsts;
import com.bi.queryer.ssm.engine.lod.LodQueryConfigure;
import com.bi.queryer.ssm.engine.lod.LodQueryConfigureItem;
import com.bi.queryer.ssm.engine.result.ResultDataSetColumn;
import com.bi.queryer.ssm.enums.*;
import com.bi.queryer.ssm.exception.SSDException;
import com.bi.queryer.ssm.util.FieldUtil;
import com.bi.queryer.ssm.util.AnalysisTotalUtil;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 10:34 2024-06-18
 * @Description
 * lod二次四则计算字段管理类
 * 支持同环对比，示例：uv价值 = gmv/lod_uv
 * 计算逻辑：uv价值：本期值=gmv/lod_uv，环比率：(gmv/lod_uv) / (上期gmv/上期lod_uv) - 1，环比实际值=上期gmv/上期lod_uv
 * 处理逻辑：因为四则运算在最外层sql构建，此时四则运算所需的原子字段在内层sql中不一定存在（如上面示例的上期值），故需要把不存在的字段先强制带出来，查询结果中再去掉
 *
 **/
public class LodCalcManager {
    protected QueryConfigure rawQueryConfigure = null;
    protected LodQueryConfigureItem mainItem = null;
    protected List<QueryField> lodCalcFields = null;

    /**
     * lod计算字段与原子字段的关系 key=lod计算字段id， value=关系
     */
    protected Map<String, LodCalcAtomQueryFieldRelation> lodCalcRelations = new HashMap<>();

    /**
     * 原子字段所关联的lod计算字段列表，key=原子字段id，value=关系
     */
    protected Map<String, AtomQueryFieldLodCalcRelation> atomRelations = new HashMap<>();

    /**
     * lod计算字段的同环对比配置关系
     */
    protected Map<String, LodCalcAnalysisItemConfigRelation> lodCalcAnalysisRelations = new HashMap<>();

    /**
     * lod二次计算的分析字段
     */
    protected Map<String, List<QueryField>> lodCalcAnalysisFields = new HashMap<>();

    /**
     * 记录查询字段副本，用于修改查询字段属性后还原
     */
    protected Map<String, QueryField> fieldCopies = new HashMap<>();

    protected IFunction fx = null;

    public LodCalcManager(QueryConfigure rawQueryConfigure, List<QueryField> lodFields){
        this.rawQueryConfigure = rawQueryConfigure;
        this.lodCalcFields = lodFields.stream()
                .filter(f-> CustomFieldType.get(f.getCustomFieldConfigure().getType()) == CustomFieldType.LOD_CALC)
                .collect(Collectors.toList());
        fx = FunctionManager.getFunction();
        this.init();
    }

    protected void init(){
        if(BIUtil.isEmpty(lodCalcFields)){
            return;
        }

        // 构建lod计算字段分析配置关系
        this.buildLodCalcAnalysisRelations(lodCalcFields);

        // 构建lod计算字段与原子字段的关系
        this.buildLodCalcAtomRelations(lodCalcFields);
    }

    /**
     * 构建lod计算字段与分析配置的关系
     */
    protected void buildLodCalcAnalysisRelations(List<QueryField> lodCalcFields){
        for(QueryField lodCalcField : lodCalcFields){
            // 同环比
            AnalysisThbItemConfig lodCalcThbItemConfig = null;
            AnalysisThbConfig thbConfig = this.rawQueryConfigure.getAnalysis().getThb();
            if(thbConfig != null && thbConfig.isActive()){
                List<AnalysisThbItemConfig> thbItemConfigs = thbConfig.getItems();
                for(AnalysisThbItemConfig itemConfig : thbItemConfigs){
                    List<String> measureIdList = itemConfig.getMeasureIdList();
                    if(BIUtil.isEmpty(measureIdList)){
                        continue;
                    }
                    if(measureIdList.contains(BIConsts.ALL_MEASURE) || measureIdList.contains(lodCalcField.getId())){
                        lodCalcThbItemConfig = itemConfig;
                        break;
                    }
                }
            }

            // 自定义对比
            AnalysisCompareItemConfig lodCalcCompareItemConfig = null;
            AnalysisCompareConfig compareConfig = this.rawQueryConfigure.getAnalysis().getCompare();
            if(compareConfig != null && compareConfig.isActive()){
                List<AnalysisCompareItemConfig> analysisCompareItemConfigs = compareConfig.getItems();
                for(AnalysisCompareItemConfig itemConfig : analysisCompareItemConfigs){
                    List<String> measureIdList = itemConfig.getMeasureIdList();
                    if(BIUtil.isEmpty(measureIdList)){
                        continue;
                    }
                    if(measureIdList.contains(BIConsts.ALL_MEASURE) || measureIdList.contains(lodCalcField.getId())){
                        lodCalcCompareItemConfig = itemConfig;
                        break;
                    }
                }
            }

            if(lodCalcThbItemConfig != null || lodCalcCompareItemConfig != null){
                LodCalcAnalysisItemConfigRelation itemConfigRelation = new LodCalcAnalysisItemConfigRelation(lodCalcField, lodCalcThbItemConfig, lodCalcCompareItemConfig);
                this.lodCalcAnalysisRelations.put(lodCalcField.getId(), itemConfigRelation);
            }
        }
    }

    /**
     * 构建lod计算字段与原子字段的关系
     * @param calcLodFields
     */
    protected void buildLodCalcAtomRelations(List<QueryField> calcLodFields){
        for(QueryField calcLodField : calcLodFields){
            /**
             * // 去掉此判断：避免无分析场景时，lod与主视图的四则运算sql报错的bug
             * // 场景：lod1/支付GMV，但支付GMV被删除，此时sql构建报错
            if(!hasAnalysis(calcLodField)){
                continue;
            }
            */
            LodCalcAtomQueryFieldRelation calcFieldRelation = new LodCalcAtomQueryFieldRelation(calcLodField);
            String expression = calcLodField.getCustomFieldConfigure().getExpression();
            Pattern pattern = Pattern.compile(CustomFieldParser.expressionPattern);
            Matcher matcher = pattern.matcher(expression);
            while (matcher.find()){
                String fieldId = matcher.group();
                QueryField calcAtomField = rawQueryConfigure.getResult().getFieldById(fieldId);
                if(calcAtomField != null){
                    if(calcAtomField.isLodField()){
                        calcFieldRelation.addLodAtomQueryField(calcAtomField);
                    }else {
                        calcFieldRelation.addMainAtomQueryField(calcAtomField);
                    }
                    AtomQueryFieldLodCalcRelation atomRelation = this.atomRelations.get(calcAtomField.getId());
                    if(atomRelation == null){
                        atomRelation = new AtomQueryFieldLodCalcRelation(calcAtomField);
                    }
                    atomRelation.addLodCalc(calcLodField);
                    this.atomRelations.put(calcAtomField.getId(), atomRelation);
                }
            }

            this.lodCalcRelations.put(calcLodField.getId(), calcFieldRelation);
        }
    }

    /**
     * lod二次计算字段是否对比分析
     * @param lodCalcField
     * @return
     */
    protected boolean hasAnalysis(QueryField lodCalcField){
        LodCalcAnalysisItemConfigRelation relation = this.lodCalcAnalysisRelations.get(lodCalcField.getId());
        if(relation == null){
            return false;
        }
        return relation.hasAnaysisConfig();
    }

    /**
     * 设置主/子视图需要附加的同环比配置
     * @return
     */
    public boolean setLodCalcAnalysisConfigs(QueryConfigure queryConfigure, boolean isMainConfig){
        if(BIUtil.isEmpty(lodCalcRelations)){
            return false;
        }

        // 所有主视图参与计算的原子字段
        List<QueryField> atomQueryFields = new ArrayList<>();
        for (LodCalcAtomQueryFieldRelation relation : this.lodCalcRelations.values()) {
            if(isMainConfig) {
                atomQueryFields.addAll(relation.getMainAtomQueryFields());
            }else {
                atomQueryFields.addAll(relation.getLodAtomQueryFields());
            }
        }

        atomQueryFields = atomQueryFields.stream().distinct().collect(Collectors.toList());

        boolean isModified = false;

        // 设置原子字段均可见，先查询后对结果隐藏
        for(QueryField f : atomQueryFields){
            if(fieldCopies.containsKey(f.getCode())){
                continue;
            }
            QueryField measure = queryConfigure.getResult().getFieldByCode(f.getCode());
            if(measure == null){
                continue;
            }
            fieldCopies.put(f.getCode(), f.clone());
            if(Enabled.isFalse(f.getIsShow())) {
                f.setIsShow(Enabled.YES.getId());
                measure.setIsShow(Enabled.YES.getId());
                isModified = true;
            }

            // 二次计算字段仅为附加字段时，需要添加到指标区域，便于后续计算同环比
            if(measure.isAppend()){
                measure.setAppend(false);
                measure.setIsShow(Enabled.YES.getId());
                queryConfigure.getResult().add(measure, QueryArea.Measure);
                isModified = true;
            }
        }

        isModified = this.setLodCalcAnalysisConfigs(queryConfigure, atomQueryFields) || isModified;

        return isModified;
    }

    /**
     * 主视图附加分析指标
     * 逻辑：
     * - 1、需要同环对比计算原子字段是否配置了同环比
     * - 1.1 若未配置，则需要添加同环对比配置（含实际值）
     * - 1.2 若配置了，再判断2
     * - 2、再判断是否配置了查询其同环对比的真实值
     * - 2.1 若未配置，则需添加查询其同环对比的真实值
     * - 2.2 若配置了，不处理
     */
    protected boolean setLodCalcAnalysisConfigs(QueryConfigure queryConfig, List<QueryField> atomQueryFields){
        if(BIUtil.isEmpty(lodCalcRelations)){
            return false;
        }

        // 同环比
        AnalysisThbConfig thbConfig = queryConfig.getAnalysis().getThb();
        /**
        if(thbConfig == null || !thbConfig.isActive()){
            return false;
        }
         */
        boolean result = false;
        if(thbConfig != null && thbConfig.isActive()) {
            List<AnalysisThbItemConfig> thbItemConfigs = thbConfig.getItems();
            List<AnalysisThbItemConfig> appendThbItemConfigs = new ArrayList<>();
            for (QueryField atomQueryField : atomQueryFields) {
                for (AnalysisThbItemConfig itemConfig : thbItemConfigs) {
                    List<String> measureIdList = itemConfig.getMeasureIdList();
                    if (BIUtil.isEmpty(measureIdList)) {
                        continue;
                    }
                    // lod计算字段配置了同环比，但原子字段不在分析配置中，则添加其相关配置
                    if (!measureIdList.contains(BIConsts.ALL_MEASURE) && !measureIdList.contains(atomQueryField.getId())) {
                        AnalysisThbItemConfig thbItemConfig = new AnalysisThbItemConfig();
                        thbItemConfig.getMeasureIdList().add(atomQueryField.getId());
                        thbItemConfig.getCalcTypes().add(AnalysisCalcType.REAL_VALUE.getCode());
                        // 计算方式从lod计算字段的计算方式获取
                        thbItemConfig.setCalcModes(this.getAnalysisThbCalcModes(atomQueryField));
                        appendThbItemConfigs.add(thbItemConfig);
                        result = true;
                    }
                    if (!itemConfig.getCalcTypes().contains(AnalysisCalcType.REAL_VALUE.getCode())) {
                        // lod计算字段配置了同环比，原子字段也配置了同环比，但计算类型没有实际值，则添加其相关配置
                        itemConfig.getCalcTypes().add(AnalysisCalcType.REAL_VALUE.getCode());

                        result = true;
                    }
                }
            }

            // 添加同环比配置
            if (BIUtil.isNotEmpty(appendThbItemConfigs)) {
                queryConfig.getAnalysis().getThb().getItems().addAll(appendThbItemConfigs);
            }
        }

        // 自定义对比：默认自定义对比配置应用于所有指标
        AnalysisCompareConfig compareConfig = queryConfig.getAnalysis().getCompare(); //this.rawQueryConfigure.getAnalysis().getCompare();
        if(compareConfig == null || !compareConfig.isActive()){
            return result;
        }
        List<AnalysisCompareItemConfig> compareItemConfigs = compareConfig.getItems();
        for (AnalysisCompareItemConfig itemConfig : compareItemConfigs) {
            List<String> measureIdList = itemConfig.getMeasureIdList();
            if (BIUtil.isEmpty(measureIdList)) {
                continue;
            }
           if(!itemConfig.getCalcTypes().contains(AnalysisCalcType.REAL_VALUE.getCode())){
                // lod计算字段配置了同环比，原子字段也配置了同环比，但计算类型没有实际值，则添加其相关配置
                itemConfig.getCalcTypes().add(AnalysisCalcType.REAL_VALUE.getCode());
               result = true;
            }
        }

        return result;
    }

    /**
     * 获取同环比设置配置
     * @param atomField
     * @return
     */
    protected List<String> getAnalysisThbCalcModes(QueryField atomField){
        List<String> calcModes = new ArrayList<>();

        AtomQueryFieldLodCalcRelation relation = this.atomRelations.get(atomField.getId());
        if(relation == null){
            return calcModes;
        }

        List<QueryField> calcLodFields = relation.getLodCalcFields();
        calcLodFields = calcLodFields.stream().distinct().collect(Collectors.toList());

        if(BIUtil.isEmpty(calcLodFields)){
            return calcModes;
        }

        AnalysisThbCalcSettingConfig atomFieldSettingConfig = new AnalysisThbCalcSettingConfig();
        for(QueryField lodCalcField : calcLodFields){
            LodCalcAnalysisItemConfigRelation analysisItemConfigRelation = this.lodCalcAnalysisRelations.get(lodCalcField.getId());
            if(analysisItemConfigRelation == null){
                continue;
            }
            AnalysisThbItemConfig thbItemConfig = analysisItemConfigRelation.getThbItemConfig();
            if(thbItemConfig == null){
                continue;
            }

            if(CollUtil.isEmpty(thbItemConfig.getCalcModes())){
                continue;
            }

            calcModes.addAll(thbItemConfig.getCalcModes());
        }
        calcModes = calcModes.stream().distinct().collect(Collectors.toList());

        return calcModes;
    }

    /**
     * 将lod二次计算字段转换可的查询表达式
     * @return
     */
    public List<String> convertQueryExpression(LodQueryConfigure lodQueryConfigure, Map<String, String> expressionMap, String groupKey){
        List<String> result = new ArrayList<>();
        Map<String, String> notExistFieldTitles = new LinkedHashMap<>();

        if(BIUtil.isEmpty(this.lodCalcFields)){
            return result;
        }

        // 获取分组值和partition by字段
        Function<String, String> fieldSqlSupplier = field -> FieldUtil.getTableFiledExpression(Collections.singletonList(BIConsts.MAIN_TABLE_ALIAS), null, field);
        Map<Integer, List<String>> groupingPartitionMap = AnalysisTotalUtil.getGroupingPartitionMap(lodQueryConfigure.getRawConfig(), fieldSqlSupplier);

        Map<String, String> lodCalcFieldQueryExpressions = new LinkedHashMap<>();
        for(QueryField lodCalcMeasure : this.lodCalcFields){
            CustomFieldConfigure customCfg = lodCalcMeasure.getCustomFieldConfigure();
            String expression = lodCalcMeasure.getCustomFieldConfigure().getExpression();
            if(BIUtil.isEmpty(expression)){
                continue;
            }

            //20260402兼容用户输入表达式前后没有空格，导致查询报错的问题
            //例子：(case when [65bc46563dbf43e7b654c989aa121112_avg_by_d]= 0 then [11a42db1bb5e4f4493adee093e1d564a_avg_by_d]else [65bc46563dbf43e7b654c989aa121112_avg_by_d]end)
            if(BIUtil.isNotEmpty(expression)){
                expression = expression.replaceAll("\\["," \\[");
                expression = expression.replaceAll("\\]","\\] ");
            }

            boolean isDefaultAgg = AnalysisTotalAggType.isDefault(lodCalcMeasure.getTotalAggType());
            Pattern pattern = Pattern.compile(CustomFieldParser.expressionPattern);
            Matcher matcher = pattern.matcher(expression);
            String expressionTips = expression; // 用于将表达式还原为中文便于错误提示，和前端显示一致
            while (matcher.find()){
                String fieldId = matcher.group();
                QueryField calcAtomField = lodQueryConfigure.getRawConfig().getResult().getFieldById(fieldId);
                if(calcAtomField == null && !StrUtil.contains(fieldId, CustomFieldType.ANALYSIS.getIdentifier())){
                    expressionTips = expressionTips.replaceAll("\\[" + fieldId + "\\]", customCfg.getMappingTitle(fieldId));
                    String refTitle = customCfg.getMappingTitle(fieldId);
                    notExistFieldTitles.put(lodCalcMeasure.getTitle(), refTitle);
                }

                Optional<LodQueryConfigureItem> item = lodQueryConfigure.getItems().stream().filter(i -> i.getConfig().getResult().getMeasures().contains(calcAtomField)).findAny();
                if(item.isPresent()) {
                    //若表达式中包含“/”运算符，且不包含“*1.0”,则需将度量字段都乘于1.0000,避免查询结果丢失精确度
                    Boolean hasDivisionWithoutDecimal = expression.indexOf("/") > -1 && expression.replaceAll("\\s*", "").indexOf("*1.0") < 0;
                    String replacement = "";
                    if(hasDivisionWithoutDecimal) {
                        replacement = String.format("(%s.[%s] * %s)", item.get().getCode(), calcAtomField.getCode(), BIConsts.INT_TO_DOUBLE_PRECISION);
                    }else {
                        replacement = String.format("%s.[%s] ", item.get().getCode(), calcAtomField.getCode());
                    }
                    expression = expression.replaceAll("\\[" + fieldId + "\\]", replacement);
                }
            }

            String commonDateFieldName = String.format("%s.dt", LodConsts.CONFIG_CODE_MAIN);
            expression = FieldUtil.rectifytCustomCalcExpression(expression,lodQueryConfigure.getRawConfig(),commonDateFieldName);

            // // 权限不在sql层处理，在最后结果输出时做掩码，避免后续sql构建时数据类型不一致导致sql查询错误
            expression = fx.tryCatch(expression);
//            expression = String.format("%s as %s", expression, lodCalcMeasure.getCode());

            //if (StringUtils.isNotEmpty(lodCalcMeasure.getTotalAggType())) {
            if (!isDefaultAgg) {
                // lod计算表达式 处理汇总指标的自定义聚合方式
                expression = AnalysisTotalUtil.buildTotalMeasureAggExpression(lodCalcMeasure, expression, groupingPartitionMap, fieldSqlSupplier, groupKey);
            }

            lodCalcFieldQueryExpressions.put(lodCalcMeasure.getCode(), expression);
        }

        // 不存在则抛异常
        if(BIUtil.isNotEmpty(notExistFieldTitles)){
            List<String> errorMsgList = new ArrayList<>();
            for(String lodTitle : notExistFieldTitles.keySet()){
                errorMsgList.add(String.format("[%s]引用的字段[%s]不存在", lodTitle, notExistFieldTitles.get(lodTitle)));
            }
            throw new SSDException(BIUtil.listToStr(errorMsgList) );
        }

        // 创建lod二次计算字段的同环对比分析字段表达式
        lodCalcFieldQueryExpressions = this.createAnalysisQueryExpression(lodCalcFieldQueryExpressions, lodQueryConfigure, expressionMap);

        // 返回结果
        lodCalcFieldQueryExpressions.entrySet().forEach(e->{
            // 去掉占位符
            String newExpression = e.getValue().replaceAll("\\[","").replaceAll("\\]", "");
            result.add(String.format("%s as %s", newExpression, e.getKey()));
        });

        return result;
    }

    /**
     * 创建lod二次计算的同环对比分析表达式
     * @param lodCalcFieldQueryExpressions lod二次计算的查询表达式
     * @return
     */
    public Map<String, String> createAnalysisQueryExpression(Map<String, String> lodCalcFieldQueryExpressions, LodQueryConfigure lodQueryConfigure, Map<String, String> expressionMap){
        // 转换lod二次计算的同环对比指标
        List<QueryField> allAnalysisMeasures = this.rawQueryConfigure.getResult().getMeasures().stream().filter(f-> Enabled.isTrue(f.getIsAnalysis())).collect(Collectors.toList());
        Map<String, QueryField> allMeasures = this.rawQueryConfigure.getResult().getMeasures().stream().collect(Collectors.toMap(QueryField::getId, f->f, (f1, f2)->f1));

        // 添加同环对比分析字段
        for(QueryField lodCalcField : this.lodCalcFields) {
            boolean isDefaultAgg = AnalysisTotalAggType.isDefault(lodCalcField.getTotalAggType());
            String lodCalcFieldQueryExpression = lodCalcFieldQueryExpressions.get(lodCalcField.getCode());
            if (BIUtil.isEmpty(lodCalcFieldQueryExpression)) {
                continue;
            }
            List<QueryField> analysisFields = new ArrayList<>();
            for (QueryField analysisField : allAnalysisMeasures) {
                if (lodCalcField.getId().equals(analysisField.getAnalysisConfig().getMeasureId())) {
                    analysisFields.add(analysisField);
                }
            }

            // 当期
            String currentRealValueExpression;
            if (isDefaultAgg) {
                currentRealValueExpression = replaceCustomAggField(lodCalcFieldQueryExpression, lodQueryConfigure, expressionMap);
                lodCalcFieldQueryExpressions.put(lodCalcField.getCode(), currentRealValueExpression);
            } else {
                currentRealValueExpression = lodCalcFieldQueryExpression.replaceAll("\\[", "").replaceAll("\\]", "");
            }

            if (BIUtil.isEmpty(analysisFields)) {
                continue;
            }
            // 添加到全局列表中，方便后续步骤引用
            lodCalcAnalysisFields.put(lodCalcField.getCode(), analysisFields);

            for (QueryField lodCalcAnalysisField : analysisFields) {

                String analysisExpression = "";
                AnalysisItemConfig itemConfig = lodCalcAnalysisField.getAnalysisConfig();

                String ratioUnit = PercentFieldRatioUnitType.PERCENT.getCode();

                // 百分比指标
                if (FieldUtil.isPercentField(lodCalcField)) {
                    ratioUnit = itemConfig.getPercentFieldRatioUnit();
                }

                AnalysisCalcMode calcMode = AnalysisCalcMode.get(itemConfig.getCalcMode());
                // 同环对比期
                if (AnalysisCalcMode.get(itemConfig.getCalcMode()).isCompare() || AnalysisCalcMode.CONTRIBUTION_RATE == calcMode || AnalysisCalcMode.ZB_THB == calcMode) {
                    String compareRealValueExpression = lodCalcFieldQueryExpression;
                    Pattern pattern = Pattern.compile(CustomFieldParser.expressionPattern);
                    Matcher matcher = pattern.matcher(lodCalcFieldQueryExpression);

                    AnalysisCalcMode finalMode = itemConfig.getRawThbCalcMode();

                    while (matcher.find()) {
                        String fieldName = matcher.group();
                        String replacement = "";
                        if (finalMode == AnalysisCalcMode.CUSTOM_COMPARE) {
                            replacement = String.format("%s_%s_%s_%s", fieldName, finalMode.getCode(), itemConfig.getCompareIndex(), AnalysisCalcType.REAL_VALUE.getCode());
                        } else {
                            replacement = String.format("%s_%s_%s", fieldName, finalMode.getCode(), AnalysisCalcType.REAL_VALUE.getCode());
                        }
                        if (isDefaultAgg) {
                            compareRealValueExpression = compareRealValueExpression.replaceAll("\\[" + fieldName + "\\]", "\\[" + replacement + "\\]");
                        } else {
                            compareRealValueExpression = compareRealValueExpression.replaceAll("\\[" + fieldName + "\\]", replacement);
                        }
                    }
                    if (isDefaultAgg) {
                        compareRealValueExpression = replaceCustomAggField(compareRealValueExpression, lodQueryConfigure, expressionMap);
                    }

                    AnalysisItemConfig analysisItemConfig = lodCalcAnalysisField.getAnalysisConfig();
                    AnalysisCalcType calcType = AnalysisCalcType.get(analysisItemConfig.getCalcType());
                    String diffValueExpression = String.format("%s - %s", currentRealValueExpression, compareRealValueExpression);

                    //计算差值时，需要对减数与被减数进行coalesce(xxx,0)的转化
                    String coalesceDiffValueExpression = String.format(" COALESCE(%s,0) - COALESCE(%s,0) ", currentRealValueExpression,compareRealValueExpression);

                    OperatorContext operatorContext = new OperatorContext();
                    operatorContext.setConfig(this.mainItem.getConfig());

                    List<QueryField> dimFields = this.mainItem.getConfig().getResult().getRowDimensions().stream().filter(f -> !f.isAppend()).collect(Collectors.toList());
                    operatorContext.setDimFields(dimFields);

                    if (AnalysisCalcMode.CONTRIBUTION_RATE == calcMode) {
                        ContributionRateOperator ctrOperator = new ContributionRateOperator();
                        analysisExpression = ctrOperator.getLodCtrExpression(operatorContext, coalesceDiffValueExpression);

                    } else if (AnalysisCalcMode.ZB_THB == calcMode) {

                        ZbThbOperator zbThbOperator = new ZbThbOperator();
                        analysisExpression = zbThbOperator.getLodCalcExpression(operatorContext, lodCalcAnalysisField,currentRealValueExpression,compareRealValueExpression,ratioUnit);

                    } else {
                        switch (calcType) {
                            case RATIO:
                                QueryField rawMeasure = allMeasures.get(analysisItemConfig.getMeasureId());
                                PercentFieldRatioUnitType percentFieldRatioUnitType = PercentFieldRatioUnitType.get(analysisItemConfig.getPercentFieldRatioUnit());
                                if (percentFieldRatioUnitType == PercentFieldRatioUnitType.PT && (rawMeasure != null && FieldUtil.isPercentField(rawMeasure))) {
                                    analysisExpression = coalesceDiffValueExpression;
                                } else {
                                    /// 比率：分母改为绝对值，处理分母为负数的场景
                                    compareRealValueExpression = fx.abs(compareRealValueExpression);
                                    analysisExpression = fx.division(diffValueExpression, compareRealValueExpression, false);
                                }
                                break;
                            case VALUE:
                                analysisExpression = coalesceDiffValueExpression;
                                break;
                            case REAL_VALUE:
                                analysisExpression = compareRealValueExpression;
                                break;
                        }
                    }

                } else {
                    BaseOperator operator = OperatorFactory.getOperator(itemConfig.getCalcMode());
                    if (operator != null) {
                        OperatorContext operatorContext = new OperatorContext();
                        operatorContext.setConfig(this.mainItem.getConfig());

                        List<QueryField> dimFields = this.mainItem.getConfig().getResult().getRowDimensions().stream().filter(f -> !f.isAppend()).collect(Collectors.toList());
                        operatorContext.setDimFields(dimFields);

                        analysisExpression = operator.lodCalc(operatorContext, currentRealValueExpression,ratioUnit);
                    }
                }

                if (BIUtil.isNotEmpty(analysisExpression)) {
                    lodCalcFieldQueryExpressions.put(lodCalcAnalysisField.getCode(), analysisExpression);
                }
            }
        }

        return lodCalcFieldQueryExpressions;
    }

    // 自定义聚合的依赖指标的处理
    private String replaceCustomAggField(String expression, LodQueryConfigure lodQueryConfigure, Map<String, String> expressionMap) {
        Pattern pattern = Pattern.compile(CustomFieldParser.expressionPattern);
        Matcher matcher = pattern.matcher(expression);
        while (matcher.find()) {
            String fieldCode = matcher.group();
            Optional<LodQueryConfigureItem> item = lodQueryConfigure.getItems().stream()
                    .filter(i -> i.getConfig().getResult().getMeasures().stream().anyMatch(v -> Objects.equals(v.getCode(), fieldCode))).findAny();
            if (item.isPresent()) {
                String alias = item.get().getCode();
                String expr = expressionMap.get(fieldCode);
                if (expr != null) {
                    expression = expression.replaceAll(alias + ".\\[" + fieldCode + "\\]", expr);
                }
            }
        }
        return expression;
    }

    /**
     * 创建结果集列
     * @return
     */
    public List<ResultDataSetColumn> createResultDataSetColumns(QueryEngine engine){
        List<ResultDataSetColumn> columns = new ArrayList<>();
        List<QueryField> allLodFields = new ArrayList<>();
        allLodFields.addAll(this.lodCalcFields);

        Map<String, String> aclCodes = engine.getCxt().getAclFields();
        for(QueryField lodCalcMeasure : allLodFields){
            ResultDataSetColumn lodCalcColumn = engine.createDataSetColumn(lodCalcMeasure, lodCalcMeasure.getCode());
            boolean hasAuth = aclCodes.containsKey(lodCalcMeasure.getRawCode());
            lodCalcColumn.setHasAuth(hasAuth); // 默认lod计算字段有权限
            // 获取其分析字段
            List<QueryField> analysisFields = this.lodCalcAnalysisFields.get(lodCalcMeasure.getCode());
            if(BIUtil.isNotEmpty(analysisFields)){
                // 有分析字段，则需要调整层级结构
                ResultDataSetColumn parentColumn = lodCalcColumn.clone();
                parentColumn.setCode("/" + lodCalcColumn.getCode());

                // 添加本期
                lodCalcColumn.setTitle(BIConsts.ANALYSIS_CURRENT_TITLE);
                lodCalcColumn.setRawTitle(BIConsts.ANALYSIS_CURRENT_TITLE);
                parentColumn.addChild(lodCalcColumn);

                // 添加同环对比
                for(QueryField analysisField : analysisFields){
                    ResultDataSetColumn analysisColumn = engine.createDataSetColumn(analysisField, analysisField.getCode());
                    analysisColumn.setHasAuth(hasAuth);
                    analysisColumn.setCalcMode(analysisField.getAnalysisConfig().getCalcMode());
                    analysisColumn.setCalcType(analysisField.getAnalysisConfig().getCalcType());
                    analysisColumn.setCompareIndex(analysisField.getAnalysisConfig().getCompareIndex());


                    if (AnalysisCalcMode.CONTRIBUTION_RATE == AnalysisCalcMode.get(analysisField.getAnalysisConfig().getCalcMode())) {
                        AnalysisContributionRateItemConfig itemConfig = (AnalysisContributionRateItemConfig) analysisField.getAnalysisConfig();
                        analysisColumn.setCtrCalcMode(itemConfig.getCtrCalcMode());
                    }

                    if (AnalysisCalcMode.ZB_THB == AnalysisCalcMode.get(analysisField.getAnalysisConfig().getCalcMode())) {
                        AnalysisZbThbItemConfig itemConfig = (AnalysisZbThbItemConfig) analysisField.getAnalysisConfig();
                        analysisColumn.getZbThbConfig().setThbCalcMode(itemConfig.getThbCalcMode());
                        analysisColumn.getZbThbConfig().setZbCalcMode(itemConfig.getZbCalcMode());
                    }

                    parentColumn.addChild(analysisColumn);
                }
                columns.add(parentColumn);
            }else {
                if (engine.getConfig() != null && engine.getConfig().getResult() != null &&
                        engine.getConfig().getResult().getFields().stream().anyMatch(v -> Enabled.isTrue(v.getIsAnalysis()))) {
                    // 其他指标分析字段，需要调整层级结构
                    ResultDataSetColumn parentColumn = lodCalcColumn.clone();
                    parentColumn.setCode("/" + lodCalcColumn.getCode());

                    // 添加本期
                    lodCalcColumn.setTitle(BIConsts.ANALYSIS_CURRENT_TITLE);
                    lodCalcColumn.setRawTitle(BIConsts.ANALYSIS_CURRENT_TITLE);
                    parentColumn.addChild(lodCalcColumn);
                    columns.add(parentColumn);
                } else {
                    columns.add(lodCalcColumn);
                }
            }

        }
        return columns;
    }

    /**
     * 移除附加列
     * @param columns
     */
    public List<ResultDataSetColumn> removeAppendDataSetColumns(List<ResultDataSetColumn> columns, List<ResultDataSetColumn> leafColumns){
        List<QueryField> measures = this.rawQueryConfigure.getResult().getMeasures();

        List<String> measureCodes = new ArrayList<>();
        List<String> hiddenCodes = new ArrayList<>();
        for(QueryField measure : measures){
            if (Enabled.value(measure.getIsShow())) {
                measureCodes.add(measure.getCode());
            }
            QueryField copy = fieldCopies.get(measure.getCode());
            if(copy != null && Enabled.isFalse(copy.getIsShow())) {
                hiddenCodes.add(measure.getCode());
            }

            // 若主体字段隐藏，则其派生的分析字段也隐藏
            AnalysisItemConfig analysisItemConfig = measure.getAnalysisConfig();
            if(analysisItemConfig != null) {
                copy = fieldCopies.get(analysisItemConfig.getMeasureCode());
                if (copy != null && Enabled.isFalse(copy.getIsShow())) {
                    hiddenCodes.add(measure.getCode());
                }
            }
        }


        List<ResultDataSetColumn> removeColumns = new ArrayList<>();
        for(ResultDataSetColumn column : leafColumns){
            QueryArea queryArea = QueryArea.get(column.getRawQueryArea());
            if(queryArea == QueryArea.Measure && !measureCodes.contains(column.getCode())){

                //整表总计不处理
                AnalysisCalcMode calcMode = AnalysisCalcMode.get(column.getCalcMode());
                if(AnalysisCalcMode.WHOLE_TABLE_TOTAL == calcMode ){
                    String wholeTableTotalCode = column.getCode().replaceAll("_" + AnalysisCalcMode.WHOLE_TABLE_TOTAL.getCode(), "");
                    if(!hiddenCodes.contains(wholeTableTotalCode)){
                        continue;
                    }

                }

                removeColumns.add(column);
            }
            // 去掉隐藏指标
            if(hiddenCodes.contains(column.getCode())){
                removeColumns.add(column);
            }
        }

        // 递归获取所有列
        Map<String, ResultDataSetColumn> allColumns = new HashMap<>();
        this.recursionAllColumns(columns, allColumns);

        if(BIUtil.isNotEmpty(removeColumns)){
            for(ResultDataSetColumn removeColumn : removeColumns){
                ResultDataSetColumn parentColumn = allColumns.get(removeColumn.getParentCode());
                if(parentColumn != null){
                    parentColumn.removeChild(removeColumn);
                }
                leafColumns.remove(removeColumn);
            }
        }
        return leafColumns;
    }

    /**
     * 递归获取所有列
     * @param columns
     * @param allColumns
     */
    protected void recursionAllColumns(List<ResultDataSetColumn> columns, Map<String, ResultDataSetColumn> allColumns){
        if(BIUtil.isEmpty(columns)){
            return;
        }
        for(ResultDataSetColumn column : columns){
            allColumns.put(column.getCode(), column);
            if(BIUtil.isNotEmpty(column.getChildren())){
                recursionAllColumns(column.getChildren(), allColumns);
            }
        }
    }

    /**
     * 移除隐藏列
     * @param columns
     * @return
     */
    public List<ResultDataSetColumn> removeHiddenDataSetColumns(List<ResultDataSetColumn> columns) {
        List<ResultDataSetColumn> newColumns = new ArrayList<>();
        for (ResultDataSetColumn column : columns) {
            QueryField copy = fieldCopies.get(column.getRawCode());

            if (copy == null) {
                //整表总计特殊处理
                AnalysisCalcMode calcMode = AnalysisCalcMode.get(column.getCalcMode());
                if (AnalysisCalcMode.WHOLE_TABLE_TOTAL == calcMode) {
                    String wholeTableTotalCode = column.getCode().replaceAll("_" + AnalysisCalcMode.WHOLE_TABLE_TOTAL.getCode(), "");
                    copy = fieldCopies.get(wholeTableTotalCode);
                }
            }

            if (copy != null) {
                if (Enabled.isFalse(copy.getIsShow()) || copy.isAppend()) {
                    continue;
                }
            }
            newColumns.add(column);
        }
        return newColumns;
    }

    /**
     * 还原：将前面对字段设置的属性还原
     */
    public void restore(){
        List<QueryField> fields = this.rawQueryConfigure.getAllFields();
        for(QueryField field : fields){
            QueryField copy = fieldCopies.get(field.getCode());
            if(copy == null){
                continue;
            }
            field.setIsShow(copy.getIsShow());
        }
    }

    public LodQueryConfigureItem getMainItem() {
        return mainItem;
    }

    public void setMainItem(LodQueryConfigureItem mainItem) {
        this.mainItem = mainItem;
    }
}
