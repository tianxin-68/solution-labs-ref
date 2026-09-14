package com.bi.queryer.ssm.util;

import com.bi.queryer.ssm.enums.FieldType;
import com.bi.queryer.ssm.meta.BitSetMetaCacheManager;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.meta.MetaTable;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIUtil;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 10:18 2026/1/9
 * @Description 字段互斥工具类
 **/
public class FieldExclusiveUtil {
    /**
     * 判断字段和对比字段是否存在互斥
     * 互斥的逻辑：
     * 1、将compare字段逐个与base字段的所有字段组成一个字段集合
     * 2、取出上一步的集合的所有维度字段，判断所有维度的表bitset索引是否存在交集，不存在，则表示加入的compare字段与base字段互斥
     * 3、若上一步存在交集，则取出组合中的所有指标和上一步的交集bitset，判断所有指标字段是否都能找此交集的中找到对应的归属表，若找不到，则表示加入的compare字段与base字段互斥
     *
     * 算法参考：https://wiki.example.com/pages/viewpage.action?pageId=665252525
     * @param baseFieldIds
     * @param compareFields
     * @return
     */
    public static Set<String> getExclusiveFieldByBitSet(String datasetId, List<String> baseFieldIds, List<MetaField> compareFields){

        long t1 = System.currentTimeMillis();

        Set<String> exclusiveFieldIds = new HashSet<>();

        // 参数校验
        if (BIUtil.isEmpty(baseFieldIds) || BIUtil.isEmpty(compareFields)) {
            return exclusiveFieldIds;
        }

        boolean isSsmDataset = SSDUtil.isSsmDataset(datasetId);
        // 获取base字段的MetaField对象列表
        List<MetaField> baseFields = new ArrayList<>();
        for (String fieldId : baseFieldIds) {
            MetaField field = SSDMetaCacheManager.getField(fieldId);
            if (field == null) {
                continue;
            }

            if (!isSsmDataset) {
                // 对于 mgp数据集， 前端会传来未绑定白皮书code的字段， 只需要取绑定白皮书code的字段
                if (Enabled.isTrue(field.getIsCommonDate()) || StringUtils.startsWith(field.getCode(), field.getKpiNo())) {
                } else {
                    continue;
                }
            }

            // 若是跨模型指标，则需将引用指标都添加到对标字段中
            if (FieldType.get(field.getFieldType()) == FieldType.CROSS_MODEL_MEASURE) {
                baseFields.addAll(field.getCalcAtomFields());
            } else {
                baseFields.add(field);
            }
        }

        if (BIUtil.isEmpty(baseFields)) {
            return exclusiveFieldIds;
        }

        // 获取BitSetMetaCacheManager的映射
        Map<String, BitSetMetaCacheManager.DatasetFieldBitSet> datasetFieldBitSetMap = BitSetMetaCacheManager.getDatasetFieldBitSetMap();
        Map<String, Integer> tableIndexAndIdMapping = BitSetMetaCacheManager.getTableIndexAndIdMapping();

        if (BIUtil.isEmpty(datasetFieldBitSetMap) || BIUtil.isEmpty(tableIndexAndIdMapping) || datasetFieldBitSetMap.get(datasetId) == null) {
            return exclusiveFieldIds;
        }

        Map<String, BitSet> fieldBitSetMap = datasetFieldBitSetMap.get(datasetId).fieldsBitSet;
        if(BIUtil.isEmpty(fieldBitSetMap)){
            return exclusiveFieldIds;
        }
        // 遍历每个compare字段，判断是否与base字段互斥
        for (MetaField compareField : compareFields) {
            if (compareField == null) {
                continue;
            }

            // 步骤1：将compare字段与base字段的所有字段组成一个字段集合
            List<MetaField> combinedFields = new ArrayList<>(baseFields);

            // 若是跨模型指标，则需将引用指标都添加到对标字段中
            if(FieldType.get(compareField.getFieldType()) == FieldType.CROSS_MODEL_MEASURE){
                combinedFields.addAll(compareField.getCalcAtomFields());
            }else {
                combinedFields.add(compareField);
            }

            // 步骤2：取出所有维度字段
            List<MetaField> dimensionFields = combinedFields.stream().filter(f -> !Enabled.isTrue(f.getIsMeasure())).collect(Collectors.toList());

            // 如果存在维度字段，计算所有维度字段的bitset交集
            BitSet dimensionIntersection = null;
            if (!BIUtil.isEmpty(dimensionFields)) {
                for (MetaField dimField : dimensionFields) {
                    BitSet dimFieldBitSet = fieldBitSetMap.get(dimField.getCode());

                    if (dimFieldBitSet == null || dimFieldBitSet.isEmpty()) {
                        // 如果某个维度字段没有bitset，则无法判断，认为互斥
                        exclusiveFieldIds.add(compareField.getId());
                        break;
                    }

                    if (dimensionIntersection == null) {
                        // 第一个维度字段，直接克隆作为初始交集
                        dimensionIntersection = (BitSet) dimFieldBitSet.clone();
                    } else {
                        // 计算交集
//                        dimensionIntersection = (BitSet) dimensionIntersection.clone();
                        dimensionIntersection.and(dimFieldBitSet);

                        // 如果交集为空，说明维度字段互斥
                        if (dimensionIntersection.isEmpty()) {
                            exclusiveFieldIds.add(compareField.getId());
                            break;
                        }
                    }
                }
            }

            // 如果已经判断为互斥，跳过步骤3
            if (exclusiveFieldIds.contains(compareField.getId())) {
                continue;
            }

            // 步骤3：若维度字段存在交集，判断所有指标字段是否都能在交集中找到对应的归属表
            // 取出所有指标字段
            List<MetaField> measureFields = combinedFields.stream().filter(f -> Enabled.isTrue(f.getIsMeasure())).collect(Collectors.toList());
            if (dimensionIntersection != null && !dimensionIntersection.isEmpty() && BIUtil.isNotEmpty(measureFields)) {
                // 判断每个指标字段是否能在交集中找到对应的归属表
                for (MetaField measureField : measureFields) {
                    String measureCode = measureField.getCode();
                    BitSet measureIntersection = fieldBitSetMap.get(measureCode);
                    if (measureIntersection == null || measureIntersection.isEmpty()) {
                        // 没有对应的bitset，认为互斥
                        exclusiveFieldIds.add(compareField.getId());
                        break;
                    }
                    // 判断表索引是否在维度字段的交集中
                    if (!measureIntersection.intersects(dimensionIntersection)) {
                        // 指标字段的归属表不在维度字段的交集中，认为互斥
                        exclusiveFieldIds.add(compareField.getId());
                        break;
                    }
                }
            } else if (dimensionIntersection == null) {
                // 如果没有维度字段，但有指标字段，需要判断指标字段之间是否互斥
                // 这种情况可以认为不互斥，或者根据业务需求进行判断
                // 这里暂时认为不互斥
            }
        }

        long t2 = System.currentTimeMillis();
        System.out.println(String.format("bitset互斥计算耗时:%sms", (t2-t1)));
        return exclusiveFieldIds;
    }

    protected void debugPrintTable(Integer tableIndex){
        Map<Integer, MetaTable> tableIndexMapping = BitSetMetaCacheManager.getTableIndexMapping();
        MetaTable metaTable = tableIndexMapping.get(tableIndex);
        if(metaTable == null) {
            return;
        }
        System.out.println("置位索引：" + tableIndex + " → 对应表:" + metaTable.getFullName());
    }
}
