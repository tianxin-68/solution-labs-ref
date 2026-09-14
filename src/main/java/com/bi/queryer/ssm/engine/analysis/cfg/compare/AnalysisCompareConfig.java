package com.bi.queryer.ssm.engine.analysis.cfg.compare;

import cn.hutool.core.collection.CollUtil;
import com.bi.queryer.sys.enums.Enabled;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: contributor
 * @CreateTime: 2023-08-23  17:39
 * @Description: 自定义对比配置
 */
public class AnalysisCompareConfig {

    protected Integer isActive = Enabled.NO.getId();

    private List<AnalysisCompareItemConfig> items = new ArrayList<>();


    public boolean isActive(){
        boolean active = Enabled.value(this.isActive);
        if(!active || CollUtil.isEmpty(items)){
            return false;
        }

        return active;
    }

    public Integer getIsActive() {
        return isActive;
    }

    public void setIsActive(Integer isActive) {
        this.isActive = isActive;
    }

    public List<AnalysisCompareItemConfig> getItems() {
        return items;
    }

    public void setItems(List<AnalysisCompareItemConfig> items) {
        this.items = items;
    }
}
