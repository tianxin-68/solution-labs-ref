package com.bi.queryer.ssm.portal.enums;

/**
 * {@code ssm_analysis_tpl_visit_log.visit_type}：区分看板与专题分析报告等访问对象。
 */
public enum AnalysisTplVisitType {

    ANALYSIS_TEMPLATE("analysis_template", "看板"),
    DYNAMIC_ANALYSIS_REPORT("dynamic_analysis_report", "专题分析报告");

    private final String code;
    private final String name;

    AnalysisTplVisitType(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
