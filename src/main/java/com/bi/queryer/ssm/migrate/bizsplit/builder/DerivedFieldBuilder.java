package com.bi.queryer.ssm.migrate.bizsplit.builder;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.query.template.view.model.TemplateViewFieldAssetEntity;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 膨胀完成后重建 cfg_dtl 中的 measure 派生字段。
 *
 * 与前端 viewAsset/buildTplViewAssets 及各类 getFieldAsset 逻辑对齐：
 * common 指标 assetIdentifier 去除自定义聚合后缀；calc 对 expression 做 id→code 及 LOD 依赖归一化；
 * LOD 指标单独 type=lod，identifier 为 measureCode-auto 或 measureCode-manual-dimCodes；
 * name 对 measure 取 displayTitle 优先，filter 取 title。
 * filter 仅纳入 type=nbr 的条目；资产按 type+assetIdentifier 去重后按 assetIdentifier 排序。
 */
@Component
public class DerivedFieldBuilder {

    private static final String ASSET_TYPE_COMMON = "common";
    private static final String ASSET_TYPE_CALC = "calc";
    private static final String ASSET_TYPE_LOD = "lod";

    private static final String LOD_FIELD_ID_PREFIX = "lod:";

    /** 与前端 aggregationOptions 中需剥离的后缀一致，长后缀优先 */
    private static final String[] AGG_EXPRESSION_SUFFIXES = {"_avg_by_d_r", "_avg_by_d"};

    /**
     * 从膨胀后的 tplConfig 重建指标编码串与资产 JSON。
     *
     * @param tplConfigJson 膨胀后的 tplConfig 根对象
     * @return 长度为 2 的数组：[0]=measureCodes，[1]=measureAsset 明文 JSON
     */
    public String[] rebuildDerivedFields(JSONObject tplConfigJson) {
        JSONObject result = tplConfigJson != null ? tplConfigJson.getJSONObject("result") : null;
        JSONArray measures = result != null ? result.getJSONArray("measures") : null;
        List<JSONObject> measureFieldList = toFieldList(measures);

        Set<String> measureCodeSet = new LinkedHashSet<>();
        List<TemplateViewFieldAssetEntity> assetList = new ArrayList<>();

        for (JSONObject measure : measureFieldList) {
            collectMeasureCodeAndAsset(measure, measureFieldList, false, measureCodeSet, assetList);
        }

        // 与前端 buildTplViewAssets 一致：filter 仅 type=nbr 纳入 measure 资产
        JSONArray filter = tplConfigJson != null ? tplConfigJson.getJSONArray("filter") : null;
        if (CollUtil.isNotEmpty(filter)) {
            for (int i = 0; i < filter.size(); i++) {
                JSONObject field = filter.getJSONObject(i);
                if (field == null) {
                    continue;
                }
                if ("nbr".equalsIgnoreCase(field.getString("type"))) {
                    collectMeasureCodeAndAsset(field, measureFieldList, true, measureCodeSet, assetList);
                }
            }
        }

        Collections.sort(assetList, Comparator.comparing(TemplateViewFieldAssetEntity::getAssetIdentifier,
                Comparator.nullsLast(String::compareTo)));

        String measureCodesStr = String.join(",", measureCodeSet);
        String assetJson = JSON.toJSONString(assetList);
        return new String[]{measureCodesStr, assetJson};
    }

    /**
     * 收集 code 与资产；资产按 type+assetIdentifier 去重（与前端 buildTplViewAssets 一致）。
     */
    private void collectMeasureCodeAndAsset(JSONObject field, List<JSONObject> allMeasures, boolean filterField,
                                            Set<String> measureCodeSet, List<TemplateViewFieldAssetEntity> assetList) {
        String code = field.getString("code");
        if (StrUtil.isNotEmpty(code)) {
            measureCodeSet.add(code);
        }
        TemplateViewFieldAssetEntity asset = buildFieldAsset(field, allMeasures, filterField);
        if (asset != null && !assetExists(assetList, asset)) {
            assetList.add(asset);
        }
    }

