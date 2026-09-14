package com.bi.queryer.ssm.engine.config.ui.normalizer;

import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.custom.CustomFieldConfigure;
import com.bi.queryer.ssm.custom.CustomFieldExpressionIdMapping;
import com.bi.queryer.ssm.custom.CustomFieldType;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.config.ui.UIQueryConfigure;
import com.bi.queryer.ssm.engine.config.ui.UIQueryField;
import com.bi.queryer.ssm.engine.lod.LodUIConfigure;
import com.bi.queryer.ssm.enums.AggExpressionType;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.meta.MetaFieldCategory;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.enums.RuntimeEnv;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;

import java.util.*;

/**
 * @Author contributor
 * @Date 16:15 2025/1/6
 * @Description 查询字段规范化：将字段id修正为正确的字段id
 * 字段修正顺序（仅当前数据集下）
 * 1、在字段tree上通过字段id查找
 * 5、在tree上去查找code相同字段 +
 * 若找到，则修正字段id和字段code
 *
 * 修正范围：
 * 1、常规结果和过滤字段（维度、指标）
 * 2、计算字段引用字段
 * 3、lod字段引用字段（lod、lod四则运算）
 * 4、agg日均值字段引用字段
 *
 **/
public class UIQueryFieldNormalizer extends UIBaseNormalizer{

    // key=字段id
    private Map<String, MetaField> fieldIdMap = new HashMap<>(500);

    // key=字段code
    private Map<String, MetaField> fieldCodeMap = new HashMap<>(500);

    public UIQueryFieldNormalizer(UIQueryConfigure uiQueryConfigure, Map<String, MetaField> fieldIdMap, Map<String, MetaField> fieldCodeMap) {
        super(uiQueryConfigure);
        this.fieldIdMap = fieldIdMap;
        this.fieldCodeMap = fieldCodeMap;
    }

    @Override
    public void normalize() {

        long t1 = System.currentTimeMillis();

        //规范化行维度
        uiQueryConfigure.getResult().setRowDimensions(normalizeFields(uiQueryConfigure.getResult().getRowDimensions()));
        //规范化列维度
        uiQueryConfigure.getResult().setColDimensions(normalizeFields(uiQueryConfigure.getResult().getColDimensions()));
        //规范化指标
        uiQueryConfigure.getResult().setMeasures(normalizeFields(uiQueryConfigure.getResult().getMeasures()));
        //规范化过滤字段
        uiQueryConfigure.setFilter(normalizeFields(uiQueryConfigure.getFilter()));

        long t2 = System.currentTimeMillis();
        System.out.println("normalize-query-field:" + (t2-t1));
    }

    /**
     * 校验字段是否存在
     * id不存在code存在的场景，修正id
     * @param fields
     * @return
     */
    public List<UIQueryField> normalizeFields(List<UIQueryField> fields) {

        List<UIQueryField> result = new ArrayList<>();
        for (UIQueryField uiQueryField : fields) {

            String code = uiQueryField.getCode();
            if ("dt".equalsIgnoreCase(code)) {
                result.add(uiQueryField);
                continue;
            }

            if (BIConsts.ALL_MEASURE_CODE.equalsIgnoreCase(code)) {
                result.add(uiQueryField);
                continue;
            }

            // 矫正lod字段
            if (uiQueryField.isLodField()) {
                uiQueryField = rectifyLodField(uiQueryField);
            } else if (this.isCalcField(uiQueryField)) {
                // 矫正计算字段
                uiQueryField = rectifyCalcField(uiQueryField);
            } else {
                // 矫正常规字段
                uiQueryField = rectifyCommonField(uiQueryField);
            }

            if (uiQueryField == null) {
                continue;
            }

            result.add(uiQueryField);
        }

        return result;
    }

