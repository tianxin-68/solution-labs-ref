package com.bi.queryer.ssm.custom;

/**
 * @Author contributor
 * @Date 15:37 2023-12-18
 * @Description 自定义字段类型
 **/
public enum CustomFieldType {
    COMMON("common", "", "常规"), LOD("lod", "lod:", "lod"), LOD_CALC("lod_calc", "lod:", "lod四则运算"),
    ANALYSIS("analysis", "analysis:", "分析指标")
    ;

    CustomFieldType(String code, String identifier, String desc) {
        this.code = code;
        this.identifier = identifier;
        this.desc = desc;
    }

    private String code;

    private String identifier;

    private String desc;

    public static CustomFieldType get(String str){
        for(CustomFieldType ct : values()){
            if(ct.toString().equalsIgnoreCase(str) || ct.getCode().equalsIgnoreCase(str)) {
                return ct;
            }
        }
        return COMMON;
    }

    public static CustomFieldType getByIdentifier(String str){
        for(CustomFieldType ct : values()){
            if(ct.getIdentifier().equalsIgnoreCase(str)) {
                return ct;
            }
        }
        return COMMON;
    }

    public static boolean isLod(String str){
        CustomFieldType t = get(str);
        return t == LOD || t == LOD_CALC;
    }


    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
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
}
