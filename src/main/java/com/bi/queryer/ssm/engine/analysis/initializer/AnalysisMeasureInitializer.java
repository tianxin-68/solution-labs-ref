package com.bi.queryer.ssm.engine.analysis.initializer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.custom.CustomFieldExpressionIdMapping;
import com.bi.queryer.ssm.custom.CustomFieldType;
import com.bi.queryer.ssm.engine.analysis.cfg.AnalysisConfigFactory;
import com.bi.queryer.ssm.engine.analysis.cfg.AnalysisItemConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.compare.AnalysisCompareConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.compare.AnalysisCompareItemConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.ctr.AnalysisContributionRateItemConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.target.AnalysisTargetConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.target.AnalysisTargetItemConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.thb.*;
import com.bi.queryer.ssm.engine.analysis.cfg.zb.AnalysisZbConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.zb.AnalysisZbItemConfig;
import com.bi.queryer.ssm.engine.config.*;
import com.bi.queryer.ssm.engine.config.field.FieldValue;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.result.ResultDataSetTargetConfig;
import com.bi.queryer.ssm.enums.*;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.ssm.util.FieldUtil;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.period.DateDiff;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;

/**
 * @Author: contributor
 * @CreateTime: 2023-08-23  10:43
 * @Description: 处理分析指标的添加
 */
public class AnalysisMeasureInitializer {

    private QueryResult result = new QueryResult();

    private QueryAnalysis analysis = new QueryAnalysis();

    private QuerySettings settings = new QuerySettings();

    private QueryFilter filter = new QueryFilter();

    private QueryConfigure config = null;

    public AnalysisMeasureInitializer(QueryConfigure config) {
        this.config = config;
        this.result = config.getResult();
        this.analysis = config.getAnalysis();
        this.settings = config.getSettings();
        this.filter = config.getFilter();
    }

    public void initialize() {

        //初始化贡献率，添加列小计、行总计、对比差值
        AnalysisContributionRateConfigInitializer ctrInitializer = new AnalysisContributionRateConfigInitializer(result, analysis, settings);
        ctrInitializer.initialize();

        //目标值分析
        addTargetAnalysisMeasure();

        //同环比
        addThbAnalysisMeasure();

        //目标值的同环比
        addTargetThbAnalysisMeasure();

        //自定义对比
        addCompareAnalysisMeasure();

        //占比
        addZbAnalysisMeasure();

        //占比的同环比
        addZbThbAnalysisMeasure();

        //占比的自定义对比
        addZbCompareAnalysisMeasure();

        //添加依赖分析字段四则运算的依赖
        addAnalysisCalcDependency();

    }

    public void addTargetAnalysisMeasure() {
        AnalysisTargetConfig targetAnalysis = analysis.getTarget();

        //未启用
        if (!targetAnalysis.isActive() || config.isAggQuery()) {
            return;
        }

        //获取公共日期
        String dateFieldCode = getDateFieldCode();
        if (StrUtil.isEmpty(dateFieldCode)) {
            return;
        }

        List<QueryField> targetAnalysisMeasureList = new ArrayList<>();

        //去重校验
        Map<String, String> unionIdMap = new HashMap<>();
        for (AnalysisTargetItemConfig itemConfig : targetAnalysis.getItems()) {
            //获取同环比配置
            if (CollUtil.isEmpty(itemConfig.getConfigs())) {
                continue;
            }

            List<String> measureIdList = itemConfig.getMeasureIdList();
            for (String measureId : measureIdList) {
                QueryField measureData = result.getFieldById(measureId);
                if (measureData == null) {
                    continue;
                }

                //字段不可见，不处理
                if (!Enabled.value(measureData.getIsShow())) {
                    continue;
                }

                // lod字段不处理
                //兼容跨模型指标
//                if (measureData.isCustom() && FieldType.CROSS_MODEL_MEASURE != FieldType.get(measureData.getFieldType()) ) {
//                    continue;
//                }

                //四则运算包含分析项指标不出同环比和自定义对比
                if (measureData.isAnalysisCalc()) {
                    continue;
                }

                for (AnalysisTargetItemConfig.TargetCalcConfig calcConfig : itemConfig.getConfigs()) {
                    String calcMode = calcConfig.getCalcMode();
                    for (String calcType : calcConfig.getCalcTypes()) {
                        QueryField queryField;
                        if (AnalysisCalcType.CONTRIBUTION_RATE == AnalysisCalcType.get(calcType)) {
                            queryField = buildCtrQueryField(measureData, calcMode, dateFieldCode, 0);
                        } else {
                            queryField = buildAnalysisQueryField(measureData, calcMode, calcType, dateFieldCode, null);
                            queryField.getAnalysisConfig().setCalcMode("");
                            queryField.getAnalysisConfig().setCalcType("");
                        }
                        // 设置目标值分析配置
                        ResultDataSetTargetConfig targetConfig = new ResultDataSetTargetConfig();
                        targetConfig.setTargetCalcMode(calcMode);
                        targetConfig.setTargetCalcType(calcType);
                        queryField.getAnalysisConfig().setTargetConfig(targetConfig);

                        String title = targetConfig.getTargetTitle();
                        queryField.setTitle(title);

                        //queryField.getAnalysisConfig().setPercentFieldRatioUnit(analysisThbItemConfig.getPercentFieldRatioUnit());
                        String unionId = String.format("%s_%s_%s", measureData.getId(), calcMode, calcType);
                        if (!unionIdMap.containsKey(unionId)) {
                            targetAnalysisMeasureList.add(queryField);
                            unionIdMap.put(unionId, unionId);
                        }
                    }
                }
            }
        }

        addResultMeasure(targetAnalysisMeasureList);
    }