    /**
     * 矫正常规字段
     * @param uiField
     */
    protected UIQueryField rectifyCommonField(UIQueryField uiField){

        MetaField metaField = find(uiField.getId(),uiField.getCode());
        if(metaField != null){

            String aggExpressionType = "";
            // 处理多日均值
            if(uiField.getId().endsWith(AggExpressionType.Avg_By_Day.getCode())){
                aggExpressionType = AggExpressionType.Avg_By_Day.getCode();
            }

            // 处理非空多日均值
            if(uiField.getId().endsWith(AggExpressionType.Avg_By_Day_Real.getCode())){
                aggExpressionType = AggExpressionType.Avg_By_Day_Real.getCode();
            }

            this.copy(metaField, uiField);

           if(StrUtil.isNotEmpty(aggExpressionType)){
               uiField.setId(uiField.getId()+"_"+aggExpressionType);
               uiField.setCode(uiField.getCode()+"_"+aggExpressionType);
           }

        }else{
            return null;
        }

        return uiField;
    }

    /**
     * 矫正计算字段（不含lod）
     * @param uiField
     */
    protected UIQueryField rectifyCalcField(UIQueryField uiField){
        CustomFieldConfigure customFieldConfigure = uiField.getCustomFieldConfigure();
        List<String> refIdList = getCalcRefFieldIdList(uiField);

        //是否存在引用字段在元数据中找不到
        boolean isNotExist = false;
        for(String refId : refIdList) {

            //分析字段不处理
            if (refId.startsWith(CustomFieldType.ANALYSIS.getIdentifier())) {
                continue;
            }

            CustomFieldExpressionIdMapping refMapping = null;
            // 获取引用字段映射关系
            for (CustomFieldExpressionIdMapping idMapping : customFieldConfigure.getExpressionIdMapping()) {
                if (refId.equals(idMapping.getId())) {
                    refMapping = idMapping;
                    break;
                }
            }
            MetaField refField = null;
            if (refMapping != null) {
                refField = find(refMapping.getId(), refMapping.getCode());
            } else {
                refField = find(refId, "");
            }

            //从兼容lod的计算字段id不对，CustomFieldExpressionIdMapping也不对的场景。从原始字段lod的配置中获取字段
            //https://ssd-admin.example.com/ssm/#/template/ccb41085ca7d4a04864d058217a114fe?viewId=804d62ec6f2b427e95ffb31a714eedaf&datasetId=68298a6d360c4c59ada307fd5fc0112c
            if(refField == null){
              Optional<UIQueryField> measure = uiQueryConfigure.getResult().getMeasures()
                        .stream().filter(m -> m.getId().equals(refId)
                ).findAny();

              if(measure.isPresent()){
                  if(measure.get().isLodField()){
                      refField = find(measure.get().getCustomFieldConfigure().getLodConfig().getMeasureId(), "");
                  }
              }
            }

            // 处理多日均值
            String originId = refId.replace("_"+AggExpressionType.Avg_By_Day_Real.getCode(),"")
                    .replace("_"+AggExpressionType.Avg_By_Day.getCode(),"");
            // 处理lod字段
            originId = originId.replace(CustomFieldType.LOD.getIdentifier(),"");

            if (refField != null) {
                String expr = customFieldConfigure.getExpression();
                if (BIUtil.isNotEmpty(expr)) {
                    customFieldConfigure.setExpression(expr.replace(originId, refField.getId()));
                }
                if (refMapping != null) {
                    String id = refMapping.getId().replace(originId, refField.getId());
                    refMapping.setId(id);
                    refMapping.setCode(refField.getCode());
                    refMapping.setTitle(refField.getTitle());
                    refMapping.setModuleCtgId(refField.getModuleCtgId());
                }
            } else {
                isNotExist = true;
            }
        }

        if(isNotExist){
            return null;
        }

        return uiField;
    }

