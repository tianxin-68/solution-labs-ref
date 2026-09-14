package com.bi.queryer.ssm.engine.analysis.cfg.compare;

import com.bi.queryer.ssm.engine.analysis.cfg.thb.AnalysisZbThbConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: contributor
 * @CreateTime: 2023-08-23  17:40
 * @Description: 自定义对比明细项
 */
public class AnalysisCompareItemConfig implements Cloneable{

    //自定义对比配置项Id
    private String id;

    /**
     * 索引
     */
    private Integer compareIndex = -1;

    /**
     * 标题
     */
    private String title;

    /**
     * 指标id集合
     */
    private List<String> measureIdList = new ArrayList<>();

    /**
     * 时间粒度
     */
    private String dateGranularity;

    /**
     * 基准日期
     */
    private List<String> baseDates = new ArrayList<>();

    /**
     * 对比日期
     */
    private List<String> compareDates = new ArrayList<>();

    /**
     * 计算类型
     */
    private List<String> calcTypes = new ArrayList<>();

    /**
     * 计算模式
     */
    private String calcMode ;

    /**
     * 占比自定义对比配置
     */
    private List<AnalysisZbThbConfig> zbThbConfigs = new ArrayList<>();

    private String percentFieldRatioUnit;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getMeasureIdList() {
        return measureIdList;
    }

    public void setMeasureIdList(List<String> measureIdList) {
        this.measureIdList = measureIdList;
    }

    public String getDateGranularity() {
        return dateGranularity;
    }

    public void setDateGranularity(String dateGranularity) {
        this.dateGranularity = dateGranularity;
    }

    public List<String> getBaseDates() {
        return baseDates;
    }

    public void setBaseDates(List<String> baseDates) {
        this.baseDates = baseDates;
    }

    public List<String> getCompareDates() {
        return compareDates;
    }

    public void setCompareDates(List<String> compareDates) {
        this.compareDates = compareDates;
    }

    public List<String> getCalcTypes() {
        return calcTypes;
    }

    public void setCalcTypes(List<String> calcTypes) {
        this.calcTypes = calcTypes;
    }

    public String getCalcMode() {
        return calcMode;
    }

    public void setCalcMode(String calcMode) {
        this.calcMode = calcMode;
    }

    public Integer getCompareIndex() {
        return compareIndex;
    }

    public void setCompareIndex(Integer compareIndex) {
        this.compareIndex = compareIndex;
    }

    public List<AnalysisZbThbConfig> getZbThbConfigs() {
        return zbThbConfigs;
    }

    public void setZbThbConfigs(List<AnalysisZbThbConfig> zbThbConfigs) {
        this.zbThbConfigs = zbThbConfigs;
    }

    public String getPercentFieldRatioUnit() {
        return percentFieldRatioUnit;
    }

    public void setPercentFieldRatioUnit(String percentFieldRatioUnit) {
        this.percentFieldRatioUnit = percentFieldRatioUnit;
    }

    public AnalysisCompareItemConfig clone()  {
        try {
            return (AnalysisCompareItemConfig)super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return new AnalysisCompareItemConfig();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
