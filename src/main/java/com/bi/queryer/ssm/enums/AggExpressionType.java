package com.bi.queryer.ssm.enums;

import com.bi.queryer.util.BIUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 聚合表达式
 * @author contributor
 */
public enum AggExpressionType {

    None("","无","", ""),
    Sum("sum","sum","", "sum(%s)"),
    Avg("avg","avg","", "avg(%s)"),
    Max("max","max","", "max(%s)"),
    Min("min","min","", "min(%s)"),
    Count("count","count","计数(不去重)", "count(%s)"),
    Count_Distinct("count(distinct)", "count(distinct)", "计数(去重)", "count(distinct %s)"),
    Avg_By_Day("avg_by_d", "多日均值","日均", ""),
    Avg_By_Day_Real("avg_by_d_r", "非空日均值","非空日均", "");

    private String code;

    private String desc;

    private String title;

    private String expression;

    private AggExpressionType(String code,String desc,String title, String expression){
        this.code = code;
        this.desc = desc;
        this.title = title;
        this.expression = expression;
    }

    public static AggExpressionType get(String code) {
        for(AggExpressionType t : values()){
            if(t.getCode().equalsIgnoreCase(code)){
                return t;
            }
        }
        return None;
    }

    public String getExpression(String aggContent){
        if(BIUtil.isEmpty(this.expression)){
            return aggContent;
        }
        return String.format(this.expression, aggContent);
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    // 转换成为 List<Map<String, String>>, 对外提供查询和遍历功能
    public static List<Map<String, String>> toListMap() {
        List<Map<String, String>> listMap = new ArrayList<>();
        for (AggExpressionType i : AggExpressionType.values()) {
            if(i.isAvgByDay()){
                continue;
            }
            Map<String, String> map = new HashMap<>();
            map.put("value", i.getCode());
            map.put("label", i.getDesc());
            listMap.add(map);
        }
        return listMap;
    }

    /**
     * 是否为日均
     * @return
     */
    public boolean isAvgByDay() {

        if (this == Avg_By_Day || this == Avg_By_Day_Real) {
            return true;
        }

        return false;
    }

}
