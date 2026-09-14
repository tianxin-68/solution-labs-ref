package com.bi.queryer.ssm.query.rt;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.custom.CustomFieldExpressionIdMapping;
import com.bi.queryer.ssm.custom.CustomFieldType;
import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;
import com.bi.queryer.ssm.enums.DataSliceGranularity;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.meta.MetricSupportDataSliceGranularityRsp;
import com.bi.queryer.ssm.meta.RtTableInfo;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Scope("prototype")
public class SSMRTQueryService {

    /**
     * 获取指标支持的切片粒度
     * @param createEngine
     * @return
     */
    public List<MetricSupportDataSliceGranularityRsp> getMetricSupportDataSliceGranularityList(QueryEngine createEngine) {

        List<MetricSupportDataSliceGranularityRsp> rspList = new ArrayList<>();

        try {

            QueryConfigure config = createEngine.getConfig();
            String datasetId = config.getSettings().getDatasetId();

            //非实时数据集不处理
            if (!config.getSettings().isRtDataset()) {
                return rspList;
            }

            //是否是小时数据集
            boolean isNearRealTimeDataset = SSDUtil.isNearRealTimeDataset(datasetId);

            //获取查询需要支持的粒度
            List<String> dimCodeList = buildQueryDimCodeList(config);
            for (QueryField measure : config.getResult().getMeasures()) {

                //分析指标不处理
                if (Enabled.value(measure.getIsAnalysis())) {
                    continue;
                }

                //不显示的指标不处理
                if(!Enabled.value(measure.getIsShow())){
                    continue;
                }

                String measureCode = measure.getCode();
                List<String> supportDataSliceGranularityList = new ArrayList<>();

                //前端配置的计算指标使用id当做标识返回，因为后台修改了计算字段的编码
                if (measure.isLodField()) {
                    measureCode = measure.getId();

                    if (CustomFieldType.LOD_CALC == CustomFieldType.get(measure.getCustomFieldConfigure().getType())) {
                        supportDataSliceGranularityList = getCalcMeasureSupportDataSliceGranularityList(datasetId, measure, dimCodeList,config);
                    }else{
                        supportDataSliceGranularityList = getLodMeasureSupportDataSliceGranularityList(datasetId, measure,config);
                    }

                }else if(measure.isCustom()) {
                    measureCode = measure.getId();
                    supportDataSliceGranularityList = getCalcMeasureSupportDataSliceGranularityList(datasetId, measure, dimCodeList,config);
                }else{
                    supportDataSliceGranularityList = getCommonMeasureSupportDataSliceGranularityList(datasetId, measure, dimCodeList);
                }

                MetricSupportDataSliceGranularityRsp rsp = new MetricSupportDataSliceGranularityRsp();
                rsp.setMetricCode(measureCode);

                //小时数据集，默认支持1h
                if(isNearRealTimeDataset){
                    if(!supportDataSliceGranularityList.contains(DataSliceGranularity.ONE_HOUR.getCode())){
                        supportDataSliceGranularityList.add(DataSliceGranularity.ONE_HOUR.getCode());
                    }
                }

                //切片粒度排序
                supportDataSliceGranularityList.sort(new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        DataSliceGranularity sliceGranularity1 = DataSliceGranularity.get(o1);
                        DataSliceGranularity sliceGranularity2 = DataSliceGranularity.get(o2);
                        return sliceGranularity1.getSortId().compareTo(sliceGranularity2.getSortId());
                    }
                });

                rsp.setDataSliceGranularityList(supportDataSliceGranularityList);
                rspList.add(rsp);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return rspList;
    }

    /**
     * 构建查询的维度列表
     * 计算维度拆开成原子维度
     * @param config
     * @return
     */
    public List<String> buildQueryDimCodeList(QueryConfigure config) {

        //List<QueryField> queryFieldList = config.getAllFields();

        //从行维度+列维度+过滤维度
        List<QueryField> queryFieldList = new ArrayList<>();
        queryFieldList.addAll(config.getResult().getRowDimensions());
        queryFieldList.addAll(config.getResult().getColDimensions());
        queryFieldList.addAll(config.getFilter().getFields());

        List<String> dimCodeList = new ArrayList<>();
        for (QueryField field : queryFieldList) {

            if (field.isMeasure()) {
                continue;
            }

            List<String> fieldIdList = new ArrayList<>();
            fieldIdList.add(field.getId());

            if (field.isCalc()) {
                fieldIdList = field.getCusCalcDependFields().stream()
                        .map(v -> v.getId()).collect(Collectors.toList());
            }

            for (String fieldId : fieldIdList) {
                MetaField metaField = SSDMetaCacheManager.getField(fieldId);
                if (metaField == null) {
                    continue;
                }

                if (Enabled.value(metaField.getIsMeasure())) {
                    continue;
                }

                //没有白皮书编码不处理,并且不为日期
                if (StrUtil.isEmpty(metaField.getKpiNo()) && !BIConsts.DATE_CODE.equalsIgnoreCase(metaField.getCode())) {
                    continue;
                }

                //内置的行级权限不处理
                //系统自动添加的行级权限
                if (field.isAppend() && field.getIsFilter() && !field.getIsResult()) {
                    continue;
                }

                dimCodeList.add(metaField.getCode());
            }

        }

        dimCodeList = dimCodeList.stream().distinct().collect(Collectors.toList());
        return dimCodeList;
    }

