package com.bi.queryer.ssm.engine.analysis.cfg.thb;

import cn.hutool.core.collection.CollUtil;
import com.bi.queryer.sys.enums.Enabled;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author contributor
 * @Date 16:40 2023-08-04
 * @Description 同环比分析配置
 **/
public class AnalysisThbConfig {

    protected Integer isActive = Enabled.NO.getId();

    private List<AnalysisThbItemConfig> items = new ArrayList<>();

    public Integer getIsActive() {
        return isActive;
    }

    public void setIsActive(Integer isActive) {
        this.isActive = isActive;
    }

    public List<AnalysisThbItemConfig> getItems() {
        return items;
    }

    public void setItems(List<AnalysisThbItemConfig> items) {
        this.items = items;
    }


    public boolean isActive(){
        boolean active = Enabled.value(this.isActive);
        if(!active || CollUtil.isEmpty(items)){
            return false;
        }

        return active;
    }
}