    /**
     * 添加同环比分析字段
     */
    public void addThbAnalysisMeasure() {

        AnalysisThbConfig analysisThbConfig = analysis.getThb();

        //未启用
        if (!analysisThbConfig.isActive()) {
            return;
        }


        //获取公共日期
        String dateFieldCode = getDateFieldCode();
        if (StrUtil.isEmpty(dateFieldCode)) {
            return;
        }

        List<QueryField> thbAnalysisMeasureList = new ArrayList<>();

        //去重校验
        Map<String, String> unionIdMap = new HashMap<>();

        for (AnalysisThbItemConfig analysisThbItemConfig : analysisThbConfig.getItems()) {

            //获取同环比配置
            if (CollUtil.isEmpty(analysisThbItemConfig.getCalcModes()) || CollUtil.isEmpty(analysisThbItemConfig.getCalcTypes())) {
                continue;
            }

            List<String> measureIdList = analysisThbItemConfig.getMeasureIdList();
            //选择了所有指标，则从配置项取
            if (measureIdList.contains(BIConsts.ALL_MEASURE)) {
                measureIdList = result.getMeasures().stream().filter(q -> !Enabled.value(q.getIsAnalysis())).map(QueryField::getId).collect(Collectors.toList());
            }

            for (String measureId : measureIdList) {

                QueryField measureData = result.getFieldById(measureId);
                if (measureData == null) {
                    continue;
                }

                //字段不可见，不处理
                if (!Enabled.value(measureData.getIsShow())) {
                    //continue;
                }

                // lod字段不处理
                if (measureData.isLodField()) {
                    //continue;
                }

                //四则运算包含分析项指标不出同环比和自定义对比
                if (measureData.isAnalysisCalc()) {
                    continue;
                }

                for (String calcMode : analysisThbItemConfig.getCalcModes()) {

                    for (String calcType : analysisThbItemConfig.getCalcTypes()) {

                        QueryField queryField = null;
                        if (AnalysisCalcType.CONTRIBUTION_RATE == AnalysisCalcType.get(calcType)) {
                            //lod不出贡献率
                            if (measureData.isLodField()) {
                            //    continue;
                            }

                            if (!analysisThbItemConfig.getCtr().getItems().contains(BIConsts.ALL_DIM) && !analysisThbItemConfig.getCtr().getItems().contains(calcMode)) {
                                continue;
                            }
                            queryField = buildCtrQueryField(measureData, calcMode, dateFieldCode, 0);
                        } else {
                            queryField = buildAnalysisQueryField(measureData, calcMode, calcType, dateFieldCode, null);
                        }

                        queryField.getAnalysisConfig().setPercentFieldRatioUnit(analysisThbItemConfig.getPercentFieldRatioUnit());
                        String unionId = String.format("%s_%s_%s", measureData.getId(), calcMode, calcType);
                        if (!unionIdMap.containsKey(unionId)) {
                            thbAnalysisMeasureList.add(queryField);
                            unionIdMap.put(unionId, unionId);
                        }

                    }

                }
            }

        }

        addResultMeasure(thbAnalysisMeasureList);

    }

    /**
     * 构建分析字段的唯一值
     */
    public String buildAnalysisCode(AnalysisItemConfig config) {
        String res = "";
        AnalysisCalcMode analysisCalcMode = AnalysisCalcMode.get(config.getCalcMode());
        String measureCode = config.getMeasureCode();
        if (StrUtil.isEmpty(measureCode)) {
            measureCode = BIConsts.Custom_Field_Name_Suffix + QueryArea.Measure.getShortCode() + "_" + config.getMeasureId().replaceAll("[\\.\\-\\ :]", "_");
            //QueryField queryField = result.getFieldById(config.getMeasureId());
            //measureCode = BIConsts.Custom_Field_Name_Suffix + QueryArea.Measure.getShortCode() + "_" + (queryField.getShowOrder() + "").replaceAll("[\\.\\-]", "_");
        }

        // 添加目标值的code构造逻辑
        if (config.getTargetConfig() != null && config.getTargetConfig().isActive()) {
            measureCode = measureCode + config.getTargetConfig().getTargetCode();
        }

        //自定义对比需要添加自定义对比的序号，来保证唯一
        if (AnalysisCalcMode.CUSTOM_COMPARE == analysisCalcMode) {
            AnalysisCompareConfig compareConfig = analysis.getCompare();
            AnalysisCompareItemConfig itemConfig = compareConfig.getItems().stream().filter(c -> Objects.equals(c.getId(), config.getCmpId())).findFirst().orElse(null);
            checkState(itemConfig != null, "四则运算中包含的自定义对比配置不存在");
            config.setCompareIndex(itemConfig.getCompareIndex());
            res = String.format("%s_%s_%s_%s", measureCode, config.getCalcMode(), itemConfig.getCompareIndex(), config.getCalcType());
        } else if (AnalysisCalcMode.CONTRIBUTION_RATE == analysisCalcMode) {
            res = String.format("%s_%s_%s_%s", measureCode, config.getCtrCalcMode(), config.getCalcMode(), config.getCalcType());
        } else if (AnalysisCalcMode.ZB_THB == analysisCalcMode) {
            String rawThbCalcMode = config.getZbThbConfig().getThbCalcMode();
            if (AnalysisCalcMode.CUSTOM_COMPARE.getCode().equals(rawThbCalcMode)) {
                res = String.format("%s_%s_%s_%s_%s", measureCode, config.getZbThbConfig().getZbCalcMode(), config.getZbThbConfig().getThbCalcMode(), config.getCompareIndex(), config.getCalcType());
            } else {
                res = String.format("%s_%s_%s_%s", measureCode, config.getZbThbConfig().getZbCalcMode(), config.getZbThbConfig().getThbCalcMode(), config.getCalcType());
            }
        } else {
            if (config.getTargetConfig() != null && config.getTargetConfig().isActive()) {
                res = measureCode;
                if (StringUtils.isNotEmpty(config.getCalcMode())) {
                    res = measureCode + "_" + config.getCalcMode();
                }
                if (StringUtils.isNotEmpty(config.getCalcType())) {
                    res = res + "_" + config.getCalcType();
                }
            } else {
                //同环比的唯一值 = calcMode + calcType  例：环比实际值 hb_r_value
                res = String.format("%s_%s_%s", measureCode, config.getCalcMode(), config.getCalcType());
            }
        }
        return res;
    }

    private void addAnalysisCalcDependency() {
        List<CustomFieldExpressionIdMapping> analysisCalcFields = new ArrayList<>(8);
        for (QueryField f : result.getMeasures()) {
            if (f.isAnalysisCalc()) {
                analysisCalcFields.addAll(f.getCustomFieldConfigure().getExpressionIdMapping());
            }
        }
        Set<String> selectFieldCodes = result.getFields().stream().map(QueryField::getCode).collect(Collectors.toSet());
        List<QueryField> dependencyFields = new ArrayList<>();
        for (CustomFieldExpressionIdMapping mapping : analysisCalcFields) {
            String fieldId = mapping.getId();
            AnalysisItemConfig analysisItemConfig = mapping.getAnalysisItemConfig();
            if (!fieldId.contains(CustomFieldType.ANALYSIS.getIdentifier()) || analysisItemConfig == null) {
                continue;
            }

            String fieldCode = buildAnalysisCode(analysisItemConfig);
            if (StrUtil.isEmpty(fieldCode)) {
                continue;
            }
            //code回写，后续要用
            mapping.setCode(fieldCode);
            if (selectFieldCodes.contains(fieldCode)) {
                continue;
            }

            List<QueryField> queryFields = buildDependencyQueryField(analysisItemConfig, selectFieldCodes);
            if (CollUtil.isNotEmpty(queryFields)) {
                queryFields.forEach(v -> {
                    if (!dependencyFields.contains(v)) {
                        dependencyFields.add(v);
                    }
                });
            }
        }

        addResultMeasure(dependencyFields);
    }

