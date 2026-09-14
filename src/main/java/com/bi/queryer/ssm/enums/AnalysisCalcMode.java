package com.bi.queryer.ssm.enums;

/**
 * @Author contributor
 * @Date 14:50 2023-06-14
 * @Description 分析计算方式
 **/
public enum  AnalysisCalcMode {
    HB("hb", "环比", -1,7.0,"(当前期-上期)/上期"),
    TB_WEEK("tb_w", "周同比", -7,8.0,"(当前期-上周同期)/上周同期"),
    TB_MONTH("tb_m", "月同比", -1,9.0,"(当前期-上月同期)/上月同期"),

    TB_YEAR("tb_y", "年同(-1)", -1,10.0,"(当前期-去年同期)/去年同期"),
    TB_YEAR_2("tb_y_2", "年同(-2)", -2,11.0,"(当前期-前年同期)/前年同期"),
    TB_YEAR_3("tb_y_3", "年同(-3)",-3,12.0,"(当前期-3年前同期)/3年前同期"),

    TB_LN_YEAR("tb_ln_y","农年同(-1)",-1,1.0,"(当前期-去年农历同期)/去年农历同期"),
    TB_LN_YEAR_2("tb_ln_y_2","农年同(-2)",-2,2.0,"(当前期-前年农历同期)/前年农历同期"),
    TB_LN_YEAR_3("tb_ln_y_3","农年同(-3)",-3,3.0,"(当前期-3年前农历同期)/3年前农历同期"),

    TB_LN_YEAR_WEEK("tb_ln_yw","农可比(-1)",-1,4.0,"(当前期-去年农可比同期)/去年农可比同期"),
    TB_LN_YEAR_WEEK_2("tb_ln_yw_2","农可比(-2)",-2,5.0,"(当前期-前年农可比同期)/前年农可比同期"),
    TB_LN_YEAR_WEEK_3("tb_ln_yw_3","农可比(-3)",-3,6.0,"(当前期-3年前农可比同期)/3年前农可比同期"),

    TB_YEAR_WEEK("tb_yw", "年周同(-1)", -1,13.0,"(当前期-去年同周同星期)/去年同周同星期"),
    TB_YEAR_WEEK_2("tb_yw_2", "年周同(-2)", -2,14.0,"(当前期-前年同周同星期)/前年同周同星期"),
    TB_YEAR_WEEK_3("tb_yw_3", "年周同(-3)", -3,15.0,"(当前期-3年前同周同星期)/3年前同周同星期"),

    TB_PROMO_YEAR("tb_promo_y","同阶段年同-1",-1,16.0,"(当前期-去年同阶段)/去年同阶段"),
    TB_PROMO_YEAR_2("tb_promo_y_2","同阶段年同-2",-2, 17.0,"(当前期-前年同阶段)/前年同阶段"),
    TB_PROMO_YEAR_3("tb_promo_y_3","同阶段年同-3",-3,18.0,"(当前期-3年前同阶段)/3年前同阶段"),

    ZB_ROW_TOTAL("zb_rt", "占\"行总计\"", null,0.0,""),
    ZB_COL_TOTAL("zb_ct", "占\"列总计\"", null,0.0,""),
    ZB_COL_SUBTOTAL("zb_cs", "占\"列小计\"", null,0.0,""),
    ZB_WHOLE_TABLE_TOTAL("zb_wt", "占\"整表\"", null,0.0,""),

    ROW_TOTAL("r_total", "行总计", null,0.0,""),
    COL_TOTAL("c_total", "列总计", null,0.0,""),
    COL_SUBTOTAL("c_s_total", "列小计", null,0.0,""),
    WHOLE_TABLE_TOTAL("w_t_total", "整表总计", null,0.0,""),

    CUSTOM_COMPARE("c_cmp", "自定义对比", null, 19.0,""),

    CONTRIBUTION_RATE("ctr", "贡献率", null,0.0,""),

    ZB_THB("zb_thb","占比同环比",null,0.0,""),

    // 以下和目标值有关
    TARGET_VALUE("target", "目标", null,0.0,""),
    BASELINE_VALUE("baseline", "底线", null,0.0,""),
    CHALLENGE_VALUE("challenge", "挑战", null,0.0,""),
    TIME_PROGRESS("time_progress", "时间进度", null,0.0,""),
    PREDICT_VALUE("achieve_value", "预估达成", null,0.0,""),

