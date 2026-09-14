package com.bi.queryer.ssm.enums;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: contributor
 * Date: 2020/2/6
 * Time: 16:44
 * Description:
 */
public enum CategoryType {
    Front("前台目录"),
    Back("后台目录");

    private String desc;

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    private CategoryType(String desc) {
        this.desc = desc;
    }

    public static CategoryType get(String str) {
        for(CategoryType t : values()){
            if(t.toString().equalsIgnoreCase(str)){
                return t;
            }
        }
        return Front;
    }

    // 转换成为 List<Map<String, String>>, 对外提供查询和遍历功能
    public static List<Map<String, String>> toListMap() {
        List<Map<String, String>> listMap = new ArrayList<>();
        for (CategoryType i : CategoryType.values()) {
            Map<String, String> map = new HashMap<>();
            map.put("value", i.getDesc());
            map.put("label", i.getDesc());
            listMap.add(map);
        }
        return listMap;
    }
}
