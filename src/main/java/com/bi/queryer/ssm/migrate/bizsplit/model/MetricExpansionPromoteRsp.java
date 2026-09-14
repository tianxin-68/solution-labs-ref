package com.bi.queryer.ssm.migrate.bizsplit.model;

/**
 * 单视图指标膨胀上线 / 回滚结果。
 */
public class MetricExpansionPromoteRsp {

    /** 视图 id */
    private String viewId;
    /** 模板 id */
    private String tplId;
    /** 上线前正式 cfg id */
    private String oldCfgId;
    /** 上线后正式 cfg id */
    private String newCfgId;
    /** 影子配置 id */
    private String shadowCfgId;
    /** 是否跳过（无影子 / 已上线等） */
    private Boolean skipped;
    /** 失败信息 */
    private String errorMessage;

    public String getViewId() {
        return viewId;
    }

    public void setViewId(String viewId) {
        this.viewId = viewId;
    }

    public String getTplId() {
        return tplId;
    }

    public void setTplId(String tplId) {
        this.tplId = tplId;
    }

    public String getOldCfgId() {
        return oldCfgId;
    }

    public void setOldCfgId(String oldCfgId) {
        this.oldCfgId = oldCfgId;
    }

    public String getNewCfgId() {
        return newCfgId;
    }

    public void setNewCfgId(String newCfgId) {
        this.newCfgId = newCfgId;
    }

    public String getShadowCfgId() {
        return shadowCfgId;
    }

    public void setShadowCfgId(String shadowCfgId) {
        this.shadowCfgId = shadowCfgId;
    }

    public Boolean getSkipped() {
        return skipped;
    }

    public void setSkipped(Boolean skipped) {
        this.skipped = skipped;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
