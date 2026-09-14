package com.bi.queryer.ssm.enums;

/**
 * @Author contributor
 * @Date 17:55 2023-03-23
 * @Description 查询区域
 **/
public enum QueryArea {
    RowDimension("rd"), ColumnDimension("cd"),Measure("m"), None("");

    private String shortCode;

    private QueryArea(String shortCode) {
        this.shortCode = shortCode;
    }

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public static QueryArea get(String str){
        for(QueryArea qa : values()){
            if(qa.toString().equalsIgnoreCase(str) || qa.getShortCode().equalsIgnoreCase(str)){
                return qa;
            }
        }
        return None;
    }

    public boolean isDimensionArea(){
        return this == RowDimension || this == ColumnDimension;
    }
}