    public List<QueryField> buildDependencyQueryField(AnalysisItemConfig analysisConfig, Set<String> selectFieldCodes) {
        AnalysisCalcType calcType = AnalysisCalcType.get(analysisConfig.getCalcType());
        AnalysisCalcMode calcMode = AnalysisCalcMode.get(analysisConfig.getCalcMode());

        List<QueryField> dependencyFields = new ArrayList<>(4);
        String measureCode = analysisConfig.getMeasureCode();
        if (StrUtil.isEmpty(measureCode)) {
            // 计算指标 没有 指标code， 依赖字段只能从 选中的字段列表中找， 基于此依赖计算字段 构造分析指标
            QueryField measureData = result.getFieldById(analysisConfig.getMeasureId());
            QueryField field = buildAnalysisQueryField(measureData, analysisConfig.getCalcMode(), analysisConfig.getCalcType(), "", null);
            field.setIsShow(Enabled.NO.getId());
            dependencyFields.add(field);
            if (calcMode.isTb() || calcMode.isHb()) {
                AnalysisThbConfig thbConfig = this.analysis.getThb();
                if (CollUtil.isEmpty(thbConfig.getItems())) {
                    AnalysisThbItemConfig thbItemConfig = new AnalysisThbItemConfig();
                    thbItemConfig.setCalcTypes(new ArrayList<>(Collections.singletonList(calcType.getCode())));
                    thbItemConfig.setMeasureIdList(new ArrayList<>(Collections.singletonList(measureData.getId())));
                    thbItemConfig.setCalcModes(new ArrayList<>(Collections.singletonList(calcMode.getCode())));
                    thbConfig.getItems().add(thbItemConfig);
                } else {
                    thbConfig.getItems().forEach(item -> {
                        if (!item.getMeasureIdList().contains(measureData.getId())) {
                            item.getMeasureIdList().add(measureData.getId());
                        }
                    });
                }
                thbConfig.setIsActive(Enabled.YES.getId());
            } else if (calcMode.isZb()) {
                AnalysisZbConfig zbConfig = this.analysis.getZb();
                if (CollUtil.isEmpty(zbConfig.getItems())) {
                    AnalysisZbItemConfig zbItemConfig = new AnalysisZbItemConfig();
                    zbItemConfig.setCalcMode(calcMode.getCode());
                    zbItemConfig.setMeasureIdList(new ArrayList<>(Collections.singletonList(measureData.getId())));
                    zbConfig.getItems().add(zbItemConfig);
                } else {
                    zbConfig.getItems().forEach(item -> {
                        if (!item.getMeasureIdList().contains(measureData.getId())) {
                            item.getMeasureIdList().add(measureData.getId());
                        }
                    });
                }
                zbConfig.setIsActive(Enabled.YES.getId());
            } else if (calcMode.isCompare()) {
                AnalysisCompareConfig compareConfig = this.analysis.getCompare();
                if (CollUtil.isEmpty(compareConfig.getItems())) {
                } else {
                    compareConfig.getItems().forEach(item -> {
                        if (!item.getMeasureIdList().contains(measureData.getId())) {
                            item.getMeasureIdList().add(measureData.getId());
                        }
                    });
                }
                compareConfig.setIsActive(Enabled.YES.getId());
            }
        } else {
            //有指标code, 直接从元数据中找
            List<MetaField> metaFields = SSDMetaCacheManager.getFieldByCode(measureCode);
            if (BIUtil.isNotEmpty(metaFields)) {
                MetaField meta = metaFields.stream().filter(m -> Objects.equals(m.getId(), analysisConfig.getMeasureId()))
                        .findFirst().orElse(metaFields.get(0));
                if (!selectFieldCodes.contains(measureCode)) {
                    QueryField measureData = new QueryField(meta);
                    measureData.setIsShow(Enabled.NO.getId());
                    measureData.setIsResult(true);
                    measureData.setQueryArea(QueryArea.Measure);
                    measureData.setRawQueryArea(QueryArea.Measure);
                    dependencyFields.add(measureData);
                }

                QueryField queryField = new QueryField();
                String key;
                String title;
                if (analysisConfig.getTargetConfig() != null && analysisConfig.getTargetConfig().isActive()) {
                    key = measureCode + analysisConfig.getTargetConfig().getTargetCode();
                    title = String.format("%s%s", calcMode.getDesc(), calcType.getTitle());
                    if (StringUtils.isNotEmpty(analysisConfig.getCalcMode())) {
                        key = measureCode + "_" + analysisConfig.getCalcMode();
                    }
                    if (StringUtils.isNotEmpty(analysisConfig.getCalcType())) {
                        key = measureCode + "_" + analysisConfig.getCalcType();
                    }
                } else {
                    key = String.format("%s_%s_%s", meta.getCode(), calcMode.getCode(), calcType.getCode());
                    title = String.format("%s%s", calcMode.getDesc(), calcType.getTitle());
                }

                queryField.setId(key);
                queryField.setCode(key);
                queryField.setName(key);

                queryField.setTitle(title);
                queryField.setSortType(FieldSortType.NONE);
                queryField.setIsAnalysis(Enabled.YES.getId());
                queryField.setIsShow(Enabled.NO.getId());
                queryField.setAppend(true);

                queryField.setIsResult(true);
                queryField.setQueryArea(QueryArea.Measure);
                queryField.setRawQueryArea(QueryArea.Measure);

                AnalysisItemConfig analysisItemConfig = AnalysisConfigFactory.get(calcMode.getCode());
                analysisItemConfig.setDateFieldCode(getDateFieldCode());
                analysisItemConfig.setDateGranularity(settings.getDateGranularity());
                analysisItemConfig.setMeasureId(meta.getId());
                analysisItemConfig.setMeasureCode(meta.getCode());
                analysisItemConfig.setCalcMode(calcMode.getCode());
                analysisItemConfig.setCalcType(calcType.getCode());
                analysisItemConfig.setZbThbConfig(analysisConfig.getZbThbConfig());
                analysisItemConfig.setCompareIndex(analysisConfig.getCompareIndex());
                if (analysisItemConfig instanceof AnalysisZbThbItemConfig) {
                    AnalysisZbThbItemConfig zbThbItemConfig = (AnalysisZbThbItemConfig) analysisItemConfig;
                    zbThbItemConfig.setZbCalcMode(analysisConfig.getZbThbConfig().getZbCalcMode());
                    zbThbItemConfig.setThbCalcMode(analysisConfig.getZbThbConfig().getThbCalcMode());
                }

                // 添加指标目标值
                if (analysisConfig.getTargetConfig() != null) {
                    analysisItemConfig.setTargetConfig(analysisConfig.getTargetConfig());
                }

                queryField.setAnalysisConfig(analysisItemConfig);
                dependencyFields.add(queryField);
            }
        }
        return dependencyFields;
    }

