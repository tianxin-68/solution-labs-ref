package com.bi.queryer.ssm.migrate.bizsplit.model;

/**
 * 指标膨胀上线记录，对应 ssm_query_template_cfg_promote_log。
 */
public class MetricExpansionPromoteLogEntity {

    /** 已上线 */
    public static final String STATUS_PROMOTED = "PROMOTED";
    /** 已回滚 */
    public static final String STATUS_ROLLED_BACK = "ROLLED_BACK";

    /** 主键 */
    private Long pkid;
    /** 视图 id（唯一） */
    private String viewId;
    /** 模板 id */
    private String tplId;
    /** 影子配置 id */
    private String shadowCfgId;
    /** 上线前正式 cfg id */
    private String oldCfgId;
    /** 上线后正式 cfg id */
    private String newCfgId;
    /** 状态：PROMOTED / ROLLED_BACK */
    private String status;
    /** 创建人 */
    private String createdBy;
    /** 创建时间 */
    private String createdTime;
    /** 更新人 */
    private String updatedBy;
    /** 更新时间 */
    private String updatedTime;

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

    public String getShadowCfgId() {
        return shadowCfgId;
    }

    public void setShadowCfgId(String shadowCfgId) {
        this.shadowCfgId = shadowCfgId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getPkid() {
        return pkid;
    }

    public void setPkid(Long pkid) {
        this.pkid = pkid;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(String createdTime) {
        this.createdTime = createdTime;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(String updatedTime) {
        this.updatedTime = updatedTime;
    }
}
