package com.bi.queryer.ssm.enums;

/**
 * @Author contributor
 * @Date 16:52 2024-04-02
 * @Description 排序模式
 **/
public enum SortMode {
    NORMAL("常规：order by在查询语句最后"),
    ROW_NUMBER("row_number排序：在select子句中使用row_number() over(order by x)排序"),
    NONE("无");

    private String desc;

    private SortMode(String desc){
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
