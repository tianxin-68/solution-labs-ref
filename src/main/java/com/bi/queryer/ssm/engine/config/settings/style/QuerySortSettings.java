package com.bi.queryer.ssm.engine.config.settings.style;

import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.sys.enums.Enabled;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author contributor
 * @Date 14:23 2023-11-28
 * @Description 查询排序设置
 **/
public class QuerySortSettings {
    private Integer isActive = Enabled.YES.getId();

    private List<QuerySortItem> sortItems = new ArrayList<>();

    public Integer getIsActive() {
        return isActive;
    }

    public void setIsActive(Integer isActive) {
        this.isActive = isActive;
    }

    public List<QuerySortItem> getSortItems() {
        return sortItems;
    }

    public void setSortItems(List<QuerySortItem> sortItems) {
        this.sortItems = sortItems;
    }
}
