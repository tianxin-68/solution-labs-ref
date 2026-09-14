package com.bi.queryer.ssm.engine.config.ui.normalizer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.custom.CustomFieldExpressionIdMapping;
import com.bi.queryer.ssm.custom.CustomFieldType;
import com.bi.queryer.ssm.engine.analysis.cfg.compare.AnalysisCompareConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.compare.AnalysisCompareItemConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.thb.AnalysisThbConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.thb.AnalysisThbItemConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.zb.AnalysisZbConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.zb.AnalysisZbItemConfig;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.config.ui.UIQueryAnalysis;
import com.bi.queryer.ssm.engine.config.ui.UIQueryConfigure;
import com.bi.queryer.ssm.engine.config.ui.UIQueryField;
import com.bi.queryer.ssm.meta.*;
import com.bi.queryer.ssm.query.template.enums.DataTypeEnum;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.ssm.util.TableDataUpdateTimeUtil;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.period.DateUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 实时数据集，字段需要选择最优的表
 * 1. 实时指标累计值查询，优先从数据更新时间最新的表查询
 */
public class UIRtDatasetNormalizer extends UIBaseNormalizer{
    public UIRtDatasetNormalizer(UIQueryConfigure uiQueryConfigure) {
        super(uiQueryConfigure);
    }

    private Map<String,String> replaceFieldIdMap = new HashMap<>();
    private String datasetId;
    private List<String> queryDimCodeList = new ArrayList<>();

    @Override
    public void normalize() {

        datasetId = uiQueryConfigure.getSetting().getDatasetId();
        MetaDataset dataset = SSDMetaCacheManager.getDataset(datasetId);
        if(dataset == null){
            return;
        }

        //非实时数据集不处理
        if(DataTypeEnum.REAL_TIME != DataTypeEnum.codeOf(dataset.getDatasetType())){
            return;
        }

        //获取数据集下面所有事实表的数据更新时间
        MetaDatasetTable metaDatasetTable = SSDMetaCacheManager.getMetaDatasetTableById(datasetId);
        List<MetaTable> metaTableList = new ArrayList<>();
        metaTableList.addAll(metaDatasetTable.getFactTables().values());
        uiQueryConfigure.getSetting().setTableDataUpdateTimeMap(TableDataUpdateTimeUtil.buildTableDataUpdateTime(metaTableList));

        //构建查询的维度编码集合
        buildQueryDimCodeList();

        for(UIQueryField measureField : uiQueryConfigure.getResult().getMeasures()){

            if (measureField.isLodField()) {
                normalizeLodField(measureField);
                continue;
            }

            if (isCalcField(measureField)) {
                normalizeCalcField(measureField);
                continue;
            }

            normalizeCommonField(measureField,queryDimCodeList);
        }

        modify();

    }

    /**
     * 构建查询的维度编码集合
     */
    public void buildQueryDimCodeList() {
        List<UIQueryField> fieldList = new ArrayList<>();
        fieldList.addAll(uiQueryConfigure.getResult().getRowDimensions());
        fieldList.addAll(uiQueryConfigure.getResult().getColDimensions());
        fieldList.addAll(uiQueryConfigure.getFilter());

        List<String> dimCodeList = new ArrayList<>();
        for(UIQueryField field : fieldList) {

            List<String> fieldIdList = new ArrayList<>();
            fieldIdList.add(field.getId());

            if (isCalcField(field)) {
                fieldIdList = this.getCalcRefFieldIdList(field);
            }

            for (String fieldId : fieldIdList) {
                MetaField metaField = SSDMetaCacheManager.getField(fieldId);
                if (metaField == null) {
                    continue;
                }

                if (Enabled.value(metaField.getIsMeasure())) {
                    continue;
                }
                dimCodeList.add(metaField.getCode());
            }

        }

        dimCodeList = dimCodeList.stream().distinct().collect(Collectors.toList());
        queryDimCodeList = dimCodeList;
    }