    /**
     * 添加目标分析同环比分析字段
     */
    public void addTargetThbAnalysisMeasure() {
        AnalysisThbConfig analysisThbConfig = analysis.getThb();
        //未启用
        if (!analysisThbConfig.isActive() || config.isAggQuery()) {
            return;
        }

        //获取公共日期
        String dateFieldCode = getDateFieldCode();
        if (StrUtil.isEmpty(dateFieldCode)) {
            return;
        }

        List<QueryField> zbThbAnalysisMeasureList = new ArrayList<>();
        for (AnalysisThbItemConfig analysisThbItemConfig : analysisThbConfig.getItems()) {
            //获取同环比配置
            if (CollUtil.isEmpty(analysisThbItemConfig.getCalcModes()) || CollUtil.isEmpty(analysisThbItemConfig.getCalcTypes())) {
                continue;
            }

            List<AnalysisTargetThbConfig> targetConfigs = analysisThbItemConfig.getTargetThbConfigs();
            if (CollUtil.isEmpty(targetConfigs)) {
                continue;
            }

            Map<String, String> unionIdMap = new HashMap<>();
            for (AnalysisTargetThbConfig targetConfig : targetConfigs) {
                if (CollUtil.isEmpty(targetConfig.getTargetList())) {
                    continue;
                }

                QueryField measureData = result.getFieldById(targetConfig.getMeasureId());
                if (measureData == null) {
                    continue;
                }

                //四则运算包含分析项指标不出同环比和自定义对比
                if (measureData.isAnalysisCalc()) {
                    continue;
                }

                if (measureData.isTargetValue() && !measureData.isTargetRawValue()) {
                    continue;
                }

                //字段不可见，不处理
                if (!Enabled.value(measureData.getIsShow())) {
                    //continue;
                }

                //率值指标不出占比
                if (measureData.isPercentField()) {
                    // continue;
                }

                for (String calcMode : analysisThbItemConfig.getCalcModes()) {
                    AnalysisCalcMode analysisCalcMode = AnalysisCalcMode.get(calcMode);
                    for (String calcType : analysisThbItemConfig.getCalcTypes()) {
                        AnalysisCalcType analysisCalcType = AnalysisCalcType.get(calcType);
                        //贡献率不处理
                        if (AnalysisCalcType.CONTRIBUTION_RATE == analysisCalcType) {
                            continue;
                        }

                        for (AnalysisTargetThbConfig.TargetCalcConfig targetCalcConfig : targetConfig.getTargetList()) {
                            AnalysisCalcMode targetCalcMode = AnalysisCalcMode.get(targetCalcConfig.getTargetCalcMode());
                            if (AnalysisCalcMode.TIME_PROGRESS == targetCalcMode || AnalysisCalcMode.PREDICT_VALUE == targetCalcMode) {
                                continue;
                            }
                            QueryField queryField = buildAnalysisQueryField(measureData, calcMode, calcType, dateFieldCode, null);
                            // 设置目标值分析配置
                            ResultDataSetTargetConfig targetCfg = new ResultDataSetTargetConfig();
                            targetCfg.setTargetCalcMode(targetCalcConfig.getTargetCalcMode());
                            targetCfg.setTargetCalcType(targetCalcConfig.getTargetCalcType());
                            queryField.getAnalysisConfig().setTargetConfig(targetCfg);

                            String key = String.format("%s_%s_%s_%s_%s",
                                    measureData.getCode(), targetCalcConfig.getTargetCalcMode(), targetCalcConfig.getTargetCalcType(),
                                    calcMode, calcType);

                            queryField.setId(key);
                            queryField.setCode(key);
                            queryField.setName(key);

                            String title = String.format("%s的%s%s",
                                    targetCfg.getTargetTitle(),
                                    analysisCalcMode.getDesc(),
                                    analysisCalcType.getTitle());
                            queryField.setTitle(title);

                            queryField.getAnalysisConfig().setPercentFieldRatioUnit(analysisThbItemConfig.getPercentFieldRatioUnit());
                            String unionId = key;
                            if (!unionIdMap.containsKey(unionId)) {
                                zbThbAnalysisMeasureList.add(queryField);
                                unionIdMap.put(unionId, unionId);
                            }

                        }
                    }
                }
            }
        }

        addResultMeasure(zbThbAnalysisMeasureList);
    }

    /**
     * 添加占比同环比分析字段
     */
    public void addZbThbAnalysisMeasure() {

        AnalysisThbConfig analysisThbConfig = analysis.getThb();

        //未启用
        if (!analysisThbConfig.isActive()) {
            return;
        }

        //获取公共日期
        String dateFieldCode = getDateFieldCode();
        if (StrUtil.isEmpty(dateFieldCode)) {
            return;
        }

        List<QueryField> zbThbAnalysisMeasureList = new ArrayList<>();

        for (AnalysisThbItemConfig analysisThbItemConfig : analysisThbConfig.getItems()) {

            //获取同环比配置
            if (CollUtil.isEmpty(analysisThbItemConfig.getCalcModes()) || CollUtil.isEmpty(analysisThbItemConfig.getCalcTypes())) {
                continue;
            }

            List<AnalysisZbThbConfig> zbThbConfigs = analysisThbItemConfig.getZbThbConfigs();
            if (CollUtil.isEmpty(zbThbConfigs)) {
                continue;
            }

            Map<String, String> unionIdMap = new HashMap<>();

            for (AnalysisZbThbConfig zbThbConfig : zbThbConfigs) {

                if (CollUtil.isEmpty(zbThbConfig.getCalcModeList())) {
                    continue;
                }

                QueryField measureData = result.getFieldById(zbThbConfig.getMeasureId());
                if (measureData == null) {
                    continue;
                }

                //四则运算包含分析项指标不出同环比和自定义对比
                if (measureData.isAnalysisCalc()) {
                    continue;
                }

                //字段不可见，不处理
                if (!Enabled.value(measureData.getIsShow())) {
                    //continue;
                }

                //率值指标不出占比
                if(measureData.isPercentField()){
                   // continue;
                }

                for (String zbCalcMode : zbThbConfig.getCalcModeList()) {

                    AnalysisCalcMode analysisZbCalcMode = AnalysisCalcMode.get(zbCalcMode);

                    for (String calcMode : analysisThbItemConfig.getCalcModes()) {

                        AnalysisCalcMode analysisCalcMode =  AnalysisCalcMode.get(calcMode);

                        for (String calcType : analysisThbItemConfig.getCalcTypes()) {

                            AnalysisCalcType analysisCalcType = AnalysisCalcType.get(calcType);

                            //贡献率不处理
                            if (AnalysisCalcType.CONTRIBUTION_RATE == analysisCalcType){
                                continue;
                            }

                            QueryField queryField = null;
                            queryField = buildAnalysisQueryField(measureData, AnalysisCalcMode.ZB_THB.getCode(), calcType, dateFieldCode, null);

                            AnalysisZbThbItemConfig analysisZbThbItemConfig = (AnalysisZbThbItemConfig) queryField.getAnalysisConfig();
                            analysisZbThbItemConfig.setThbCalcMode(calcMode);
                            analysisZbThbItemConfig.setZbCalcMode(zbCalcMode);
                            queryField.setAnalysisConfig(analysisZbThbItemConfig);

                            String key = String.format("%s_%s_%s_%s",
                                    measureData.getCode(), zbCalcMode,
                                    calcMode,
                                    calcType);

                            queryField.setId(key);
                            queryField.setCode(key);
                            queryField.setName(key);

                            String title = String.format("%s的%s%s",
                                    getCalcModeTitle(analysisZbCalcMode),
                                    getCalcModeTitle(analysisCalcMode),
                                    analysisCalcType.getTitle());
                            queryField.setTitle(title);

                            queryField.getAnalysisConfig().setPercentFieldRatioUnit(analysisThbItemConfig.getPercentFieldRatioUnit());
                            String unionId = key;
                            if (!unionIdMap.containsKey(unionId)) {
                                zbThbAnalysisMeasureList.add(queryField);
                                unionIdMap.put(unionId, unionId);
                            }

                        }

                    }

                }

            }

        }

        addResultMeasure(zbThbAnalysisMeasureList);

    }

