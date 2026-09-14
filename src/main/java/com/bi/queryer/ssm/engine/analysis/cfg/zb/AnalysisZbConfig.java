package com.bi.queryer.ssm.engine.analysis.cfg.zb;

import cn.hutool.core.collection.CollUtil;
import com.bi.queryer.ssm.enums.PercentFieldRatioUnitType;
import com.bi.queryer.sys.enums.Enabled;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author contributor
 * @Date 16:39 2023-08-04
 * @Description 占比分析配置
 **/
public class AnalysisZbConfig {
    protected Integer isActive = Enabled.NO.getId();

    /**
     * 百分比指标计算方式
     */
    private String percentFieldRatioUnit = PercentFieldRatioUnitType.PT.getCode();

    private List<AnalysisZbItemConfig> items = new ArrayList<>();

    public Integer getIsActive() {
        return isActive;
    }

    public void setIsActive(Integer isActive) {
        this.isActive = isActive;
    }

    public List<AnalysisZbItemConfig> getItems() {
        return items;
    }

    public void setItems(List<AnalysisZbItemConfig> items) {
        this.items = items;
    }

    public boolean isActive(){
        boolean active = Enabled.value(this.isActive);
        if(!active || CollUtil.isEmpty(items)){
            return false;
        }

        return active;
    }

    public String getPercentFieldRatioUnit() {
        return percentFieldRatioUnit;
    }

    public void setPercentFieldRatioUnit(String percentFieldRatioUnit) {
        this.percentFieldRatioUnit = percentFieldRatioUnit;
    }
}
