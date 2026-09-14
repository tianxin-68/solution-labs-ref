package com.bi.queryer.ssm.enums;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author contributor
 */
public enum FilterQueryRuleType {

    Default("default","默认"),
    Like("like","模糊查询"),
    Exact("exact","精准查询");

    private FilterQueryRuleType(String code, String desc){
        this.code = code;
        this.desc = desc;
    }

    private String code;
    private String desc;

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

    public static FilterQueryRuleType getFilterQueryRule(String code) {
        for(FilterQueryRuleType t : values()){
            if(t.getCode().equalsIgnoreCase(code)) {
                return t;
            }
        }
        return Default;
    }

    // 转换成为 List<Map<String, String>>, 对外提供查询和遍历功能
    public static List<Map<String, String>> toListMap() {
        List<Map<String, String>> listMap = new ArrayList<>();
        for (FilterQueryRuleType i : FilterQueryRuleType.values()) {
            Map<String, String> map = new HashMap<>();
            map.put("value", i.getCode());
            map.put("label", i.getDesc());
            listMap.add(map);
        }
        return listMap;
    }
}