    private boolean assetExists(List<TemplateViewFieldAssetEntity> assetList, TemplateViewFieldAssetEntity asset) {
        for (TemplateViewFieldAssetEntity existing : assetList) {
            if (StrUtil.equals(existing.getType(), asset.getType())
                    && StrUtil.equals(existing.getAssetIdentifier(), asset.getAssetIdentifier())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 构建单字段资产，逻辑对齐前端 BaseField / CptField / LodCptField / FilterField.getFieldAsset。
     *
     * @param field       字段 JSON（measure 或 filter）
     * @param allMeasures 全量 measures，供 calc 替换 LOD 依赖
     * @param filterField true 表示 filter 区 nbr 字段（common 资产不做聚合后缀剥离，name 用 title）
     */
    private TemplateViewFieldAssetEntity buildFieldAsset(JSONObject field, List<JSONObject> allMeasures,
                                                         boolean filterField) {
        if (field == null) {
            return null;
        }
        JSONObject customCfg = field.getJSONObject("customFieldConfigure");
        if (customCfg != null) {
            if (isLodField(field, customCfg)) {
                return buildLodFieldAsset(field, customCfg, allMeasures, filterField);
            }
            if (StrUtil.isNotEmpty(customCfg.getString("expression"))) {
                return buildCalcFieldAsset(field, customCfg, allMeasures, filterField);
            }
        }
        return buildCommonFieldAsset(field, filterField);
    }

    /**
     * 普通指标资产：type=common，assetIdentifier 去除自定义聚合后缀。
     */
    private TemplateViewFieldAssetEntity buildCommonFieldAsset(JSONObject field, boolean filterField) {
        String code = field.getString("code");
        if (StrUtil.isEmpty(code)) {
            return null;
        }
        TemplateViewFieldAssetEntity asset = new TemplateViewFieldAssetEntity();
        asset.setType(ASSET_TYPE_COMMON);
        asset.setAssetIdentifier(filterField ? code : getOriginalExpByAggregateExp(code));
        asset.setName(resolveAssetName(field, filterField));
        return asset;
    }

    /**
     * 四则计算字段资产：type=calc，expression 归一化后作为 assetIdentifier。
     * 对齐 CptField.getFieldAsset：剥离聚合后缀、mapping 中 id→code、LOD 依赖替换为 LOD assetIdentifier。
     */
    private TemplateViewFieldAssetEntity buildCalcFieldAsset(JSONObject field, JSONObject customCfg,
                                                             List<JSONObject> allMeasures, boolean filterField) {
        String expression = customCfg.getString("expression");
        if (StrUtil.isEmpty(expression)) {
            return null;
        }
        String assetIdentifier = getOriginalExpByAggregateExp(expression);
        JSONArray mapping = customCfg.getJSONArray("expressionIdMapping");
        if (CollUtil.isNotEmpty(mapping)) {
            for (int i = 0; i < mapping.size(); i++) {
                JSONObject item = mapping.getJSONObject(i);
                if (item == null) {
                    continue;
                }
                String mapId = item.getString("id");
                String mapCode = item.getString("code");
                if (StrUtil.isEmpty(mapId) || StrUtil.isEmpty(mapCode) || mapId.startsWith(LOD_FIELD_ID_PREFIX)) {
                    continue;
                }
                assetIdentifier = replaceLiteral(assetIdentifier,
                        getOriginalExpByAggregateExp(mapId), getOriginalExpByAggregateExp(mapCode));
            }
        }
        for (JSONObject measureField : allMeasures) {
            if (!isLodField(measureField, measureField.getJSONObject("customFieldConfigure"))) {
                continue;
            }
            TemplateViewFieldAssetEntity lodAsset = buildLodFieldAsset(measureField,
                    measureField.getJSONObject("customFieldConfigure"), allMeasures, false);
            if (lodAsset == null) {
                continue;
            }
            String lodId = measureField.getString("id");
            if (StrUtil.isNotEmpty(lodId)) {
                assetIdentifier = replaceLiteral(assetIdentifier,
                        getOriginalExpByAggregateExp(lodId), lodAsset.getAssetIdentifier());
            }
        }
        TemplateViewFieldAssetEntity asset = new TemplateViewFieldAssetEntity();
        asset.setType(ASSET_TYPE_CALC);
        asset.setAssetIdentifier(assetIdentifier);
        asset.setName(resolveAssetName(field, filterField));
        return asset;
    }

    /**
     * LOD 指标资产：type=lod，assetIdentifier 为 measureCode-auto 或 measureCode-manual-sortedDimCodes。
     * 对齐 LodCptField.getFieldAsset。
     */
    private TemplateViewFieldAssetEntity buildLodFieldAsset(JSONObject field, JSONObject customCfg,
                                                            List<JSONObject> allMeasures, boolean filterField) {
        JSONObject lodConfig = customCfg.getJSONObject("lodConfig");
        if (lodConfig == null) {
            return null;
        }
        String measureId = lodConfig.getString("measureId");
        String measureCode = resolveLodMeasureCode(measureId, customCfg, allMeasures);
        if (StrUtil.isEmpty(measureCode)) {
            return null;
        }
        String assetIdentifier;
        if ("auto".equals(lodConfig.getString("dimConfigType"))) {
            assetIdentifier = measureCode + "-auto";
        } else {
            JSONArray dimensionList = lodConfig.getJSONArray("dimensionList");
            List<String> dimCodes = new ArrayList<>();
            if (CollUtil.isNotEmpty(dimensionList)) {
                for (int i = 0; i < dimensionList.size(); i++) {
                    JSONObject dim = dimensionList.getJSONObject(i);
                    if (dim != null && StrUtil.isNotEmpty(dim.getString("code"))) {
                        dimCodes.add(dim.getString("code"));
                    }
                }
            }
            Collections.sort(dimCodes);
            assetIdentifier = measureCode + "-manual-" + String.join("_", dimCodes);
        }
        TemplateViewFieldAssetEntity asset = new TemplateViewFieldAssetEntity();
        asset.setType(ASSET_TYPE_LOD);
        asset.setAssetIdentifier(assetIdentifier);
        asset.setName(resolveAssetName(field, filterField));
        return asset;
    }

    /**
     * 解析 LOD 依赖的基础指标 code，优先 expressionIdMapping，其次 measures 中同 id 字段。
     */
    private String resolveLodMeasureCode(String measureId, JSONObject customCfg, List<JSONObject> allMeasures) {
        if (StrUtil.isEmpty(measureId)) {
            return null;
        }
        JSONArray mapping = customCfg.getJSONArray("expressionIdMapping");
        if (CollUtil.isNotEmpty(mapping)) {
            for (int i = 0; i < mapping.size(); i++) {
                JSONObject item = mapping.getJSONObject(i);
                if (item != null && measureId.equals(item.getString("id"))
                        && StrUtil.isNotEmpty(item.getString("code"))) {
                    return getOriginalExpByAggregateExp(item.getString("code"));
                }
            }
        }
        for (JSONObject measure : allMeasures) {
            if (measureId.equals(measure.getString("id")) && StrUtil.isNotEmpty(measure.getString("code"))) {
                return getOriginalExpByAggregateExp(measure.getString("code"));
            }
        }
        return getOriginalExpByAggregateExp(measureId);
    }

    private boolean isLodField(JSONObject field, JSONObject customCfg) {
        if ("lod".equals(field.getString("cptType"))) {
            return true;
        }
        return customCfg != null && "lod".equals(customCfg.getString("type"));
    }

    /**
     * measure 区：displayTitle 优先，否则 title；filter 区：仅 title（对齐 FilterField）。
     */
    private String resolveAssetName(JSONObject field, boolean filterField) {
        if (filterField) {
            return field.getString("title");
        }
        String displayTitle = field.getString("displayTitle");
        if (StrUtil.isNotEmpty(displayTitle)) {
            return displayTitle;
        }
        return field.getString("title");
    }

    /**
     * 去除自定义聚合后缀，与前端 getOriginalExpByAggregateExp 一致。
     */
    private String getOriginalExpByAggregateExp(String aggExp) {
        if (StrUtil.isEmpty(aggExp)) {
            return aggExp;
        }
        String result = aggExp;
        for (String suffix : AGG_EXPRESSION_SUFFIXES) {
            result = result.replace(suffix, "");
        }
        return result;
    }

    private String replaceLiteral(String text, String from, String to) {
        if (StrUtil.isEmpty(text) || StrUtil.isEmpty(from) || from.equals(to)) {
            return text;
        }
        return text.replace(from, to);
    }

    private List<JSONObject> toFieldList(JSONArray array) {
        if (CollUtil.isEmpty(array)) {
            return Collections.emptyList();
        }
        List<JSONObject> list = new ArrayList<>(array.size());
        for (int i = 0; i < array.size(); i++) {
            JSONObject item = array.getJSONObject(i);
            if (item != null) {
                list.add(item);
            }
        }
        return list;
    }
}
