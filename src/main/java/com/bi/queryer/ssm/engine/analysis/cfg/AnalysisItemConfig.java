package com.bi.queryer.ssm.engine.analysis.cfg;

import com.bi.queryer.ssm.engine.result.ResultDataSetTargetConfig;
import com.bi.queryer.ssm.engine.result.ResultDataSetZbThbConfig;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;
import com.bi.queryer.ssm.enums.PercentFieldRatioUnitType;
import com.bi.queryer.util.BIUtil;
import com.alibaba.fastjson.JSONObject;

/**
 * @Author contributor
 * @Date 14:48 2023-06-14
 * @Description 分析配置
 **/
public class AnalysisItemConfig {
    protected String cmpId;

    // 日期字段编码
    protected String dateFieldCode = "";

    // 日期粒度
    protected String dateGranularity = "";

    // 分析指标
    protected String measureCode = "";

    // 分析指标id
    protected String measureId = "";

    protected String calcMode = "";

    // 计算方式
    protected String ctrCalcMode = "";

    // 计算类型
    protected String calcType = "";

    // 对比日期的差值
    protected Integer compareDatesInterval;

    //自定义对比索引
    protected Integer compareIndex;

    //自定义对比名称
    protected String compareTitle;

    // 原始计算方式：用于算子的计算方式改变导致原始计算方式变化，如：行总计的同环比
    protected String rawCalcMode = "";

    //百分比指标差异率设置 pt \ %
    protected String percentFieldRatioUnit;

    // 聚合表达式
    protected String aggExpressionType;

    //占比同环比配置
    protected ResultDataSetZbThbConfig zbThbConfig;

    protected ResultDataSetTargetConfig targetConfig;

    public String getCmpId() {
        return cmpId;
    }

    public void setCmpId(String cmpId) {
        this.cmpId = cmpId;
    }

    public String getDateFieldCode() {
        return dateFieldCode;
    }

    public void setDateFieldCode(String dateFieldCode) {
        this.dateFieldCode = dateFieldCode;
    }

    public String getMeasureCode() {
        return measureCode;
    }

    public void setMeasureCode(String measureCode) {
        this.measureCode = measureCode;
    }

    public String getCalcMode() {
        return calcMode;
    }

    public void setCalcMode(String calcMode) {
        this.calcMode = calcMode;
    }

    public String getCalcType() {
        return calcType;
    }

    public void setCalcType(String calcType) {
        this.calcType = calcType;
    }

    public String getDateGranularity() {
        return dateGranularity;
    }

    public void setDateGranularity(String dateGranularity) {
        this.dateGranularity = dateGranularity;
    }

    public String getMeasureId() {
        return measureId;
    }

    public void setMeasureId(String measureId) {
        this.measureId = measureId;
    }

    public Integer getCompareDatesInterval() {
        return compareDatesInterval;
    }

    public void setCompareDatesInterval(Integer compareDatesInterval) {
        this.compareDatesInterval = compareDatesInterval;
    }

    public Integer getCompareIndex() {
        return compareIndex;
    }

    public void setCompareIndex(Integer compareIndex) {
        this.compareIndex = compareIndex;
    }

    public AnalysisItemConfig clone(){
        return JSONObject.parseObject(BIUtil.toJSONString(this), this.getClass());
    }

    public String getRawCalcMode() {
        if(BIUtil.isEmpty(rawCalcMode)){
            return calcMode;
        }
        return rawCalcMode;
    }

    public void setRawCalcMode(String rawCalcMode) {
        this.rawCalcMode = rawCalcMode;
    }

    public String getCompareTitle() {
        return compareTitle;
    }

    public void setCompareTitle(String compareTitle) {
        this.compareTitle = compareTitle;
    }

    public String getPercentFieldRatioUnit() {
        return percentFieldRatioUnit;
    }

    public void setPercentFieldRatioUnit(String percentFieldRatioUnit) {
        this.percentFieldRatioUnit = percentFieldRatioUnit;
    }

    public String getAggExpressionType() {
        return aggExpressionType;
    }
    public void setAggExpressionType(String aggExpressionType) {
        this.aggExpressionType = aggExpressionType;
    }

    /**
     * 获取实际同环比的计算类型
     * @return
     */
    public AnalysisCalcMode getRawThbCalcMode() {
        return AnalysisCalcMode.get(calcMode);
    }

    public String getCtrCalcMode() {
        return ctrCalcMode;
    }

    public void setCtrCalcMode(String ctrCalcMode) {
        this.ctrCalcMode = ctrCalcMode;
    }

    public ResultDataSetZbThbConfig getZbThbConfig() {
        return zbThbConfig;
    }

    public void setZbThbConfig(ResultDataSetZbThbConfig zbThbConfig) {
        this.zbThbConfig = zbThbConfig;
    }

    public ResultDataSetTargetConfig getTargetConfig() {
        return targetConfig;
    }

    public void setTargetConfig(ResultDataSetTargetConfig targetConfig) {
        this.targetConfig = targetConfig;
    }

    public boolean isTargetValue() {
        return this.getTargetConfig() != null && this.getTargetConfig().isActive();
    }

    public String getDependFieldCode() {
        if (isTargetValue()) {
            return getMeasureCode() + getTargetConfig().getTargetCode();
        } else {
            return getMeasureCode();
        }
    }
}
