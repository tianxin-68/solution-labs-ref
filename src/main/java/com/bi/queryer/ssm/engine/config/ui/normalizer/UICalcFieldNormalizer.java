package com.bi.queryer.ssm.engine.config.ui.normalizer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.engine.config.ui.UIQueryConfigure;
import com.bi.queryer.ssm.engine.config.ui.UIQueryField;
import com.bi.queryer.ssm.enums.AggExpressionType;
import com.bi.queryer.ssm.enums.FieldType;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.ssm.util.FieldUtil;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.Guid;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 16:05 2025/1/6
 * @Description 计算字段规范化处理：计算指标字段，如果引用字段不在指标区域，则新创建并添加到指标区，并设置为隐藏
 **/
public class UICalcFieldNormalizer extends UIBaseNormalizer{

    public UICalcFieldNormalizer(UIQueryConfigure uiQueryConfigure) {
        super(uiQueryConfigure);
    }

    public void normalize() {
        Map<String, UIQueryField> measureFields = uiQueryConfigure.getResult().getMeasures().stream()
                .collect(Collectors.toMap(UIQueryField::getId, UIQueryField -> UIQueryField, (f1, f2) -> f1));

        int index = 100;
        for (UIQueryField measureField : measureFields.values()) {

            //兼容后台配置的跨模型计算字段
            MetaField metaField = SSDMetaCacheManager.getField(measureField.getId());
            if (metaField != null) {
                if (FieldType.CROSS_MODEL_MEASURE == FieldType.get(metaField.getFieldType())) {
                    measureField.getCustomFieldConfigure().setExpression(metaField.getAggExpression());
                }
            }

            if (!isCalcField(measureField)) {
                continue;
            }
            if (measureField.isLodField()) {
                continue;
            }

            if (StrUtil.isEmpty(measureField.getId()) && measureField.isAnalysisCalc()) {
                measureField.setId(Guid.id());
            }
            if (Enabled.isFalse(measureField.getIsShow())) {
                continue;
            }

            //计算字段使用的跨模型指标，需要将跨模型计算字段的表达式展开
            String expression = FieldUtil.getExtendCalcExpression(measureField.getCustomFieldConfigure().getExpression());
            measureField.getCustomFieldConfigure().setExpression(expression);

            List<String> refFieldIdList = this.getCalcRefFieldIdList(measureField);
            for (String refFieldId : refFieldIdList) {
                // 已存在，不处理
                if (measureFields.containsKey(refFieldId)) {
                    continue;
                }
                // 引用字段不在指标区域，新创建并添加到指标区，并设置为隐藏
                UIQueryField refField = this.createFieldById(refFieldId);
                if (refField == null) {
                    continue;
                }

                MetaField refMetaField = SSDMetaCacheManager.getField(refFieldId);
                //维度不处理
                if (refMetaField != null && !Enabled.value(refMetaField.getIsMeasure())) {
                    continue;
                }

                //特殊处理按日去重后再sum聚合字段，引用字段在指标区域已存在，不处理
                List<String> distinctByDayThenSumCodes = Arrays.asList(SC.v("distinct.by.day.then.sum.code.list", "").split(","));
                if (CollUtil.isNotEmpty(distinctByDayThenSumCodes)) {
                    String refCode = refField.getCode().replace("_" + AggExpressionType.Avg_By_Day.getCode(), "");
                    if (distinctByDayThenSumCodes.contains(refCode)) {
                        long refCount = uiQueryConfigure.getResult().getMeasures().stream().filter(f -> f.getCode().contains(refCode)).count();
                        if (refCount > 0) {
                            continue;
                        }
                    }
                }

                //日均字段打标
                if(refFieldId.endsWith(AggExpressionType.Avg_By_Day.getCode())) {
                    refField.setAggExpressionType(AggExpressionType.Avg_By_Day.getCode());
                }

                //非空日均字段打标
                if(refFieldId.endsWith(AggExpressionType.Avg_By_Day_Real.getCode())) {
                    refField.setAggExpressionType(AggExpressionType.Avg_By_Day_Real.getCode());
                }

                refField.setIsShow(Enabled.NO.getId());
                refField.setShowOrder(index * 1.0);
                uiQueryConfigure.getResult().addMeasure(refField);
                index++;
            }
        }
    }
}
