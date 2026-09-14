package com.bi.queryer.ssm.enums;

/**
 * @Author contributor
 * @Date 14:16 2025/9/24
 * @Description TODO
 **/
public enum DataSensitiveLevel {
    C1("c1",false,1),
    C2("c2", false,2),
    C3("c3", true,3),
    C4("c4", true, 4);

    private String code;
    private boolean isHigh;
    private Integer level;
    private DataSensitiveLevel(String code, boolean isHigh, Integer level){
        this.code = code;
        this.isHigh = isHigh;
        this.level = level;
    }

    public static DataSensitiveLevel get(String str){
        for(DataSensitiveLevel s : DataSensitiveLevel.values()){
            if(s.toString().equalsIgnoreCase(str)){
                return s;
            }
        }
        return C1;
    }

    public static DataSensitiveLevel get(Integer level){
        for(DataSensitiveLevel s : DataSensitiveLevel.values()){
            if(s.getLevel() == level){
                return s;
            }
        }
        return C1;
    }

    public boolean isHigh() {
        return isHigh;
    }

    public void setHigh(boolean high) {
        isHigh = high;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