    NONE("", "无", null,9999.0,""),

    UNKNOW("null", "未知", null,9999.0,"")
    ;

    private String desc;
    private String code ;
    private Integer offset;

    // 排序
    private Double sortId;

    //公式
    private String formula;

    private AnalysisCalcMode(String code, String desc, Integer offset,Double sortId,String formula) {
        this.code = code;
        this.desc = desc;
        this.offset = offset;
        this.sortId = sortId;
        this.formula = formula;
    }

    public static AnalysisCalcMode get(String str){
        for(AnalysisCalcMode m : values()){
            if(m.toString().equalsIgnoreCase(str) || m.code.equalsIgnoreCase(str)) {
                return m;
            }
        }
        return UNKNOW;
    }

    public boolean isHb(){
        return this.toString().toLowerCase().startsWith("hb");
    }

    public boolean isTb(){
        return this.toString().toLowerCase().startsWith("tb");
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

    public Double getSortId() {
        return sortId;
    }

    public void setSortId(Double sortId) {
        this.sortId = sortId;
    }

    public String getFormula() {
        return formula;
    }

    public void setFormula(String formula) {
        this.formula = formula;
    }

    /**
     * 是否是同环比
     * @return
     */
    public boolean isCompare(){
        switch (this){
            case HB:
            case TB_WEEK:
            case TB_MONTH:
            case TB_YEAR:
            case TB_YEAR_2:
            case TB_YEAR_3:
            case TB_LN_YEAR:
            case TB_LN_YEAR_2:
            case TB_LN_YEAR_3:
            case TB_YEAR_WEEK:
            case TB_YEAR_WEEK_2:
            case TB_YEAR_WEEK_3:
            case CUSTOM_COMPARE:
            case TB_PROMO_YEAR:
            case TB_PROMO_YEAR_2:
            case TB_PROMO_YEAR_3:
            case TB_LN_YEAR_WEEK:
            case TB_LN_YEAR_WEEK_2:
            case TB_LN_YEAR_WEEK_3:
                return true;
        }

        return false;
    }

    /**
     * 是否是占比
     * @return
     */
    public boolean isZb(){
        if(this == ZB_COL_SUBTOTAL || this == ZB_COL_TOTAL || this == ZB_ROW_TOTAL || this == ZB_WHOLE_TABLE_TOTAL){
            return true;
        }
        return false;
    }

    /**
     * 是否是总计
     * @return
     */
    public boolean isTotal(){
        if(this == COL_TOTAL || this == COL_SUBTOTAL || this == ROW_TOTAL || this == WHOLE_TABLE_TOTAL){
            return true;
        }
        return false;
    }

    /**
     * 是否是年同比
     * @return
     */
    public boolean isTby() {

        if (this == TB_YEAR || this == TB_YEAR_2 || this == TB_YEAR_3) {
            return true;
        }

        if (this == TB_LN_YEAR || this == TB_LN_YEAR_2 || this == TB_LN_YEAR_3) {
            return true;
        }

        return false;
    }

    /**
     * 是否是农历年同比
     * @return
     */
    public boolean isTblny() {

        if (this == TB_LN_YEAR || this == TB_LN_YEAR_2 || this == TB_LN_YEAR_3) {
            return true;
        }
        return false;
    }

    /**
     * 是否是农历年周同比
     * @return
     */
    public boolean isTblnyw(){

        if (this == TB_LN_YEAR_WEEK || this == TB_LN_YEAR_WEEK_2 || this == TB_LN_YEAR_WEEK_3) {
            return true;
        }

        return false;
    }

    /**
     * 是否是同阶段年同比
     * @return
     */
    public boolean isPromoTb() {
        if (this == TB_PROMO_YEAR || this == TB_PROMO_YEAR_2 || this == TB_PROMO_YEAR_3 ) {
            return true;
        }
        return false;
    }

    public boolean isTarget() {
        if (this == TARGET_VALUE) {
            return true;
        }
        if (this == BASELINE_VALUE) {
            return true;
        }
        if (this == CHALLENGE_VALUE) {
            return true;
        }
        if (this == TIME_PROGRESS) {
            return true;
        }
        return this == PREDICT_VALUE;
    }

    public Integer getOffset() {
        return offset;
    }
}
