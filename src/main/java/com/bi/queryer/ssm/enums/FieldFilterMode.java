package com.bi.queryer.ssm.enums;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: contributor
 * Date: 2020/6/6
 * Time: 11:26
 * Description: 筛选值方式 detail=明细筛选，agg=结果筛选（聚合筛选）
 */
public enum FieldFilterMode {
    detail("detail", "明细筛选"),
    agg("agg", "结果筛选");


    private String code;
    private String desc;

    private FieldFilterMode(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static FieldFilterMode get(String code) {
        for(FieldFilterMode t : values()){
            if(t.code.equalsIgnoreCase(code) || t.toString().equalsIgnoreCase(code)) {
                return t;
            }
        }
        return agg;
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
        for (FieldFilterMode i : FieldFilterMode.values()) {
            Map<String, String> map = new HashMap<>();
            map.put("value", i.getCode());
            map.put("label", i.getDesc());
            listMap.add(map);
        }
        return listMap;
    }
}
