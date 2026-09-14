package com.bi.queryer.ssm.promotion.model;

import java.util.ArrayList;
import java.util.List;

public class PromotionDateRangeReq {

    /**
     * 活动日期单元格的值
     */
    private String promotionValue;

    /**
     * 配置的活动名称集合
     */
    private List<String> promotionConfigList = new ArrayList<>();

    /**
     * 数据截止时间
     */
    private String dataSnapshotDate;

    public String getPromotionValue() {
        return promotionValue;
    }

    public void setPromotionValue(String promotionValue) {
        this.promotionValue = promotionValue;
    }

    public List<String> getPromotionConfigList() {
        return promotionConfigList;
    }

    public void setPromotionConfigList(List<String> promotionConfigList) {
        this.promotionConfigList = promotionConfigList;
    }

    public String getDataSnapshotDate() {
        return dataSnapshotDate;
    }

    public void setDataSnapshotDate(String dataSnapshotDate) {
        this.dataSnapshotDate = dataSnapshotDate;
    }
}
