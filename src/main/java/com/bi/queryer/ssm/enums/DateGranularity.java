package com.bi.queryer.ssm.enums;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author contributor
 * @Date 17:01 2023-06-15
 * @Description 日期粒度
 **/
public enum DateGranularity {
    DAY("d","yyyy-MM-dd", "日"),
    WEEK("w", "yyyyWW", "周"),
    MONTH("m", "yyyy-MM", "月"),
    QUARTER("q","yyyyQQ", "季"),
    YEAR("y", "yyyy", "年"),
    AUTO("auto", "", "默认（系统自动匹配）");

    private String code = "";

    private String format = "";
    private String desc = "";

    private DateGranularity(String code, String format, String desc) {
        this.code = code;
        this.format = format;
        this.desc = desc;
    }

    public static DateGranularity get(String str) {
        for (DateGranularity dg : values()) {
            if (dg.toString().equalsIgnoreCase(str) || dg.getCode().equalsIgnoreCase(str)) {
                return dg;
            }
        }
        return DAY;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    // 转换成为 List<Map<String, String>>, 对外提供查询和遍历功能
    public static List<Map<String, String>> toListMap() {
        List<Map<String, String>> listMap = new ArrayList<>();
        for (DateGranularity i : DateGranularity.values()) {
            Map<String, String> map = new HashMap<>();
            map.put("value", i.getCode());
            map.put("label", i.getDesc());
            listMap.add(map);
        }
        return listMap;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }
}
