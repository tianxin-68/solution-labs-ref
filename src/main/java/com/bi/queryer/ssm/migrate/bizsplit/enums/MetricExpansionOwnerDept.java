package com.bi.queryer.ssm.migrate.bizsplit.enums;

import cn.hutool.core.util.StrUtil;

/**
 * 指标膨胀 owner 组织与目标业务线枚举。
 *
 * 用于未指定 targetBusinessline 时，按视图 owner 的 DEPT_ID 收窄映射：
 * - {@link #BI}：公共视图 tplOwner 中 DEPT_ID 包含此部门的人先剔除
 * - {@link #OIL} / {@link #PARTS} / {@link #CAR_MODIFY} 等：命中后仅保留对应 target_businessline
 *
 * targetBusinessline 须与 ssm_metric_expansion_mapping.target_businessline 一致。
 */
public enum MetricExpansionOwnerDept {

    /** BI 部门：从公共视图 tplOwner 剔除后再判业务线，无对应目标业务线 */
    BI("200704", null),
    /** 保养油液：DEPT_ID 包含此部门则只保留油液类 target（源业务线=保养） */
    OIL("701485", "保养油液", "保养"),
    /** 保养配件：DEPT_ID 包含此部门则只保留配件类 target（源业务线=保养） */
    PARTS("701490", "保养配件", "保养"),
    /** 超市改装：DEPT_ID 包含此部门则只保留超市改装 target（源业务线=改装超市） */
    CAR_MODIFY("100290", "超市改装", "改装超市"),
    /** 电子改装：DEPT_ID 包含此部门则只保留电子改装 target（源业务线=改装超市） */
    ELECTRONIC_MODIFY("701484", "电子改装", "改装超市"),
    /** 电瓶车：DEPT_ID 包含此部门则只保留电瓶车 target（源业务线=改装超市） */
    TWO_WHEEL("701424", "电瓶车", "改装超市");

    /** 组织部门 id */
    private final String deptId;
    /** 映射表目标业务线；BI 为 null，不参与业务线收窄 */
    private final String targetBusinessline;
    /** 适用的源业务线（与入参 sourceBusinessline / 映射表 source_businessline 一致） */
    private final String sourceBusinessline;

    MetricExpansionOwnerDept(String deptId, String targetBusinessline, String sourceBusinessline) {
        this.deptId = deptId;
        this.targetBusinessline = targetBusinessline;
        this.sourceBusinessline = sourceBusinessline;
    }

    /** BI 等不参与业务线收窄的部门 */
    MetricExpansionOwnerDept(String deptId, String targetBusinessline) {
        this(deptId, targetBusinessline, null);
    }

    /**
     * @return 组织部门 id
     */
    public String getDeptId() {
        return deptId;
    }

    /**
     * @return 目标业务线；BI 返回 null
     */
    public String getTargetBusinessline() {
        return targetBusinessline;
    }

    /**
     * @return 适用的源业务线；BI 返回 null
     */
    public String getSourceBusinessline() {
        return sourceBusinessline;
    }

    /**
     * 是否参与按业务线收窄（排除仅用于剔除的 BI）。
     *
     * @return true 表示有对应 target_businessline
     */
    public boolean hasTargetBusinessline() {
        return StrUtil.isNotEmpty(targetBusinessline);
    }
}
