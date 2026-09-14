package com.bi.queryer.ssm.engine.analysis.dataset;

import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.function.FunctionManager;
import com.bi.queryer.ssm.engine.function.IFunction;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;

/**
 * @Author contributor
 * @Date 17:24 2023-06-15
 * @Description 分析数据集
 **/
public abstract class AnalysisDataSet {

    protected final String namePrefix = "ds_";

    protected String name = "";

    protected AnalysisCalcMode calcMode;

    protected IFunction fx = null;

    protected String dateField = "";

    protected QueryConfigure config = null;

    public AnalysisDataSet(String dateField, AnalysisCalcMode calcMode){
        this.dateField = dateField;
        this.calcMode = calcMode;
        fx = FunctionManager.getFunction();
    }

    /**
     * 获取日期自动
     * @return
     */
    public abstract String getDateFieldExpression(String defaultValue);

    /**
     * 获取日期自动
     * @return
     */
    public String getDateFieldExpression(){
        return this.getDateFieldExpression(BIConsts.SSM_ALL);
    }

    public String getName() {
        return name;
    }

    public String getDateFieldFullName(){
        if(BIUtil.isEmpty(dateField)){
            return "";
        }
        if(dateField.contains(".")){
            return dateField;
        }
        return this.getName() + "." + dateField;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AnalysisCalcMode getCalcMode() {
        return calcMode;
    }

    public void setCalcMode(AnalysisCalcMode calcMode) {
        this.calcMode = calcMode;
    }

    public String getDateField() {
        return dateField;
    }

    public void setDateField(String dateField) {
        this.dateField = dateField;
    }

    public boolean isLunarDate() {

        if (this.config != null && this.config.isShowLunarDate()) {
            return true;
        }

        if (AnalysisCalcMode.TB_LN_YEAR == this.calcMode || AnalysisCalcMode.TB_LN_YEAR_2 == this.calcMode || AnalysisCalcMode.TB_LN_YEAR_3 == this.calcMode) {
            return true;
        }

        return false;
    }

    public QueryConfigure getConfig() {
        return config;
    }

    public void setConfig(QueryConfigure config) {
        this.config = config;
    }
}