    /**
     * 获取通用指标支持的切片粒度
     * @param measure
     * @param dimCodeList
     * @return
     */
    public List<String> getCommonMeasureSupportDataSliceGranularityList(String datasetId,QueryField measure, List<String> dimCodeList) {

        List<String> supportDataSliceGranularityList = new ArrayList<>();
        String measureCode = measure.getCode();
        if(measure.isLodField()){
           String measureId = measure.getId().replace(CustomFieldType.LOD.getIdentifier(),"");
            MetaField metaField = SSDMetaCacheManager.getField(measureId);
            if(measure != null){
                measureCode = metaField.getCode();
            }
        }

        List<RtTableInfo> rtTableInfoList = SSDMetaCacheManager.getFieldRtTableByCode(datasetId, measureCode);

        if(CollUtil.isEmpty(rtTableInfoList)){
            return supportDataSliceGranularityList;
        }

        for (RtTableInfo rtTableInfo : rtTableInfoList) {
            //判断维度是否都适配
            if (!SSDUtil.isSuperset(rtTableInfo.getDimCodeList(), dimCodeList)) {
                continue;
            }

            supportDataSliceGranularityList.add(rtTableInfo.getDataSliceGranularity());
        }

        return supportDataSliceGranularityList;
    }

    /**
     * 获取Lod指标支持的切片粒度
     * @param datasetId
     * @param measure
     * @return
     */
    public List<String> getLodMeasureSupportDataSliceGranularityList(String datasetId,QueryField measure,QueryConfigure config) {
        List<QueryField> lodDimensionList =  measure.getCustomFieldConfigure().getLodConfig().getDimensionList();

        List<String> lodDimCodeList = new ArrayList<>();
        for(QueryField dimField : lodDimensionList) {
            if (StrUtil.isEmpty(dimField.getCode())) {
                // 编码为空，通过id从查询配置获取
                Optional<QueryField> opt = config.getAllFields().stream().filter(v -> v.getId().equals(dimField.getId())).findAny();
                if (!opt.isPresent()) {
                    continue;
                }

                if (CollUtil.isEmpty(opt.get().getCusCalcDependFields())) {
                    continue;
                }

                for (QueryField calcField : opt.get().getCusCalcDependFields()) {
                    lodDimCodeList.add(calcField.getCode());
                }

                continue;
            }

            lodDimCodeList.add(dimField.getCode());
        }

        lodDimCodeList = lodDimCodeList.stream().distinct().collect(Collectors.toList());

        return getCommonMeasureSupportDataSliceGranularityList(datasetId, measure, lodDimCodeList);
    }

    /**
     * 获取计算指标支持的切片粒度
     * 依赖字段支持粒度的交集
     * @param datasetId
     * @param measure
     * @return
     */
    public List<String> getCalcMeasureSupportDataSliceGranularityList(String datasetId,QueryField measure,List<String> dimCodeList,QueryConfigure config) {

        List<String> supportDataSliceGranularityList = new ArrayList<>();

        List<List<String>> collections = new ArrayList<>();

        List<QueryField> dependFields = new ArrayList<>();

        //lod的计算字段从ExpressionIdMapping字段中获取
        if(measure.isLodField()){
            List<CustomFieldExpressionIdMapping> expressionIdMappings =  measure.getCustomFieldConfigure().getExpressionIdMapping();
            for(CustomFieldExpressionIdMapping mapping : expressionIdMappings){
               Optional<QueryField> opt = config.getResult().getMeasures().stream().filter(f->f.getId().equals(mapping.getId())).findAny();
                if(opt.isPresent()){
                    dependFields.add(opt.get());
                }
            }
        }else{
            dependFields.addAll(measure.getCusCalcDependFields());
        }

        //使用cusCalcDependFields,只需要解析一层。
        //不能使用calcAtomFields，因为calcAtomFields会将后台配置的计算字段拆成原子字段，影响粒度判断
        for(QueryField atomField : dependFields){

            List<String> sliceGranularityList = new ArrayList<>();
            if(atomField.isLodField()){
                sliceGranularityList = getLodMeasureSupportDataSliceGranularityList(datasetId, atomField,config);
            }else{
                sliceGranularityList = getCommonMeasureSupportDataSliceGranularityList(datasetId, atomField, dimCodeList);
            }

            //存在一个依赖字段没有支持的粒度，则返回空
            if(CollUtil.isEmpty(sliceGranularityList)){
                return new ArrayList<>();
            }

            collections.add(sliceGranularityList);
        }

        //求交集
        supportDataSliceGranularityList.addAll(SSDUtil.getIntersection(collections));
        return supportDataSliceGranularityList;
    }

}
