package com.bi.queryer.ssm.portal.template.enums;

public enum AnalysisTemplateWidgetType {

    UNKNOWN("unknown",""),
    FILTER("filter","筛选器"),
    GLOBALFILTERGROUP("globalFilterGroup","全局筛选组"),
    QUERYTEMPLATE("queryTemplate","查询模板"),
    STORYLINE("storyline","故事线"),
    TABLE("table","表格"),
    TEXT("text","文本");

    private String code;
    private String desc;

    AnalysisTemplateWidgetType(String code, String desc) {
        this.code = code;
        this.desc = desc;
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

    public static AnalysisTemplateWidgetType get(String code) {
        for(AnalysisTemplateWidgetType e : AnalysisTemplateWidgetType.values()) {
            if(e.toString().equalsIgnoreCase(code)) {
                return e;
            }
        }
        return UNKNOWN;
    }

}
