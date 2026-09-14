package com.bi.queryer.ssm.enums;

/**
 * @Author contributor
 * @Date 11:14 2023-07-26
 * @Description 分析-总计类型
 **/
public enum AnalysisTotalType {
    ROW_TOTAL("row_total", "行总计"),
    COL_TOTAL("col_total", "列总计"),
    COL_SUBTOTAL("col_subtotal", "列小计"),
    WHOLE_TABLE("whole_table", "整表总计"),
    NONE("", "无"),
    UNKNOW("null", "未知");

    private String desc;
    private String code ;

    private AnalysisTotalType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static AnalysisTotalType get(String str){
        for(AnalysisTotalType m : values()){
            if(m.toString().equalsIgnoreCase(str) || m.code.equalsIgnoreCase(str)) {
                return m;
            }
        }
        return UNKNOW;
    }

    public static AnalysisTotalType get(AnalysisCalcMode calcMode){
        switch (calcMode){
            case ZB_COL_TOTAL:
            case COL_TOTAL:
                return COL_TOTAL;

            case ZB_COL_SUBTOTAL:
            case COL_SUBTOTAL:
                return COL_SUBTOTAL;

            case ZB_ROW_TOTAL:
            case ROW_TOTAL:
                return ROW_TOTAL;

            case ZB_WHOLE_TABLE_TOTAL:
            case WHOLE_TABLE_TOTAL:
                return WHOLE_TABLE;
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

    public boolean isActive(){
        return this != NONE && this != UNKNOW;
    }
}
