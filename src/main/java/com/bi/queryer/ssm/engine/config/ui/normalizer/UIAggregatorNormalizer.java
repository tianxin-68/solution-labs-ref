package com.bi.queryer.ssm.engine.config.ui.normalizer;

import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.custom.CustomFieldConfigure;
import com.bi.queryer.ssm.custom.CustomFieldExpressionIdMapping;
import com.bi.queryer.ssm.custom.CustomFieldType;
import com.bi.queryer.ssm.engine.analysis.cfg.compare.AnalysisCompareConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.compare.AnalysisCompareItemConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.thb.AnalysisThbConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.thb.AnalysisThbItemConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.zb.AnalysisZbConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.zb.AnalysisZbItemConfig;
import com.bi.queryer.ssm.engine.config.ui.UIQueryAnalysis;
import com.bi.queryer.ssm.engine.config.ui.UIQueryConfigure;
import com.bi.queryer.ssm.engine.config.ui.UIQueryField;
import com.bi.queryer.ssm.engine.lod.LodUIConfigure;
import com.bi.queryer.ssm.enums.AggExpressionType;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.util.BIUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 17:13 2025/1/7
 * @Description 前端聚合方式规范化
 **/
public class UIAggregatorNormalizer extends UIBaseNormalizer{
    protected List<String> distinctByDayThenSumCodeList = new ArrayList<>();
    protected Map<String, UIQueryField> allMeasures = new HashMap<>();
    public UIAggregatorNormalizer(UIQueryConfigure uiQueryConfigure) {
        super(uiQueryConfigure);
    }

    /**
     * 特殊处理：默认去重指标调整为先按日去重再汇总，复用日均逻辑
     * 处理范围：
     * 1、常规字段
     * 2、四则计算字段
     * 3、lod计算（含四则运算字段）
     * 处理逻辑：
     * 1、若在列表中的字段code且未配置日均，则进行下一步处理，否则不处理
     * 2、遍历所有指标，若指标code在列表中，修改字段id、code、聚合方式=日均、日去重后聚合模式=sum
     */
    @Override
    public void normalize() {

        String distinctByDayThenSumCodes = SC.v("distinct.by.day.then.sum.code.list", "");
        if(StrUtil.isEmpty(distinctByDayThenSumCodes)) {
            return;
        }

        this.distinctByDayThenSumCodeList = Arrays.asList(distinctByDayThenSumCodes.split(","));
        if(BIUtil.isEmpty(distinctByDayThenSumCodeList)){
            return;
        }
        allMeasures = uiQueryConfigure.getResult().getMeasures().stream().collect(Collectors.toMap(UIQueryField::getId, UIQueryField->UIQueryField, (f1, f2)->f1));
        List<UIQueryField> measures = new ArrayList<>();
        // 处理有先后顺序：先处理常规字段，在处理lod字段，最后处理四则运算字段。便于lod四则运算最后处理
        List<UIQueryField> common = new ArrayList<>();
        List<UIQueryField> lod = new ArrayList<>();
        List<UIQueryField> calc = new ArrayList<>();
        for(UIQueryField f : allMeasures.values()){
            if(f.isLodField()){
                lod.add(f);
            }else if(isCalcField(f)){
                calc.add(f);
            }else{
                common.add(f);
            }
        }
        measures.addAll(common);
        measures.addAll(lod);
        measures.addAll(calc);
        for(UIQueryField measure : measures){
            this.rectifyCommonField(measure);
            this.rectifyLodField(measure);
            this.rectifyCalcField(measure);
        }
    }

    /**
     * 矫正常规字段
     * @param uiField
     */
    protected void rectifyCommonField(UIQueryField uiField){
        if(this.isCalcField(uiField)){
            return;
        }
        if(!distinctByDayThenSumCodeList.contains(uiField.getCode())){
            return;
        }
        this.modify(uiField);
    }