    /**
     * 添加占比的自定义对比分析字段
     */
    public void addZbCompareAnalysisMeasure() {
        AnalysisCompareConfig analysisCompareConfig = analysis.getCompare();

        //未启用
        if (!analysisCompareConfig.isActive()) {
            return;
        }

        //业务日历不出自定义对比
        if(config.getSettings().isBusinessCalendar()){
            return;
        }

        //获取公共日期
        String dateFieldCode = getDateFieldCode();
        if (StrUtil.isEmpty(dateFieldCode)) {
            return;
        }

        List<QueryField> zbThbAnalysisMeasureList = new ArrayList<>();

        for (AnalysisCompareItemConfig analysisCompareItemConfig : analysisCompareConfig.getItems()) {

            //获取同环比配置
            if (BIUtil.isEmpty(analysisCompareItemConfig.getCalcMode()) || CollUtil.isEmpty(analysisCompareItemConfig.getCalcTypes())) {
                continue;
            }

            List<AnalysisZbThbConfig> zbThbConfigs = analysisCompareItemConfig.getZbThbConfigs();
            if (CollUtil.isEmpty(zbThbConfigs)) {
                continue;
            }

            Map<String, String> unionIdMap = new HashMap<>();
            for (AnalysisZbThbConfig zbThbConfig : zbThbConfigs) {
                if (CollUtil.isEmpty(zbThbConfig.getCalcModeList())) {
                    continue;
                }

                QueryField measureData = result.getFieldById(zbThbConfig.getMeasureId());
                if (measureData == null) {
                    continue;
                }

                //四则运算包含分析项指标不出同环比和自定义对比
                if (measureData.isAnalysisCalc()) {
                    continue;
                }

                for (String zbCalcMode : zbThbConfig.getCalcModeList()) {
                    String calcMode = analysisCompareItemConfig.getCalcMode();
                    AnalysisCalcMode analysisZbCalcMode = AnalysisCalcMode.get(zbCalcMode);

                    for (String calcType : analysisCompareItemConfig.getCalcTypes()) {
                        AnalysisCalcType analysisCalcType = AnalysisCalcType.get(calcType);

                        //贡献率不处理
                        if (AnalysisCalcType.CONTRIBUTION_RATE == analysisCalcType) {
                            continue;
                        }

                        QueryField queryField = buildAnalysisQueryField(measureData, AnalysisCalcMode.ZB_THB.getCode(), calcType, dateFieldCode, analysisCompareItemConfig.getCompareIndex());

                        AnalysisZbThbItemConfig analysisZbThbItemConfig = (AnalysisZbThbItemConfig) queryField.getAnalysisConfig();
                        analysisZbThbItemConfig.setThbCalcMode(calcMode);
                        analysisZbThbItemConfig.setZbCalcMode(zbCalcMode);
                        analysisZbThbItemConfig.setCompareIndex(analysisCompareItemConfig.getCompareIndex());
                        queryField.setAnalysisConfig(analysisZbThbItemConfig);

                        String key = String.format("%s_%s_%s_%s_%s",
                                measureData.getCode(), zbCalcMode,
                                calcMode,
                                analysisCompareItemConfig.getCompareIndex(),
                                calcType);

                        queryField.setId(key);
                        queryField.setCode(key);
                        queryField.setName(key);

                        String title = String.format("%s的%s%s",
                                getCalcModeTitle(analysisZbCalcMode),
                                analysisCompareItemConfig.getTitle(),
                                analysisCalcType.getTitle());
                        queryField.setTitle(title);

                        queryField.getAnalysisConfig().setPercentFieldRatioUnit(analysisCompareItemConfig.getPercentFieldRatioUnit());
                        if (!unionIdMap.containsKey(key)) {
                            zbThbAnalysisMeasureList.add(queryField);
                            unionIdMap.put(key, key);
                        }
                    }
                }
            }
        }

        addResultMeasure(zbThbAnalysisMeasureList);
    }