    /**
     * 普通字段处理
     * 1 只能从一张表出，不处理
     * 2 在维度都支持的情况下，优先从数据更新时间最新的表查询
     * @param field
     */
    public void normalizeCommonField(UIQueryField field,List<String> dimCodeList) {

        String measureCode = field.getCode();
        String measureFieldId = field.getId();
        if(field.isLodField()) {
            measureFieldId = field.getId().replace(CustomFieldType.LOD.getIdentifier(), "");
            MetaField metaField = SSDMetaCacheManager.getField(measureFieldId);
            if (metaField != null) {
                measureCode = metaField.getCode();
            }
        }
        List<RtTableInfo> rtTableInfoList = SSDMetaCacheManager.getFieldRtTableByCode(datasetId, measureCode);

        if (CollUtil.isEmpty(rtTableInfoList)) {
            return;
        }

        if (rtTableInfoList.size() == 1) {
            return;
        }

        String replaceFieldId = null;
        String lastDataUpdateTime = null;
        for (RtTableInfo rtTableInfo : rtTableInfoList) {

            //判断维度是否都适配
            if (!SSDUtil.isSuperset(rtTableInfo.getDimCodeList(), dimCodeList)) {
                continue;
            }
            
            //判断数据更新时间
            String tableDataUpdateTime = uiQueryConfigure.getSetting().getTableDataUpdateTimeMap().get(rtTableInfo.getTableFullName());
            lastDataUpdateTime = DateUtil.getMaxDate(lastDataUpdateTime, tableDataUpdateTime);

            //rtTableInfo的数据更新时间等于lastDataUpdateTime，并且不是当前字段，则替换当前字段
            if (tableDataUpdateTime.equalsIgnoreCase(lastDataUpdateTime) && !measureFieldId.equalsIgnoreCase(rtTableInfo.getFieldId())) {
                replaceFieldId = rtTableInfo.getFieldId();
                if(field.isLodField()){
                    replaceFieldId = CustomFieldType.LOD.getIdentifier() + replaceFieldId;
                }
            }

        }

        if (StrUtil.isNotEmpty(replaceFieldId)) {
            replaceFieldIdMap.put(field.getId(), replaceFieldId);
        }

    }


    /**
     * lod字段处理
     * @param field
     */
    public void normalizeLodField(UIQueryField field) {
        List<QueryField> lodDimensionList = field.getCustomFieldConfigure().getLodConfig().getDimensionList();

        List<String> lodDimCodeList = new ArrayList<>();
        for (QueryField dimField : lodDimensionList) {
            if (StrUtil.isEmpty(dimField.getCode())) {
                // 编码为空，通过id从查询配置获取
                Optional<UIQueryField> opt = uiQueryConfigure.getResult().getAllFields().stream().filter(v -> v.getId().equals(dimField.getId())).findAny();
                if (!opt.isPresent()) {
                    continue;
                }

                List<String> fieldIdList = this.getCalcRefFieldIdList(opt.get());

                for (String calcFieldId : fieldIdList) {
                    MetaField metaField = SSDMetaCacheManager.getField(calcFieldId);
                    if (metaField == null) {
                        continue;
                    }
                    lodDimCodeList.add(metaField.getCode());
                }

                continue;
            }

            lodDimCodeList.add(dimField.getCode());
        }

        lodDimCodeList = lodDimCodeList.stream().distinct().collect(Collectors.toList());
        normalizeCommonField(field, lodDimCodeList);
    }

    /**
     * 计算字段处理
     * @param field
     */
    public void normalizeCalcField(UIQueryField field) {
        List<String> calcRefFieldIdList = this.getCalcRefFieldIdList(field);

        //分析计算字段
        //需要从expressionIdMapping中获取
        if(field.isAnalysisCalc()){
            for(CustomFieldExpressionIdMapping expressionIdMapping : field.getCustomFieldConfigure().getExpressionIdMapping()){
                if(expressionIdMapping.getAnalysisItemConfig() == null){
                    continue;
                }
                calcRefFieldIdList.add(expressionIdMapping.getAnalysisItemConfig().getMeasureId());
            }
        }

        //去重
        calcRefFieldIdList = calcRefFieldIdList.stream().distinct().collect(Collectors.toList());
        for (String calcRefFieldId : calcRefFieldIdList) {
            Optional<UIQueryField> uiQueryFieldOpt = uiQueryConfigure.getResult().getMeasures().stream()
                    .filter(measureField -> measureField.getId().equalsIgnoreCase(calcRefFieldId)).findAny();

            UIQueryField calcRefField = null;
            if(uiQueryFieldOpt.isPresent()){
                calcRefField = uiQueryFieldOpt.get();
            }

            if (!uiQueryFieldOpt.isPresent()) {

                //指标区域没有，从元数据查询一次
                MetaField calcRefMetaField = SSDMetaCacheManager.getField(calcRefFieldId);
                if(calcRefMetaField == null){
                    continue;
                }else{
                    calcRefField = new UIQueryField();
                    calcRefField.setId(calcRefMetaField.getId());
                }

            }

            if (calcRefField.isLodField()) {
                normalizeLodField(calcRefField);
            } else {
                normalizeCommonField(calcRefField, queryDimCodeList);
            }
        }
    }

