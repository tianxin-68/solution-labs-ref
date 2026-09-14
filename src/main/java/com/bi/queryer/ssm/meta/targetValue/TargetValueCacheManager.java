package com.bi.queryer.ssm.meta.targetValue;

import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.sys.rmi.RMIServer;
import com.bi.queryer.sys.rmi.impl.MemoryCacheSyncSSMService;
import com.bi.queryer.sys.startup.Initializable;
import com.bi.queryer.sys.startup.InitializableModule;
import com.bi.queryer.sys.startup.SystemInitializer;
import com.bi.queryer.util.SpringContextUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 17:12 2024/12/2
 * @Description 目标值管理器
 **/

@Component
public class TargetValueCacheManager implements Initializable {
    /**
     * <measureCode, List>
     */
    protected final static Map<String, List<TargetValueAdaptiveInfo>> measureAdaptiveMap = new Hashtable<>();
    @Override
    public void initialize(Map<String, ?> initParams) throws BIException {
        BaseDao dao = (BaseDao) SpringContextUtil.getBean("baseDao");
        List<TargetTableField> targetTableFields = dao.queryObjectList("ssm.target.value.queryAllField", null, TargetTableField.class);
        Map<String, List<TargetValueAdaptiveInfo>> res = buildMeasureAdaptiveList(targetTableFields);
        synchronized (measureAdaptiveMap) {
            measureAdaptiveMap.clear();
            measureAdaptiveMap.putAll(res);
        }
    }

    // 同步缓存
    public boolean refreshTargetMetaCache() {
        Map<String, Object> rmiParamMap = new HashMap<>();
        rmiParamMap.put(SystemInitializer.INIT_MODULE_KEY, InitializableModule.TargetValueCacheManager.toString());
        RMIServer.syncInvoke(MemoryCacheSyncSSMService.class, rmiParamMap);
        return true;
    }

    protected Map<String, List<TargetValueAdaptiveInfo>> buildMeasureAdaptiveList(List<TargetTableField> targetTableFields) {
        Map<String, List<TargetValueAdaptiveInfo>> measureAdaptiveMap = new HashMap<>();
        // 按表分字段类型

        Map<String, Map<String, List<TargetTableField>>> tableFieldMap = targetTableFields.stream()
                .collect(Collectors.groupingBy(TargetTableField::getTableId, Collectors.groupingBy(TargetTableField::getFieldType)));

        /** 设置数据集元信息(指标+维度字段) **/
        for (Map.Entry<String, Map<String, List<TargetTableField>>> entry : tableFieldMap.entrySet()) {
            List<TargetTableField> measureFields = entry.getValue().getOrDefault("metric", Collections.emptyList());
            List<TargetTableField> dimFields = entry.getValue().getOrDefault("dim", Collections.emptyList());
            for (TargetTableField measureField : measureFields) {
                List<String> granularityList = new ArrayList<>();
                if (StringUtils.isNotEmpty(measureField.getDateGranularity())) {
                    granularityList = Arrays.asList(measureField.getDateGranularity().split(","));
                }
                List<TargetValueAdaptiveInfo> adaptiveInfoList = measureAdaptiveMap.get(measureField.getWhitePaperCode());
                if (adaptiveInfoList == null) {
                    adaptiveInfoList = new ArrayList<>();
                }
                adaptiveInfoList.add(new TargetValueAdaptiveInfo(measureField, granularityList, dimFields));
                measureAdaptiveMap.put(measureField.getWhitePaperCode(), adaptiveInfoList);
            }
        }
        return measureAdaptiveMap;
    }

    public static Map<String, List<TargetValueAdaptiveInfo>> getMeasureAdaptiveMap() {
        return new HashMap<>(measureAdaptiveMap);
    }

    public static Set<String> getTargetDimCode() {
        return measureAdaptiveMap.values().stream().flatMap(List::stream)
                .map(TargetValueAdaptiveInfo::getDimensionList).flatMap(List::stream).map(TargetTableField::getWhitePaperCode)
                .collect(Collectors.toSet());
    }

    public static Set<String> getTargetMetricCode() {
        Set<String> targetMeasures = new HashSet<>();
        for (List<TargetValueAdaptiveInfo> value : measureAdaptiveMap.values()) {
            for (TargetValueAdaptiveInfo targetValueAdaptiveInfo : value) {
                TargetTableField measureField = targetValueAdaptiveInfo.getMeasureField();
                for (String targetType : measureField.getTargetTypeList()) {
                    targetMeasures.add(String.format("%s_%s_%s", measureField.getWhitePaperCode(), targetType, "value"));
                }
            }
        }
        return targetMeasures;
    }
}
