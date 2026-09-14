package com.bi.queryer.ssm.enums;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum ExportAppendStringType {

    NONE("","无"),
    TAB("tab","制表符");

    ExportAppendStringType(String id,String desc){
        this.id = id;
        this.desc = desc;
    }

    private String id;

    private String desc;

    public static ExportAppendStringType get(String id) {
        for(ExportAppendStringType t : values()){
            if(t.name().equalsIgnoreCase(id)) {
                return t;
            }
        }
        return NONE;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
        for (ExportAppendStringType i : ExportAppendStringType.values()) {
            Map<String, String> map = new HashMap<>();
            map.put("value", i.getId());
            map.put("label", i.getDesc());
            listMap.add(map);
        }
        return listMap;
    }
}
