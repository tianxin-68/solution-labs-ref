package com.bi.queryer.ssm.promotion.model;

/**
 * 大促配置
 */
public class PromotionCfg {

    /**
     * 大促活动年份
     */
    private Integer promoYear;

    /**
     * 大促活动名称
     */
    private String promoName;

    /**
     * 大促活动阶段
     */
    private String promoPhase;

    /**
     * 大促标识 =年份+名称+阶段
     */
    private String promoIdentifier;

    /**
     * 大促活动名称排序
     */
    private Integer promoNameSortId;

    /**
     * 大促活动阶段排序
     */
    private Integer promoPhaseSortId;

    /**
     * 开始日期
     */
    private String startDate;

    /**
     * 结束日期
     */
    private String endDate;

    /**
     * 持续天数
     */
    private Integer durationDays;

    public Integer getPromoYear() {
        return promoYear;
    }

    public void setPromoYear(Integer promoYear) {
        this.promoYear = promoYear;
    }

    public String getPromoName() {
        return promoName;
    }

    public void setPromoName(String promoName) {
        this.promoName = promoName;
    }

    public String getPromoPhase() {
        return promoPhase;
    }

    public void setPromoPhase(String promoPhase) {
        this.promoPhase = promoPhase;
    }

    public String getPromoIdentifier() {
        return promoIdentifier;
    }

    public void setPromoIdentifier(String promoIdentifier) {
        this.promoIdentifier = promoIdentifier;
    }

    public Integer getPromoNameSortId() {
        return promoNameSortId;
    }

    public void setPromoNameSortId(Integer promoNameSortId) {
        this.promoNameSortId = promoNameSortId;
    }

    public Integer getPromoPhaseSortId() {
        return promoPhaseSortId;
    }

    public void setPromoPhaseSortId(Integer promoPhaseSortId) {
        this.promoPhaseSortId = promoPhaseSortId;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public Integer getDurationDays() {
        return durationDays;
    }

    public void setDurationDays(Integer durationDays) {
        this.durationDays = durationDays;
    }
}
