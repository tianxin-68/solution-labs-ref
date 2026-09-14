package com.bi.queryer.ssm.engine.accelerate.hot;

import com.bi.queryer.ssm.meta.accelerate.hot.HotTableInfo;
import com.bi.queryer.ssm.engine.model.StarModel;

/**
 * @Author contributor
 * @Date 19:24 2024/8/19
 * @Description 热星型模型
 **/
public class HotModel {
    private StarModel model;

    private HotTableInfo trinoHotTable = null;

    private HotTableInfo dorisHotTable = null;

    public HotModel(StarModel model) {
        this.model = model;
    }
    public StarModel getModel() {
        return model;
    }

    public void setModel(StarModel model) {
        this.model = model;
    }

    public HotTableInfo getTrinoHotTable() {
        return trinoHotTable;
    }

    public void setTrinoHotTable(HotTableInfo trinoHotTable) {
        this.trinoHotTable = trinoHotTable;
    }

    public HotTableInfo getDorisHotTable() {
        return dorisHotTable;
    }

    public void setDorisHotTable(HotTableInfo dorisHotTable) {
        this.dorisHotTable = dorisHotTable;
    }
}
