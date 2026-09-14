package com.bi.queryer.util.period;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author contributor
 * @Date 18:54 2024-01-08
 * @Description 对比日期映射：基准日期对应的对比日期
 **/
public class CompareDateMapping {
    private String baseDate;

    // key=calcMode,value=compareDate
    private Map<String, String> mappingDates = new HashMap<>();

    public CompareDateMapping(String baseDate) {
        this.baseDate = baseDate;
    }

    public String getBaseDate() {
        return baseDate;
    }

    public void setBaseDate(String baseDate) {
        this.baseDate = baseDate;
    }

    public Map<String, String> getMappingDates() {
        return mappingDates;
    }

    public void setMappingDates(Map<String, String> mappingDates) {
        this.mappingDates = mappingDates;
    }
}
