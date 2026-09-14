package com.bi.queryer.ssm.enums;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 关联类型
 * @author contributor
 */
public enum FieldJoinType {

    InnerJoine("inner join"),
    LeftJoin("left join"),
    RightJoin("right join"),
    FullJoin("full join");

    private String code;

    private FieldJoinType(String code) {
        this.code = code;
    }
    public static FieldJoinType getType(String code){
        for(FieldJoinType t : values()){
            if(t.getCode().equalsIgnoreCase(code)){
                return t;
            }
        }
        return InnerJoine;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    // 转换成为 List<Map<String, String>>, 对外提供查询和遍历功能
    public static List<Map<String, String>> toListMap() {
        List<Map<String, String>> listMap = new ArrayList<>();
        for (FieldJoinType i : FieldJoinType.values()) {
            Map<String, String> map = new HashMap<>();
            map.put("value", i.getCode());
            map.put("label", i.getCode());
            listMap.add(map);
        }
        return listMap;
    }
}
