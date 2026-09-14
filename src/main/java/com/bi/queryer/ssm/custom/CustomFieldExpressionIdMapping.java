package com.bi.queryer.ssm.custom;

import com.bi.queryer.ssm.engine.analysis.cfg.AnalysisItemConfig;

public class CustomFieldExpressionIdMapping implements Comparable{
    private String id;
    private String title;

    private String code;

    private String moduleCtgId;

    //分析指标的配置
    private AnalysisItemConfig analysisItemConfig;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public int compareTo(Object o) {
        if (o == null || id == null) {
            return -1;
        }
        return this.id.compareTo( ((CustomFieldExpressionIdMapping)o).id);
    }

    @Override
    public boolean equals(Object obj) {
        if(id == null) id = "";
        return id.equals(((CustomFieldExpressionIdMapping)obj).id);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getModuleCtgId() {
        return moduleCtgId;
    }

    public void setModuleCtgId(String moduleCtgId) {
        this.moduleCtgId = moduleCtgId;
    }

    public AnalysisItemConfig getAnalysisItemConfig() {
        return analysisItemConfig;
    }

    public void setAnalysisItemConfig(AnalysisItemConfig analysisItemConfig) {
        this.analysisItemConfig = analysisItemConfig;
    }
}