    /**
     * 添加占比分析字段
     */
    public void addZbAnalysisMeasure() {

        AnalysisZbConfig analysisZbConfig = analysis.getZb();

        //未启用
        if (!analysisZbConfig.isActive()) {
            return;
        }

        List<QueryField> zbAnalysisMeasureList = new ArrayList<>();

        //用于去重
        Map<String, String> unionIdMap = new HashMap<>();

        for (AnalysisZbItemConfig analysisZbItemConfig : analysisZbConfig.getItems()) {
            List<String> measureIdList = analysisZbItemConfig.getMeasureIdList();

            AnalysisCalcMode analysisCalcMode = AnalysisCalcMode.get(analysisZbItemConfig.getCalcMode());

            long rowDimensionCount = result.getRowDimensions().stream().count();
            //只有一个行维度，不处理列小计
            if (AnalysisCalcMode.ZB_COL_SUBTOTAL == analysisCalcMode) {
                if (rowDimensionCount <= 1) {
                    continue;
                }
            }

            //一个行维度都没有，不出列总计
            if (AnalysisCalcMode.ZB_COL_TOTAL == analysisCalcMode) {
                if (rowDimensionCount == 0) {
                    continue;
                }
            }

            //选择了所有指标，则从配置项取
            if (measureIdList.contains(BIConsts.ALL_MEASURE)) {
                measureIdList = result.getMeasures().stream()
//                        .filter(q -> !Enabled.value(q.getIsAnalysis()) && AggExpressionType.Sum == AggExpressionType.get(q.getMeta().getAggExpression()))
                        .filter(q -> !Enabled.value(q.getIsAnalysis()))
                        .map(QueryField::getId)
                        .collect(Collectors.toList());
            }

            //如果measureIdList，没有一个在查询区域且可见，不查占比
            int activeMeasureCount = 0;
            for (String measureId : measureIdList) {
                QueryField measureData = result.getFieldById(measureId);
                if (measureData == null) {
                    continue;
                }

                if(measureData.isAppend()){
                    continue;
                }

                if (!Enabled.value(measureData.getIsShow())) {
                    continue;
                }
                activeMeasureCount++;
            }

            if (activeMeasureCount == 0) {
                continue;
            }

            for (String measureId : measureIdList) {
                QueryField measureData = result.getFieldById(measureId);
                if (measureData == null) {
                    continue;
                }

                //字段不可见，不处理
                if (!Enabled.value(measureData.getIsShow())) {
                    //continue;
                }

                //率值指标不出占比
                if (measureData.isPercentField()) {
                    // continue;
                }

                // lod字段不处理 占整表总计
                if (measureData.isLodField() && AnalysisCalcMode.ZB_WHOLE_TABLE_TOTAL == analysisCalcMode) {
                    continue;
                }

                //四则运算包含分析项指标且是百分比指标不出占比
                //20251023 率值指标需要出占比
//                if (measureData.isAnalysisCalc() && measureData.getCustomFieldConfigure().getRatio()) {
//                    continue;
//                }

                // 四则运算包含分析项指标不处理 占整表总计
                if (measureData.isAnalysisCalc() && AnalysisCalcMode.ZB_WHOLE_TABLE_TOTAL == analysisCalcMode) {
                    continue;
                }

                QueryField queryField = buildAnalysisQueryField(measureData, analysisZbItemConfig.getCalcMode(), AnalysisCalcType.RATIO.getCode(), "", null);
                String title = String.format("%s比例", analysisCalcMode.getDesc());
                queryField.setTitle(title);

                queryField.getAnalysisConfig().setPercentFieldRatioUnit(analysisZbConfig.getPercentFieldRatioUnit());
                String unionId = String.format("%s_%s_%s", measureData.getId(), analysisZbItemConfig.getCalcMode(), AnalysisCalcType.RATIO.getCode());
                if (!unionIdMap.containsKey(unionId)) {
                    zbAnalysisMeasureList.add(queryField);
                    unionIdMap.put(unionId, unionId);
                }
            }

        }

        addResultMeasure(zbAnalysisMeasureList);

    }

    /**
     * 添加自定义对比分析字段
     */
    public void addCompareAnalysisMeasure() {

        AnalysisCompareConfig analysisCompareConfig = analysis.getCompare();

        //未启用
        if (!analysisCompareConfig.isActive()) {
            return;
        }

        //业务日历不出自定义对比
        if(config.getSettings().isBusinessCalendar()){
            return;
        }

        //获取公共日期
        String dateFieldCode = getDateFieldCode();
        if (StrUtil.isEmpty(dateFieldCode)) {
            return;
        }

        List<QueryField> compareAnalysisMeasureList = new ArrayList<>();

        //去重校验
        Map<String, String> unionIdMap = new HashMap<>();

        for (AnalysisCompareItemConfig analysisCompareItemConfig : analysisCompareConfig.getItems()) {

            List<String> measureIdList = analysisCompareItemConfig.getMeasureIdList();
            //选择了所有指标，则从配置项取
            if (measureIdList.contains(BIConsts.ALL_MEASURE)) {
                measureIdList = result.getMeasures().stream()
                        .filter(q -> !Enabled.value(q.getIsAnalysis()) || q.isTargetRawValue())
                        .map(QueryField::getId)
                        .collect(Collectors.toList());
            }

            for (String measureId : measureIdList) {
                QueryField measureData = result.getFieldById(measureId);
                if (measureData == null) {
                    continue;
                }

                //字段不可见，不处理
                if(!Enabled.value(measureData.getIsShow())){
                    //continue;
                }

                // lod字段不处理
                if(measureData.isLodField()){
                    // continue;
                }

                //四则运算包含分析项指标不出同环比和自定义对比
                if (measureData.isAnalysisCalc()) {
                    continue;
                }

                for (String calcType : analysisCompareItemConfig.getCalcTypes()) {

                    QueryField queryField = null;
                    if (AnalysisCalcType.CONTRIBUTION_RATE == AnalysisCalcType.get(calcType)) {
                        //lod不出贡献率
                        if (measureData.isLodField()) {
                            continue;
                        }

                        queryField = buildCtrQueryField(measureData, AnalysisCalcMode.CUSTOM_COMPARE.getCode(), dateFieldCode, analysisCompareItemConfig.getCompareIndex());
                    }else{
                        queryField = buildAnalysisQueryField(measureData, analysisCompareItemConfig.getCalcMode(), calcType, dateFieldCode, analysisCompareItemConfig.getCompareIndex());
                    }

                    queryField.getAnalysisConfig().setPercentFieldRatioUnit(analysisCompareItemConfig.getPercentFieldRatioUnit());
                    // 日期差值计算
                    Long dateDiff = DateDiff.getDateDiff(analysisCompareItemConfig.getDateGranularity(), analysisCompareItemConfig.getCompareDates().get(0), analysisCompareItemConfig.getBaseDates().get(0));
                    queryField.getAnalysisConfig().setCompareDatesInterval(dateDiff.intValue());
                    queryField.getAnalysisConfig().setCompareIndex(analysisCompareItemConfig.getCompareIndex());
                    queryField.getAnalysisConfig().setCompareTitle(analysisCompareItemConfig.getTitle());
                    String title = analysisCompareItemConfig.getTitle() + AnalysisCalcType.get(calcType).getTitle();
                    if (queryField.getAnalysisConfig() != null && queryField.getAnalysisConfig().getTargetConfig() != null) {
                        title = queryField.getAnalysisConfig().getTargetConfig().getTargetTitle() + title;
                    }
                    queryField.setTitle(title);

                    String unionId = String.format("%s_%s_%s", measureData.getId(), analysisCompareItemConfig.getCalcMode() + "_" + analysisCompareItemConfig.getCompareIndex(), calcType);
                    if (!unionIdMap.containsKey(unionId)) {
                        compareAnalysisMeasureList.add(queryField);
                        unionIdMap.put(unionId, unionId);
                    }
                }

            }

        }

        addResultMeasure(compareAnalysisMeasureList);

    }

