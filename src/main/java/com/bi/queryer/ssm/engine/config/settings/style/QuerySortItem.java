package com.bi.queryer.ssm.engine.config.settings.style;

import com.bi.queryer.sys.enums.Enabled;

/**
 *     {
 *             "orderEntity": "AAA",
 *             "orderBy": "O_ORD_00199/环比",
 *             "orderCategory": "轮胎",
 *             "orderType": "desc",
 *             "columnField": "O_ORD_00199_hb_ratio__c__d_0_1"
 *     }
 */
public class QuerySortItem {

    private String orderEntity;

    private String orderBy;

    private String orderCategory;

    private String orderType;

    private String columnField;

    //null值是不是排在最后
    private Integer isNullsLast = Enabled.NO.getId();

    public String getOrderEntity() {
        return orderEntity;
    }

    public void setOrderEntity(String orderEntity) {
        this.orderEntity = orderEntity;
    }

    public String getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }

    public String getOrderCategory() {
        return orderCategory;
    }

    public void setOrderCategory(String orderCategory) {
        this.orderCategory = orderCategory;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public String getColumnField() {
        return columnField;
    }

    public void setColumnField(String columnField) {
        this.columnField = columnField;
    }

    public Integer getIsNullsLast() {
        return isNullsLast;
    }

    public void setIsNullsLast(Integer isNullsLast) {
        this.isNullsLast = isNullsLast;
    }
}