    /**
     * 矫正lod
     * @param uiField
     */
    protected UIQueryField rectifyLodField(UIQueryField uiField){

        CustomFieldConfigure customFieldConfigure = uiField.getCustomFieldConfigure();
        List<String> refIdList = getCalcRefFieldIdList(uiField);

        //是否存在引用字段在元数据中找不到
        boolean isNotExist = false;
        for(String refId : refIdList){

            //分析字段不处理
            if(refId.startsWith(CustomFieldType.ANALYSIS.getIdentifier())){
                continue;
            }

            //计算字段里面的lod字段不处理
            if(refId.startsWith(CustomFieldType.LOD_CALC.getIdentifier())){
                continue;
            }

            CustomFieldExpressionIdMapping refMapping = null;
            // 获取引用字段映射关系
            for(CustomFieldExpressionIdMapping idMapping : customFieldConfigure.getExpressionIdMapping()){
                if(refId.equals(idMapping.getId())){
                    refMapping = idMapping;
                    break;
                }
            }
            MetaField refField = null;
            if(refMapping != null){
                refField = find( refMapping.getId(), refMapping.getCode());
            }else {
                refField = find( refId, "");
            }

            String originId = refId.replace("_"+AggExpressionType.Avg_By_Day_Real.getCode(),"")
                    .replace("_"+AggExpressionType.Avg_By_Day.getCode(),"");

            if(refField != null){
                String expr = customFieldConfigure.getExpression();
                if(BIUtil.isNotEmpty(expr)){
                    customFieldConfigure.setExpression(expr.replace(originId, refField.getId()));
                }
                if(refMapping != null) {
                    String id = refMapping.getId().replace(originId, refField.getId());
                    refMapping.setId(id);
                    refMapping.setCode(refField.getCode());
                    refMapping.setTitle(refField.getTitle());
                    refMapping.setModuleCtgId(refField.getModuleCtgId());
                }
            }else{
                isNotExist = true;
            }

            LodUIConfigure lodConfigure = customFieldConfigure.getLodConfig();

            // 指标
            MetaField lodMeasureField = this.find( lodConfigure.getMeasureId(), lodConfigure.getMeasureCode());
            if(lodMeasureField != null){
                lodConfigure.setMeasureId(lodMeasureField.getId());
                lodConfigure.setMeasureCode(lodMeasureField.getCode());
            }else{
                isNotExist = true;
            }

            // 维度
            List<QueryField> dimensionList = lodConfigure.getDimensionList();
            if(BIUtil.isNotEmpty(dimensionList)){
                for(QueryField dim : dimensionList){
                    MetaField lodDimField = this.find(dim.getId(), dim.getCode());
                    if(lodDimField != null){
                        dim.setId(lodDimField.getId());
                        dim.setCode(lodDimField.getCode());
                        dim.setModuleCtgId(lodDimField.getModuleCtgId());
                    }
                }
            }
        }

        if(isNotExist){
            return null;
        }

        return uiField;
    }

    /**
     * 查找逻辑
     * 1 先通过id查找，如果没有则通过code查找
     * 2 针对日均聚合字段，去掉聚合方式后再查找
     * 3 针对lod字段，去掉lod的标识符再查找
     * @param fieldId
     * @param fieldCode
     * @return
     */
    protected MetaField find(String fieldId, String fieldCode) {

        //处理日均场景
        if (fieldId.endsWith(AggExpressionType.Avg_By_Day.getCode())) {
            fieldId = fieldId.replace("_" + AggExpressionType.Avg_By_Day.getCode(), "");
            fieldCode = fieldCode.replace("_" + AggExpressionType.Avg_By_Day.getCode(), "");
        }

        if (fieldId.endsWith(AggExpressionType.Avg_By_Day_Real.getCode())) {
            fieldId = fieldId.replace("_" + AggExpressionType.Avg_By_Day_Real.getCode(), "");
            fieldCode = fieldCode.replace("_" + AggExpressionType.Avg_By_Day_Real.getCode(), "");
        }

        //处理lod场景
        fieldId = fieldId.replace(CustomFieldType.LOD.getIdentifier(),"");

        // id查找
        MetaField fieldMeta = fieldIdMap.get(fieldId);

        // code查找
        if (fieldMeta == null) {
            fieldMeta = fieldCodeMap.get(fieldCode);
        }
        return fieldMeta;
    }

}
