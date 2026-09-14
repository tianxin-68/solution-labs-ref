package com.bi.queryer.ssm.custom;

import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.enums.FieldType;
import com.bi.queryer.ssm.enums.ShowFormatExpressionType;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.ssm.util.FieldUtil;
import com.bi.queryer.sys.db.DataType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.Guid;
import com.bi.queryer.util.StringUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 自定义字段管理类
 */
public abstract class CustomFieldManager {
    public static MetaField getMetaField(QueryField queryField,boolean isBackEndCalculation){
        if(queryField == null || queryField.getCustomFieldConfigure() == null || queryField.getCustomFieldConfigure().isEmpty()) {
            return null;
        }
        CustomFieldConfigure cfg = queryField.getCustomFieldConfigure();
        String fieldId = queryField.getId();
        if(BIUtil.isEmpty(fieldId)) {
            //throw new SSDException("字段[" + queryField.getTitle() + "]id为空");
            fieldId = CustomFieldType.get(cfg.getType()).getIdentifier() + Guid.id();
            queryField.setId(fieldId);
        }
        MetaField mf = SSDMetaCacheManager.getField(fieldId);
        // 若缓存中有则直接返回
        if(mf != null && !isBackEndCalculation) {
            return mf;
        }

        //计算字段使用的跨模型指标，需要将跨模型计算字段的表达式展开
        String expression = FieldUtil.getExtendCalcExpression(queryField.getCustomFieldConfigure().getExpression());
        queryField.getCustomFieldConfigure().setExpression( expression);

        // 自定义字段的原子字段
        List<QueryField> atomFields = CustomFieldParser.parseExpression(cfg.getExpression());
        String factTableId = null;
        String dimTableId = null;
        if(BIUtil.isNotEmpty(atomFields)) {
            for(QueryField f : atomFields){
                if(BIUtil.isEmpty(factTableId)) {
                    factTableId = f.getMeta().getFactTableId();
                }
                if(BIUtil.isEmpty(dimTableId)) {
                    dimTableId = f.getMeta().getDimTableId();
                }
            }
        }else {
            if (mf != null) {
                factTableId = mf.getFactTableId();
            }
        }

        // 没有则创建元字段，并加入本地缓存
        mf = new MetaField();
        //String fieldId = queryField.getId();
        mf.setId(fieldId);
        mf.setTitle(queryField.getTitle());
        //mf.setCode(BIConsts.Custom_Field_Name_Suffix + queryField.getQueryArea().getShortCode() + "_" + (queryField.getShowOrder() + "").replaceAll("[\\.\\-]", "_"));

        if(isBackEndCalculation){
            mf.setCode(queryField.getCode());
        }else {
            mf.setCode(BIConsts.Custom_Field_Name_Suffix + queryField.getQueryArea().getShortCode() + "_" + fieldId.replaceAll("[\\.\\-\\ :]", "_"));
            //mf.setCode(BIConsts.Custom_Field_Name_Suffix + queryField.getQueryArea().getShortCode() + "_" + (queryField.getShowOrder() + "").replaceAll("[\\.\\-]", "_"));
        }
        mf.setName(mf.getCode());
        mf.setIsActive(Enabled.YES.getId());
        mf.setIsMeasure(cfg.getIsMeasure());
        String calcExpression = CustomFieldParser.convertCalcExpression(queryField, cfg.getExpression());
        if(!Enabled.value(mf.getIsMeasure()) && isBackEndCalculation ) {
            queryField.getCustomFieldConfigure().setExpression(calcExpression);
        }
        if(Enabled.value(cfg.getIsMeasure())) {
            mf.setWeight(100);
            MetaField lodMeasureMeta = null;
            if(queryField.isLodField()){
                // 若是lod字段，数据类型和格式化从所选指标获取
                String lodMeasureId = queryField.getCustomFieldConfigure().getLodConfig().getMeasureId();
                lodMeasureMeta = SSDMetaCacheManager.getField(lodMeasureId);
            }else if(fieldId.startsWith(CustomFieldType.LOD.getIdentifier())){
                // lod字段被解构出来后，其自定义字段类型=common，但id有lod:标识，此时需按其源指标设置其数据类型和格式化字符串
                // 源字段在其原子字段中
                if(BIUtil.isNotEmpty(atomFields)) {
                    lodMeasureMeta = atomFields.get(0).getMeta();
                }
            }
            if(lodMeasureMeta != null) {
                mf.setDataType(lodMeasureMeta.getDataType());
                mf.setShowFormatExpression(lodMeasureMeta.getShowFormatExpression());
            }else {
                mf.setDataType(DataType.Double.toString());
                mf.setShowFormatExpression(ShowFormatExpressionType.Format_Decimal.getCode());
            }
            queryField.setCalc(true);
        }else {
            mf.setDataType(DataType.String.toString());
        }
        mf.setAggExpression(calcExpression);
        mf.setFactTableId(factTableId);
        mf.setDimTableId(dimTableId);

        // 设置格式化表达式
        if (cfg != null && cfg.getDecimal() != null) {
            String formatExpression = "";
            formatExpression = "###,###,##0" + (cfg.getDecimal() > 0 ? "." + StringUtil.createRepeatString("0", cfg.getDecimal()) : "");
            formatExpression = formatExpression + (cfg.getRatio() ? "%" : "");
            mf.setShowFormatExpression(formatExpression);
        }

        queryField.setMeta(mf);
        return mf;
    }


