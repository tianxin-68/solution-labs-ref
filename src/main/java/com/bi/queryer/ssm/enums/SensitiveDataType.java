package com.bi.queryer.ssm.enums;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum SensitiveDataType {

    NONE("","无"),
    FINANCE("财务类","财务类"),
    THIRD_PARTY("第三方信息类","第三方信息类"),
    MERCHANT_PARTNER("商户&合作伙伴","商户&合作伙伴"),
    MANAGEMENT("经营数据类","经营数据类"),
    STORE("门店信息类","门店信息类"),
    EXTERNAL("外部数据类 ","外部数据类"),
    USER_PROFILE("用户画像类","用户画像类");

    SensitiveDataType(String id, String desc){
        this.id = id;
        this.desc = desc;
    }

    private String id;

    private String desc;

    public static SensitiveDataType get(String id) {
        for(SensitiveDataType t : values()){
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
        for (SensitiveDataType i : SensitiveDataType.values()) {
            Map<String, String> map = new HashMap<>();
            map.put("value", i.getId());
            map.put("label", i.getDesc());
            listMap.add(map);
        }
        return listMap;
    }
}
