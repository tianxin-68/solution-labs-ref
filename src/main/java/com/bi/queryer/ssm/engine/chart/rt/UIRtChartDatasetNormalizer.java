package com.bi.queryer.ssm.engine.chart.rt;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.custom.CustomFieldConfigure;
import com.bi.queryer.ssm.custom.CustomFieldExpressionIdMapping;
import com.bi.queryer.ssm.custom.CustomFieldParser;
import com.bi.queryer.ssm.custom.CustomFieldType;
import com.bi.queryer.ssm.engine.chart.vo.ChartQueryConfigVO;
import com.bi.queryer.ssm.engine.config.QuerySettings;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.config.ui.UIQueryField;
import com.bi.queryer.ssm.meta.*;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.ssm.util.TableDataUpdateTimeUtil;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.period.DateUtil;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class UIRtChartDatasetNormalizer {

    private ChartQueryConfigVO chartQueryConfigVO;
    private Map<String,String> replaceFieldIdMap = new HashMap<>();
    private String datasetId;
    private List<String> queryDimCodeList = new ArrayList<>();

    public UIRtChartDatasetNormalizer(ChartQueryConfigVO vo){
        this.chartQueryConfigVO = vo;
    }

    public void normalize(ChartQueryConfigVO vo) {

        QuerySettings settings = vo.getSetting();
        datasetId = settings.getDatasetId();

        //非实时数据集不处理
        if (!vo.getSetting().isRtDataset()) {
            return;
        }

        //获取数据集下面所有事实表的数据更新时间
        MetaDatasetTable metaDatasetTable = SSDMetaCacheManager.getMetaDatasetTableById(settings.getDatasetId());
        List<MetaTable> metaTableList = new ArrayList<>();
        metaTableList.addAll(metaDatasetTable.getFactTables().values());
        settings.setTableDataUpdateTimeMap(TableDataUpdateTimeUtil.buildTableDataUpdateTime(metaTableList));

        //构建查询的维度编码集合
        buildQueryDimCodeList();

        for(QueryField measureField : this.chartQueryConfigVO.getMeasures()){

            if (measureField.isLodField()) {
                normalizeLodField(measureField);
                continue;
            }

            if (measureField.isCalc()) {
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
        List<QueryField> fieldList = new ArrayList<>();
        fieldList.addAll(chartQueryConfigVO.getDimensions());
        fieldList.addAll(chartQueryConfigVO.getFilters());
        fieldList.addAll(chartQueryConfigVO.getGlobalFilters());

        List<String> dimCodeList = new ArrayList<>();
        for(QueryField field : fieldList) {

            List<String> fieldIdList = new ArrayList<>();
            fieldIdList.add(field.getId());

            if (field.isCalc()) {
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


    protected List<String> getCalcRefFieldIdList(QueryField calcField){
        List<String> refFieldIdList = new ArrayList<>();
        if(!calcField.isCalc()){
            return refFieldIdList;
        }

        CustomFieldConfigure customFieldConfigure = calcField.getCustomFieldConfigure();
        String expression = customFieldConfigure.getExpression();
        Pattern pattern = Pattern.compile(CustomFieldParser.expressionPattern);
        Matcher matcher = pattern.matcher(expression);
        while (matcher.find()){
            String fieldId = matcher.group();
            refFieldIdList.add(fieldId);
        }
        return refFieldIdList;
    }

    /**
     * 普通字段处理
     * 1 只能从一张表出，不处理
     * 2 在维度都支持的情况下，优先从数据更新时间最新的表查询
     * @param field
     */
    public void normalizeCommonField(QueryField field,List<String> dimCodeList) {

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

            //趋势图需要判断数据切片粒度
            String dataSliceGranularity = chartQueryConfigVO.getSetting().getDataSliceGranularity();
            if(StrUtil.isNotEmpty(dataSliceGranularity)){
                if(!dataSliceGranularity.equalsIgnoreCase(rtTableInfo.getDataSliceGranularity())){
                    continue;
                }
            }

            //判断数据更新时间
            String tableDataUpdateTime = chartQueryConfigVO.getSetting().getTableDataUpdateTimeMap().get(rtTableInfo.getTableFullName());
            lastDataUpdateTime = DateUtil.getMaxDate(lastDataUpdateTime, tableDataUpdateTime);

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
    public void normalizeLodField(QueryField field){
        List<QueryField> lodDimensionList =  field.getCustomFieldConfigure().getLodConfig().getDimensionList();
        List<String> lodDimCodeList = new ArrayList<>();
        for (QueryField dimField : lodDimensionList) {
            if (StrUtil.isEmpty(dimField.getCode())) {
                // 编码为空，通过id从查询配置获取
                Optional<QueryField> opt = this.chartQueryConfigVO.getDimensions().stream().filter(v -> v.getId().equals(dimField.getId())).findAny();
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
        normalizeCommonField(field,lodDimCodeList);
    }

    /**
     * 计算字段处理
     * @param field
     */
    public void normalizeCalcField(QueryField field) {
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
            Optional<QueryField> queryFieldOpt = chartQueryConfigVO.getMeasures().stream()
                    .filter(measureField -> measureField.getId().equalsIgnoreCase(calcRefFieldId)).findAny();

            QueryField calcRefField = null;
            if(queryFieldOpt.isPresent()){
                calcRefField = queryFieldOpt.get();
            }

            if (!queryFieldOpt.isPresent()) {

                //指标区域没有，从元数据查询一次
                MetaField calcRefMetaField = SSDMetaCacheManager.getField(calcRefFieldId);
                if(calcRefMetaField == null){
                    continue;
                }else{
                    calcRefField = new QueryField();
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
            for (QueryField field : chartQueryConfigVO.getMeasures()) {
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
                }else if(field.isCalc()) {

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
    }

}