    @Deprecated
    public static MetaField getMetaField(String fieldId) {
        /*
        Map<String, MetaField> cache = customFields.get();
        if(cache == null) {
            return null;
        }
        return cache.get(fieldId);
         */
        return null;
    }

    /**
     * 检查字段互斥：
     * 计算字段的原生字段获取其互斥列表，若互斥列表中有结果区、过滤区字段中任何字段，则存在互斥字段问题
     * @return 异常互斥字段名称列表
     */
    public static List<String> checkExclude(QueryConfigure config, QueryField customField){
        List<String> exceptionExcludeTitles = new ArrayList<>();
        CustomFieldConfigure customCfg = customField.getCustomFieldConfigure();
        if(customCfg == null || customCfg.isEmpty()) {
            return exceptionExcludeTitles;
        }
        List<QueryField> customFieldSrcFields = CustomFieldParser.parseExpression(customField.getCustomFieldConfigure().getExpression());
        // 结果字段
        List<QueryField> configFields = config.getAllFields();

        // 添加其他计算字段的原生字段
        List<QueryField> atomFields = new ArrayList<>();
        configFields.forEach(cf -> {
            if (cf.getCustomFieldConfigure() != null && !cf.getCustomFieldConfigure().isEmpty()) {
                List<QueryField> fieldList = CustomFieldParser.parseExpression(cf.getCustomFieldConfigure().getExpression());
                atomFields.addAll(fieldList);
            }}
            );
        configFields.addAll(atomFields);

        // 所有模板配置字段的编码（排除当前自定义字段）
        List<String> configFieldCodes = new ArrayList<>();
        configFields.forEach(f-> {
            // 排除掉当前计算字段
            if(f.getCustomFieldConfigure() != null
                    && !f.getCustomFieldConfigure().isEmpty()
                    && customCfg.getExpression().equals(f.getCustomFieldConfigure().getExpression())) {
            }else if(BIUtil.isNotEmpty(f.getCode())){
                configFieldCodes.add(f.getCode());
            }
        });

        // 计算字段的源字段
        List<String> customIds = new ArrayList<>();
        if(customFieldSrcFields != null) {
            customFieldSrcFields.forEach(f -> {
                if (!customIds.contains(f.getId())) {
                    customIds.add(f.getId());
                }});
        }

        /*
        Set<String> excludeCodes = SSDMetaCacheManager.getExcludeFieldById(customIds);

        // 互斥字段与查询配置模板字段存在互斥，则为异常互斥
        Set<String> exceptionExcludeCodes = excludeCodes.stream().filter(s -> {
            return configFieldCodes.contains(s);
        }).collect(Collectors.toSet());


        if(BIUtil.isNotEmpty(exceptionExcludeCodes)) {
            configFields.forEach(f -> {
                if(exceptionExcludeCodes.contains(f.getCode())) {
                    exceptionExcludeTitles.add(f.getTitle());
                }
            });
        }
         */

        return exceptionExcludeTitles;
    }

    /**
     * 校验所有计算字段互斥信息
     * @param config
     * @return 互斥字段名
     */
    public static Map<String, List<String>> checkAllExclude(QueryConfigure config){
        List<QueryField> allFields = config.getAllFields();
        // 自定义字段
        List<QueryField> allCustomFields = allFields.stream()
                                            .filter(cf -> cf.getCustomFieldConfigure() != null && !cf.getCustomFieldConfigure().isEmpty())
                                            .collect(Collectors.toList());
        Map<String, List<String>> exceptionExcludes = new HashMap<>();
        allCustomFields.forEach(cf -> {
            List<String> excludeTitles = checkExclude(config, cf);
            if(BIUtil.isNotEmpty(excludeTitles)) {
                exceptionExcludes.put(cf.getTitle(), excludeTitles);
            }
        });
        return exceptionExcludes;
    }

    public static void clearCache(){
        /*
        if(customFields.get() != null){
            customFields.get().clear();
            customFields.remove();
        }
         */
    }

}
