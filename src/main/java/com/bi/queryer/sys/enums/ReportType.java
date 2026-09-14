package com.bi.queryer.sys.enums;

/**
 * 报表类型
 * @author contributor
 */
public enum ReportType {

    TableAu("Tableau"),
    Java("Java"),
    Designer("设计器");

    private String desc;
    public String getDesc() {
        return desc;
    }
    public void setDesc(String desc) {
        this.desc = desc;
    }
    private ReportType(String desc) {
        this.desc = desc;
    }

    public static ReportType get(String code) {
        for(ReportType e : ReportType.values()) {
            if(e.toString().equalsIgnoreCase(code)) {
                return e;
            }
        }
        return TableAu;
    }
}
