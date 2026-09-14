package com.bi.queryer.ssm.enums;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: contributor
 * Date: 2020/2/6
 * Time: 11:26
 * Description: 字段过滤类型
 */
public enum FieldFilterType {
    YearRange("year-range", "起止年份"),
    MonthRange("month-range", "起止月份"),
    WeekRange("week-range", "起止年周"),
    DateRange("date-range", "起止日期"),
    DatetimeRange("datetime-range", "起止时间"),
    BooleanSelect("bool-select", "布尔选择"),
    MultiSelect("multi-select", "列表多选"),
    MultiTree("multi-tree", "树多选"),
    Textarea("textarea", "文本"),
    DoubleRange("double-range", "数字区间"),
    None("", "无");

    private String code;
    private String desc;
    private FieldFilterType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static FieldFilterType get(String code) {
        for(FieldFilterType t : values()){
            if(t.code.equalsIgnoreCase(code) || t.toString().equalsIgnoreCase(code)) {
                return t;
            }
        }
        return None;
    }

    public static Boolean isDateRange(String code){
        return isDateRange(get(code));
    }

    public static Boolean isDateRange(FieldFilterType type){
        boolean isDateRange = false;
        switch (type) {
            case YearRange:
            case MonthRange:
            case WeekRange:
            case DateRange:
            case DatetimeRange:
                isDateRange = true;
                break;
            default:
                isDateRange = false;
                break;
        }

        return isDateRange;
    }

    public boolean isRange(){
        boolean isRange = isDateRange(this);
        if(!isRange){
            isRange = (this == DoubleRange);
        }
        return isRange;
    }

    public Boolean isDateRange(){
        return isDateRange(this);
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
        for (FieldFilterType i : FieldFilterType.values()) {
            Map<String, String> map = new HashMap<>();
            map.put("value", i.getCode());
            map.put("label", i.getDesc());
            listMap.add(map);
        }
        return listMap;
    }
}
