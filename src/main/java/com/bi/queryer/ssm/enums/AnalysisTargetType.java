package com.bi.queryer.ssm.enums;

/**
 * @Auther: contributor
 * @Date: 2025/12/13 10:50
 * @Description:
 */
public enum AnalysisTargetType {
    TARGET("target", "目标值","目标值"),
    BASELINE("baseline", "底线值","底线值"),
    CHALLENGE("challenge", "挑战值","挑战值"),
    UNKNOW("", "未知","");

    private String desc;
    private String code;
    private String title;

    private AnalysisTargetType(String code, String desc,String title) {
        this.code = code;
        this.desc = desc;
        this.title = title;
    }

    public static AnalysisTargetType get(String str){
        for(AnalysisTargetType m : values()){
            if(m.toString().equalsIgnoreCase(str) || m.code.equalsIgnoreCase(str)) {
                return m;
            }
        }
        return UNKNOW;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
