package com.bi.queryer.ssm.engine.config.field;

import com.bi.queryer.sys.enums.Enabled;

import java.util.ArrayList;
import java.util.List;

/**
 * 字段分类聚合
 */
public class FieldCategoryAggregation {

    private Integer enable = Enabled.NO.getId();

    /**
     * 聚合分类项
     */
    private List<FieldCategoryAggregationItem> categories = new ArrayList<>();

    public List<FieldCategoryAggregationItem> getCategories() {
        return categories;
    }

    public void setCategories(List<FieldCategoryAggregationItem> categories) {
        this.categories = categories;
    }

    public Integer getEnable() {
        return enable;
    }

    public void setEnable(Integer enable) {
        this.enable = enable;
    }

    public boolean isEmpty(){
        return categories.isEmpty();
    }
}
