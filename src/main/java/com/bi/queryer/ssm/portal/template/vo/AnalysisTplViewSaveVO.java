package com.bi.queryer.ssm.portal.template.vo;

import java.util.List;

public class AnalysisTplViewSaveVO extends AnalysisTplViewVO{

    /**
     * 组件配置
     */
    private List<WidgetNodeVO> widgetConfigs;

    /**
     * 若设置，批量保存组件时从该 analysis_tpl_cfg_id 对应组件行填充 query_tpl_id、query_tpl_viewid_mapping（快照场景为源视图配置），
     * 否则仍按看板 analysis_tpl_id 的线上版本填充
     */
    private String queryTplFillSourceCfgId;

    public List<WidgetNodeVO> getWidgetConfigs() {
        return widgetConfigs;
    }

    public void setWidgetConfigs(List<WidgetNodeVO> widgetConfigs) {
        this.widgetConfigs = widgetConfigs;
    }

    public String getQueryTplFillSourceCfgId() {
        return queryTplFillSourceCfgId;
    }

    public void setQueryTplFillSourceCfgId(String queryTplFillSourceCfgId) {
        this.queryTplFillSourceCfgId = queryTplFillSourceCfgId;
    }

}