    /**
     * 矫正计算字段（不含lod）
     * @param uiField
     */
    protected void rectifyCalcField(UIQueryField uiField){
        if(!isCalcField(uiField)){
            return;
        }
        if(uiField.isLodField()){
            return ;
        }
        CustomFieldConfigure customFieldConfigure = uiField.getCustomFieldConfigure();
        List<String> refIdList = getCalcRefFieldIdList(uiField);
        for(String refId : refIdList){
            CustomFieldExpressionIdMapping refMapping = null;
            // 获取引用字段映射关系
            for(CustomFieldExpressionIdMapping idMapping : customFieldConfigure.getExpressionIdMapping()){
                if(refId.equals(idMapping.getId())){
                    refMapping = idMapping;
                    break;
                }
            }
            UIQueryField refField = allMeasures.get(refId);
            if(refField != null && (distinctByDayThenSumCodeList.contains(refField.getCode()) || AggExpressionType.get(refField.getDistinctByDayAggMode()) == AggExpressionType.Sum)){
                String expr = customFieldConfigure.getExpression();
                if(BIUtil.isNotEmpty(expr)){
                    if(refField.getId().endsWith(AggExpressionType.Avg_By_Day.getCode())){
                        customFieldConfigure.setExpression(expr.replace(refId, refField.getId()));
                    }else {
                        customFieldConfigure.setExpression(expr.replace(refId, refField.getId() + "_" + AggExpressionType.Avg_By_Day.getCode()));
                    }
                }
                this.modify(refField);
            }
        }
    }

    /**
     * 矫正lod
     * @param uiField
     */
    protected void rectifyLodField(UIQueryField uiField){
        if(!uiField.isLodField()){
            return ;
        }
        if(AggExpressionType.get(uiField.getAggExpressionType()).isAvgByDay()){
            return;
        }
        CustomFieldConfigure customFieldConfigure = uiField.getCustomFieldConfigure();
        if(CustomFieldType.get(customFieldConfigure.getType()) == CustomFieldType.LOD){
            LodUIConfigure lodConfigure = customFieldConfigure.getLodConfig();

            // 指标
            MetaField lodMeasureField = SSDMetaCacheManager.getField(lodConfigure.getMeasureId());
            if(lodMeasureField != null && distinctByDayThenSumCodeList.contains(lodMeasureField.getCode())){
                //lodConfigure.setMeasureId(lodMeasureField.getId() + "_" + AggExpressionType.Avg_By_Day.getCode());
                // lod字段本身也要设置为其日去重后聚合逻辑
                this.modify(uiField);
            }
        }
    }

    protected void modify(UIQueryField uiField){
        if(AggExpressionType.get(uiField.getAggExpressionType()).isAvgByDay()){
            return;
        }
        if(uiField.getId().endsWith(AggExpressionType.Avg_By_Day.getCode()) || uiField.getCode().endsWith( AggExpressionType.Avg_By_Day_Real.getCode())){
            return;
        }
        String rawFieldId = uiField.getId();
        if(!uiField.getId().endsWith(AggExpressionType.Avg_By_Day.getCode())) {
            uiField.setId(uiField.getId() + "_" + AggExpressionType.Avg_By_Day.getCode());
            uiField.setCode(uiField.getCode() + "_" + AggExpressionType.Avg_By_Day.getCode());
        }
        uiField.setAggExpressionType(AggExpressionType.Avg_By_Day.getCode());
        uiField.setDistinctByDayAggMode(AggExpressionType.Sum.getCode());
        this.allMeasures.put(uiField.getId(), uiField);

        String newFieldId = rawFieldId + "_" + AggExpressionType.Avg_By_Day.getCode();
        UIQueryAnalysis analysis = uiQueryConfigure.getAnalysis();
        if (analysis == null) {
            return;
        }

        AnalysisThbConfig thbConfig = analysis.getThb();
        if (thbConfig != null && thbConfig.isActive()) {
            List<AnalysisThbItemConfig> items = thbConfig.getItems();
            for (AnalysisThbItemConfig itemConfig : items) {
                if (itemConfig.getMeasureIdList().remove(rawFieldId)) {
                    itemConfig.getMeasureIdList().add(newFieldId);
                }
            }
        }

        AnalysisZbConfig zbConfig = analysis.getZb();
        if (zbConfig != null && zbConfig.isActive()) {
            List<AnalysisZbItemConfig> items = zbConfig.getItems();
            for (AnalysisZbItemConfig itemConfig : items) {
                if (itemConfig.getMeasureIdList().remove(rawFieldId)) {
                    itemConfig.getMeasureIdList().add(newFieldId);
                }
            }
        }

        AnalysisCompareConfig compareConfig = analysis.getCompare();
        if (compareConfig != null && compareConfig.isActive()) {
            List<AnalysisCompareItemConfig> items = compareConfig.getItems();
            for (AnalysisCompareItemConfig itemConfig : items) {
                if (itemConfig.getMeasureIdList().remove(rawFieldId)) {
                    itemConfig.getMeasureIdList().add(newFieldId);
                }
            }
        }
    }

}
