package com.bi.queryer.ssm.migrate.bizsplit.model;

import java.util.List;

/**
 * 批量视图指标膨胀执行请求。
 *
 * 字段说明：
 * - viewIds：必填，按顺序处理；服务端按每批 100 个分批加载，避免 IN 参数过多
 * - sourceBusinessline：必填，每次只处理一个源业务线
 * - targetBusinessline：可选
 *   - 有值：门户替换场景，SQL 直接按目标业务线过滤映射；filter 与指标膨胀替换源数据（不保留）
 *   - 无值：按视图 owner 的 DEPT_ID 收窄；filter 与指标膨胀保留原始数据并追加 target
 */
public class MetricExpansionExecuteReq {

    /** 视图 id 列表（必填；服务端按每批 100 分批处理，结果顺序与入参一致） */
    private List<String> viewIds;
    /** 源业务线（必填；每次执行只处理一个，如「保养」） */
    private String sourceBusinessline;
    /** 目标业务线（可选；门户指定如「保养油液」；未指定则按 owner 组织收窄） */
    private String targetBusinessline;

    public List<String> getViewIds() {
        return viewIds;
    }

    public void setViewIds(List<String> viewIds) {
        this.viewIds = viewIds;
    }

    public String getSourceBusinessline() {
        return sourceBusinessline;
    }

    public void setSourceBusinessline(String sourceBusinessline) {
        this.sourceBusinessline = sourceBusinessline;
    }

    public String getTargetBusinessline() {
        return targetBusinessline;
    }

    public void setTargetBusinessline(String targetBusinessline) {
        this.targetBusinessline = targetBusinessline;
    }
}