    /**
     * 获取日期字段code
     *
     * @return
     */
    public String getDateFieldCode() {
        String dateFieldCode = "";
        List<QueryField> fields = filter.getFields();
        for (QueryField field : fields) {
            if (field.isCommonDate()) {
                dateFieldCode = field.getCode();
                break;
            }
        }

        return dateFieldCode;
    }

    /**
     * 获取过滤时间
     * @return
     */
    public List<FieldValue> getDateFieldValues() {
        List<FieldValue> values = new ArrayList<>();

        List<QueryField> fields = filter.getFields();
        for (QueryField field : fields) {
            if (field.isCommonDate()) {
                values = FieldUtil.getFilterRealValues(field);
                break;
            }
        }

        return values;
    }

    /**
     * 构建贡献度指标
     * @param measureData
     * @param calcMode
     * @param compareIndex
     * @param dateFieldCode
     * @return
     */
    public QueryField buildCtrQueryField(QueryField measureData,String calcMode,String dateFieldCode,Integer compareIndex) {

        AnalysisCalcMode ctrCalcMode = AnalysisCalcMode.get(calcMode);
        QueryField queryField = buildAnalysisQueryField(measureData, AnalysisCalcMode.CONTRIBUTION_RATE.getCode(), AnalysisCalcType.RATIO.getCode(), dateFieldCode, compareIndex);

        String key = String.format("%s_%s_%s_%s",
                measureData.getCode(), ctrCalcMode.getCode(),
                AnalysisCalcMode.CONTRIBUTION_RATE.getCode(),
                AnalysisCalcType.RATIO.getCode());

        String title = String.format("%s-%s%s", getCalcModeTitle(ctrCalcMode), AnalysisCalcMode.CONTRIBUTION_RATE.getDesc(), AnalysisCalcType.RATIO.getTitle());

        if (AnalysisCalcMode.CUSTOM_COMPARE == ctrCalcMode) {
            key = String.format("%s_%s_%s_%s_%s",
                    measureData.getCode(), ctrCalcMode.getCode(),
                    compareIndex,
                    AnalysisCalcMode.CONTRIBUTION_RATE.getCode(),
                    AnalysisCalcType.RATIO.getCode());

            Optional<AnalysisCompareItemConfig> compareItemConfigOpt = analysis.getCompare().getItems().stream().filter(c -> c.getCompareIndex() == compareIndex).findAny();
            if (compareItemConfigOpt.isPresent()) {
                title = String.format("%s-%s%s", compareItemConfigOpt.get().getTitle(), AnalysisCalcMode.CONTRIBUTION_RATE.getDesc(), AnalysisCalcType.RATIO.getTitle());
            }
        }

        queryField.setId(key);
        queryField.setCode(key);
        queryField.setName(key);

        queryField.setTitle(title);

        AnalysisContributionRateItemConfig itemConfig = (AnalysisContributionRateItemConfig) queryField.getAnalysisConfig();
        itemConfig.setCtrCalcMode(ctrCalcMode.getCode());
        itemConfig.setCompareIndex(compareIndex);
        queryField.setAnalysisConfig(itemConfig);


        /**
         * 设置贡献率格式式：百分比（比率+千分位）
         */
        queryField.getAnalysisConfig().setCalcType(AnalysisCalcType.RATIO.getCode());
        if (queryField.getMeta() != null) {
            queryField.getMeta().setShowFormatExpression("###,###,##0.00");
        }

        return queryField;
    }

    /**
     * 构建分析字段
     *
     * @param measureData   原始指标
     * @param calcMode      计算模式
     * @param calcType      计算类型
     * @param dateFieldCode 时间字段
     * @return
     */
    public QueryField buildAnalysisQueryField(QueryField measureData, String calcMode, String calcType, String dateFieldCode, Integer index) {

        QueryField queryField = new QueryField();

        AnalysisCalcMode analysisCalcMode = AnalysisCalcMode.get(calcMode);
        AnalysisCalcType analysisCalcType = AnalysisCalcType.get(calcType);

        String key = String.format("%s_%s_%s", measureData.getCode(), calcMode, calcType);
        String title = String.format("%s%s", getCalcModeTitle(analysisCalcMode), analysisCalcType.getTitle());

        if (AnalysisCalcMode.CUSTOM_COMPARE == analysisCalcMode) {
            key = String.format("%s_%s_%s", measureData.getCode(), index != null ? calcMode + "_" + index : calcMode, calcType);
            title = String.format("%s%s", index != null ? analysisCalcMode.getDesc() + (index + 1) : analysisCalcMode.getDesc(), analysisCalcType.getTitle());
        }

        queryField.setId(key);
        queryField.setCode(key);
        queryField.setName(key);

        queryField.setTitle(title);
        queryField.setSortType(FieldSortType.NONE);
        queryField.setIsAnalysis(Enabled.YES.getId());

        queryField.setIsResult(true);
        queryField.setQueryArea(QueryArea.Measure);
        queryField.setRawQueryArea(QueryArea.Measure);
        queryField.setDecimalPlaces(measureData.getDecimalPlaces());

        //queryField.setShowOrder(measureData.getShowOrder()+Math.random());

        AnalysisItemConfig analysisConfig = AnalysisConfigFactory.get(calcMode);
        analysisConfig.setDateFieldCode(dateFieldCode);
        analysisConfig.setDateGranularity(settings.getDateGranularity());
        analysisConfig.setMeasureId(measureData.getId());
        analysisConfig.setMeasureCode(measureData.getCode());
        analysisConfig.setCalcMode(calcMode);
        analysisConfig.setCalcType(calcType);
        analysisConfig.setAggExpressionType(measureData.getAggExpressionType());
        if (measureData.getAnalysisConfig() != null && measureData.getAnalysisConfig().getTargetConfig() != null) {
            analysisConfig.setTargetConfig(measureData.getAnalysisConfig().getTargetConfig());
            analysisConfig.setMeasureCode(measureData.getAnalysisConfig().getMeasureCode());
            analysisConfig.setMeasureId(measureData.getAnalysisConfig().getMeasureId());
        }
        queryField.setAnalysisConfig(analysisConfig);
        if (measureData.isAnalysisCalc()) {
            queryField.setCustomFieldConfigure(measureData.getCustomFieldConfigure());
        }
        return queryField;
    }

