package com.bi.queryer.ssm.enums;

/**
 * @Author contributor
 * @Date 14:56 2023-06-14
 * @Description 分析计算类型
 **/
public enum AnalysisCalcType {

    RATIO("ratio", "百分比",""),
    VALUE("value", "差值","差值"),
    REAL_VALUE("r_value", "实际值","实际值"),
    CONTRIBUTION_RATE("ctr", "贡献率", "贡献率"),
    P_RATIO("p_ratio", "预估达成率", "预估达成率"),
    UNKNOW("", "未知","");

    private String desc;
    private String code;
    private String title;

    private AnalysisCalcType(String code, String desc,String title) {
        this.code = code;
        this.desc = desc;
        this.title = title;
    }

    public static AnalysisCalcType get(String str){
        for(AnalysisCalcType m : values()){
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

    public boolean isRatio() {
        return this.equals(RATIO) || this.equals(CONTRIBUTION_RATE) || this.equals(P_RATIO);
    }
}
