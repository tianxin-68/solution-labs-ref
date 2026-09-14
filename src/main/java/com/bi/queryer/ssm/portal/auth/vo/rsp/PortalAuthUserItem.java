package com.bi.queryer.ssm.portal.auth.vo.rsp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

/**
 * 有权限的用户清单条目
 */
@Data
public class PortalAuthUserItem {

    /** 用户名（域账号） */
    private String userName;

    /** 部门 */
    private String deptName;

    /**
     * 授权来源
     * individual=（本层）按个人授权 org=（本层）按组织授权 parent_dir=（从父层）继承父层权限
     * 同一用户多来源时逗号组合
     */
    private String authSource;

    /** 权限开始时间 */
    private String authStartTime;

    /** 权限结束时间（null=永久有效） */
    private String authEndTime;

    /** 内部字段：批量查询时用于 authSource 映射，不输出到 JSON */
    @JsonIgnore
    private String roleId;
}