    /**
     * 分析指标加入结果集
     *
     * @param analysisMeasureList
     */
    public void addResultMeasure(List<QueryField> analysisMeasureList) {

        for (QueryField queryField : analysisMeasureList) {
            if (Enabled.NO.getId().equals(queryField.getIsAnalysis())) {
                result.getFields().add(queryField);
                result.getMeasures().add(queryField);
                continue;
            }

            AnalysisItemConfig analysisConfig = queryField.getAnalysisConfig();
            QueryField measureData = result.getFieldById(analysisConfig.getMeasureId());
            if (measureData == null) {
                measureData = result.getFieldByCode(analysisConfig.getMeasureCode());
                if (measureData == null) {
                    throw new BIException(("字段不存在:" + queryField.getTitle()));
                }
            }
            MetaField meta = measureData.getMeta();

            MetaField copy = meta.clone();
            copy.setId(queryField.getId());
            copy.setName(queryField.getName());
            copy.setCode(queryField.getCode());
            copy.setTitle(queryField.getTitle());

            FieldUtil.initializeQueryField(copy, queryField);

            // 若分析指标编码为空则重新设置分析配置的指标编码、id、code、name
            if (BIUtil.isEmpty(analysisConfig.getMeasureCode())) {
                analysisConfig.setMeasureCode(meta.getCode());
                queryField.setId(meta.getCode() + queryField.getId());
                queryField.setCode(queryField.getId());
                queryField.setName(queryField.getId());
            }

            String analysisMeasuresId = queryField.getAnalysisConfig().getMeasureId();
            AnalysisCalcMode calcMode = AnalysisCalcMode.get(queryField.getAnalysisConfig().getCalcMode());


            int idx = 0;

            //同一指标id 最后的下标
            int measureIdlastIdx = 0;
            for (int i = 0; i < result.getMeasures().size(); i++) {
                QueryField qf = result.getMeasures().get(i);
                if (qf.getId().equalsIgnoreCase(analysisMeasuresId) ||
                        (Enabled.value(qf.getIsAnalysis()) && analysisMeasuresId.equalsIgnoreCase(qf.getAnalysisConfig().getMeasureId()))) {

                    measureIdlastIdx = i;
                    AnalysisCalcMode qfCalcMode = AnalysisCalcMode.get(qf.getAnalysisConfig().getCalcMode());

                    //贡献率放在对应的计算方式后面
                    if (AnalysisCalcMode.CONTRIBUTION_RATE == calcMode && Enabled.value(qf.getIsAnalysis())) {

                        AnalysisContributionRateItemConfig ctrItemConfig = (AnalysisContributionRateItemConfig) queryField.getAnalysisConfig();

                        //自定义对比需要比较索引
                        if (AnalysisCalcMode.CUSTOM_COMPARE == qfCalcMode) {
                            if (AnalysisCalcMode.get(ctrItemConfig.getCtrCalcMode()) == qfCalcMode && ctrItemConfig.getCompareIndex().equals(qf.getAnalysisConfig().getCompareIndex())) {
                                idx = i;
                            }

                        } else {
                            if (AnalysisCalcMode.get(ctrItemConfig.getCtrCalcMode()) == qfCalcMode) {
                                idx = i;
                            }
                        }

                    } else if (AnalysisCalcMode.ZB_THB == calcMode && Enabled.value(qf.getIsAnalysis())) {


                        AnalysisZbThbItemConfig zbThbItemConfig = (AnalysisZbThbItemConfig)queryField.getAnalysisConfig();
                        String zbThbCalcMode = zbThbItemConfig.getZbCalcMode();

                        //占比同环比 需要对比占比类型。
                        //例：占"列小计"的环比实际值 ，放在占"列小计"后面
                        if(qfCalcMode.getCode().equalsIgnoreCase(zbThbCalcMode)){
                            idx = i;
                        }

                        if (qfCalcMode == AnalysisCalcMode.ZB_THB) {

                            AnalysisZbThbItemConfig qfZbThbItemConfig = (AnalysisZbThbItemConfig)qf.getAnalysisConfig();

                            if (zbThbItemConfig != null && qfZbThbItemConfig != null) {
                                if (zbThbItemConfig.getZbCalcMode().equalsIgnoreCase(qfZbThbItemConfig.getZbCalcMode())) {
                                    idx = i;
                                }

                            }
                        }

                    } else {
                        if (qfCalcMode == calcMode) {
                            idx = i;
                        }
                    }
                }

            }

            //如果没有相关的计算方式的指标，放到统一指标id的最后面
            if(idx == 0) {
                idx = measureIdlastIdx;
            }

            //result.add(queryField, QueryArea.Measure);
            if(!result.getMeasures().contains(queryField)) {
                result.getMeasures().add(idx + 1, queryField);
            }

            //找到指标在所有字段里面的位置
            int fieldIdx = 0;
            for (int i = 0; i < result.getFields().size(); i++) {
                if (result.getFields().get(i).getId().equalsIgnoreCase(result.getMeasures().get(idx).getId())) {
                    fieldIdx = i;
                    break;
                }
            }
            //保证field顺序
            if(!result.getFields().contains(queryField)) {
                result.getFields().add(fieldIdx + 1, queryField);
            }
        }

    }

    /**
     * 获取分析计算方式的 表头标题展示
     * @param calcMode
     * @return
     */
    public String getCalcModeTitle(AnalysisCalcMode calcMode) {

        String result = calcMode.getDesc();
//        if (AnalysisCalcMode.TB_YEAR == calcMode
//                || AnalysisCalcMode.TB_YEAR_2 == calcMode
//                || AnalysisCalcMode.TB_YEAR_3 == calcMode
//                || AnalysisCalcMode.TB_YEAR_WEEK == calcMode
//                || AnalysisCalcMode.TB_YEAR_WEEK_2 == calcMode
//                || AnalysisCalcMode.TB_YEAR_WEEK_3 == calcMode) {
//
//            List<FieldValue> values = getDateFieldValues();
//            if (CollUtil.isEmpty(values)) {
//                return calcMode.getDesc();
//            }
//            /*
//            //查询的年份
//            Integer queryYear = Integer.parseInt(values.get(values.size() - 1).getId().substring(0, 4));
//            Integer offset = 0;
//            switch (calcMode) {
//                case TB_YEAR:
//                case TB_YEAR_WEEK:
//                    offset = 1;
//                    break;
//                case TB_YEAR_2:
//                case TB_YEAR_WEEK_2:
//                    offset = 2;
//                    break;
//                case TB_YEAR_3:
//                case TB_YEAR_WEEK_3:
//                    offset = 3;
//                    break;
//            }
//            result = String.format(calcMode.getDesc(), (queryYear - offset));
//             */
//            QueryField dateField = config.getFilterCommonDateField();
//            boolean showLunarDate = Enabled.isTrue(this.settings.getShowLunarDate())
//                    && (dateField != null)
//                    && DateGranularity.get(dateField.getQueryDateGranularity()) == DateGranularity.DAY;
//            String yearTitle = showLunarDate ? "农历" : "";
//            result = String.format(calcMode.getDesc(), yearTitle);
//        }

        return result;
    }

}