    /**
     * 执行变更
     */
    public void modify() {
        if (replaceFieldIdMap.size() == 0) {
            return;
        }

        for (String rawFieldId : replaceFieldIdMap.keySet()) {
            for (UIQueryField field : uiQueryConfigure.getResult().getMeasures()) {
                if(field.isLodField()){
                    if(field.getId().equalsIgnoreCase(rawFieldId)){
                        String lodReplaceFieldId = replaceFieldIdMap.get(rawFieldId).replace(CustomFieldType.LOD.getIdentifier(),"");
                        field.getCustomFieldConfigure().setExpression(String.format("[%s]", lodReplaceFieldId));
                        //替换计算字段表达式id映射
                        field.getCustomFieldConfigure().getExpressionIdMapping().forEach(mapping -> {
                            if (mapping.getId().equalsIgnoreCase(rawFieldId)) {
                                mapping.setId(lodReplaceFieldId);
                            }
                        });
                        field.getCustomFieldConfigure().getLodConfig().setMeasureId(lodReplaceFieldId);
                    }
                }else if (isCalcField(field)) {

                    //lod字段id不变，lod的计算字段不替换lod的id
                    if(rawFieldId.startsWith(CustomFieldType.LOD.getIdentifier())){
                        continue;
                    }

                    //替换计算字段表达式
                    String expression = field.getCustomFieldConfigure().getExpression()
                            .replaceAll(rawFieldId, replaceFieldIdMap.get(rawFieldId));
                    field.getCustomFieldConfigure().setExpression(expression);

                    //替换计算字段表达式id映射
                    field.getCustomFieldConfigure().getExpressionIdMapping().forEach(mapping -> {
                        if (mapping.getId().equalsIgnoreCase(rawFieldId)) {
                            mapping.setId(replaceFieldIdMap.get(rawFieldId));
                        }
                        //替换分析计算字段的measureId
                        if(mapping.getAnalysisItemConfig() != null && mapping.getAnalysisItemConfig().getMeasureId().equalsIgnoreCase(rawFieldId)){
                            mapping.getAnalysisItemConfig().setMeasureId(replaceFieldIdMap.get(rawFieldId));
                        }
                    });
                } else {
                    if (field.getId().equalsIgnoreCase(rawFieldId)) {
                        field.setId(replaceFieldIdMap.get(rawFieldId));
                    }
                }
            }
        }

        UIQueryAnalysis analysis = uiQueryConfigure.getAnalysis();
        if (analysis == null) {
            return;
        }

        AnalysisThbConfig thbConfig = analysis.getThb();
        if (thbConfig != null && thbConfig.isActive()) {
            List<AnalysisThbItemConfig> items = thbConfig.getItems();
            for (AnalysisThbItemConfig itemConfig : items) {
                for (String rawFieldId : replaceFieldIdMap.keySet()) {
                    if(rawFieldId.startsWith(CustomFieldType.LOD.getIdentifier())){
                        continue;
                    }
                    if (itemConfig.getMeasureIdList().remove(rawFieldId)) {
                        itemConfig.getMeasureIdList().add(replaceFieldIdMap.get(rawFieldId));
                    }
                }
            }
        }

        AnalysisZbConfig zbConfig = analysis.getZb();
        if (zbConfig != null && zbConfig.isActive()) {
            List<AnalysisZbItemConfig> items = zbConfig.getItems();
            for (AnalysisZbItemConfig itemConfig : items) {
                for (String rawFieldId : replaceFieldIdMap.keySet()) {
                    if(rawFieldId.startsWith(CustomFieldType.LOD.getIdentifier())){
                        continue;
                    }
                    if (itemConfig.getMeasureIdList().remove(rawFieldId)) {
                        itemConfig.getMeasureIdList().add(replaceFieldIdMap.get(rawFieldId));
                    }
                }
            }
        }

        AnalysisCompareConfig compareConfig = analysis.getCompare();
        if (compareConfig != null && compareConfig.isActive()) {
            List<AnalysisCompareItemConfig> items = compareConfig.getItems();
            for (AnalysisCompareItemConfig itemConfig : items) {
                for (String rawFieldId : replaceFieldIdMap.keySet()) {
                    if(rawFieldId.startsWith(CustomFieldType.LOD.getIdentifier())){
                        continue;
                    }
                    if (itemConfig.getMeasureIdList().remove(rawFieldId)) {
                        itemConfig.getMeasureIdList().add(replaceFieldIdMap.get(rawFieldId));
                    }
                }
            }
        }
    }

}
