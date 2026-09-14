package com.bi.queryer.ssm.engine.config.ui.check;

import com.bi.queryer.ssm.enums.*;
import com.bi.queryer.ssm.query.template.enums.DataTypeEnum;
import com.bi.queryer.sys.enums.Enabled;

public class UICheckQuerySettings {

    /**
     * 是否隐藏空行
     */
    private Integer isHideNullColumn = Enabled.NO.getId();

    /**
     * 时间粒度
     */
    private String dateGranularity = DateGranularity.DAY.getCode();

    /**
     * 是否为时间汇总
     */
    private Integer isAggQuery = Enabled.NO.getId();


    /**
     * 显示农历日期
     */
    private Integer showLunarDate = Enabled.NO.getId();


    /**
     * 数据集类型 实时还是离线
     */
    private DataTypeEnum datasetType = DataTypeEnum.OFFLINE;

    /**
     * 业务日历的年份
     */
    private Integer promoYear;

    /**
     * 是否显示对比日期
     */
    private Integer showDateRemark = Enabled.YES.getId();

    /**
     * 数据分片粒度
     */
    private String dataSliceGranularity = "";


    public Integer getIsHideNullColumn() {
        return isHideNullColumn;
    }

    public void setIsHideNullColumn(Integer isHideNullColumn) {
        this.isHideNullColumn = isHideNullColumn;
    }

    public String getDateGranularity() {
        return dateGranularity;
    }

    public void setDateGranularity(String dateGranularity) {
        this.dateGranularity = dateGranularity;
    }

    public Integer getIsAggQuery() {
        return isAggQuery;
    }

    public void setIsAggQuery(Integer isAggQuery) {
        this.isAggQuery = isAggQuery;
    }

    public Integer getShowLunarDate() {
        return showLunarDate;
    }

    public void setShowLunarDate(Integer showLunarDate) {
        this.showLunarDate = showLunarDate;
    }

    public DataTypeEnum getDatasetType() {
        return datasetType;
    }

    public void setDatasetType(DataTypeEnum datasetType) {
        this.datasetType = datasetType;
    }

    public Integer getPromoYear() {
        return promoYear;
    }

    public void setPromoYear(Integer promoYear) {
        this.promoYear = promoYear;
    }

    public Integer getShowDateRemark() {
        return showDateRemark;
    }

    public void setShowDateRemark(Integer showDateRemark) {
        this.showDateRemark = showDateRemark;
    }

    public String getDataSliceGranularity() {
        return dataSliceGranularity;
    }

    public void setDataSliceGranularity(String dataSliceGranularity) {
        this.dataSliceGranularity = dataSliceGranularity;
    }
}
