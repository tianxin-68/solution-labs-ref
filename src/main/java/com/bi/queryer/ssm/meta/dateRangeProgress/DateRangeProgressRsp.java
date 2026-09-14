package com.bi.queryer.ssm.meta.dateRangeProgress;

import java.math.BigDecimal;

public class DateRangeProgressRsp {

    /**
     * 总天数
     */
    private Integer totalDays;

    /**
     * 剩余天数
     */
    private Integer leftDays;

    /**
     * 时间进度
     */
    private BigDecimal progress;

    public Integer getTotalDays() {
        return totalDays;
    }

    public void setTotalDays(Integer totalDays) {
        this.totalDays = totalDays;
    }

    public Integer getLeftDays() {
        return leftDays;
    }

    public void setLeftDays(Integer leftDays) {
        this.leftDays = leftDays;
    }

    public BigDecimal getProgress() {
        return progress;
    }

    public void setProgress(BigDecimal progress) {
        this.progress = progress;
    }
}
