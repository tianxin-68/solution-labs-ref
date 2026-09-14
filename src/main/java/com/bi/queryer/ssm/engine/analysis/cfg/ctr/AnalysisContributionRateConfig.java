package com.bi.queryer.ssm.engine.analysis.cfg.ctr;

import cn.hutool.core.collection.CollUtil;
import com.bi.queryer.sys.enums.Enabled;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author contributor
 * @Date 09:52 2023-09-11
 * @Description 贡献率
 **/
public class AnalysisContributionRateConfig {
    protected Integer isActive = Enabled.NO.getId();

    private List<String> items = new ArrayList<>();

    public Integer getIsActive() {
        return isActive;
    }

    public void setIsActive(Integer isActive) {
        this.isActive = isActive;
    }

    public List<String> getItems() {
        return items;
    }

    public void setItems(List<String> items) {
        this.items = items;
    }

    public boolean isActive() {
        boolean active = Enabled.value(this.isActive);
        if (!active || CollUtil.isEmpty(items)) {
            return false;
        }

        return active;
    }
}
