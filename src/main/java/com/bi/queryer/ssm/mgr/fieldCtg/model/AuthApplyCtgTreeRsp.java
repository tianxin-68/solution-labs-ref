package com.bi.queryer.ssm.mgr.fieldCtg.model;

import com.bi.queryer.sys.enums.Enabled;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 权限申请目录树响应节点
 */
@Data
public class AuthApplyCtgTreeRsp {

    /**
     * 目录ID
     */
    private String ctgId;

    /**
     * 目录名称
     */
    private String ctgName;

    /**
     * 分析师域账号
     */
    private String rptDevOwner;

    /**
     * 分析师展示名（真实姓名(域账号)）
     */
    private String rptDevOwnerDesc;

    /**
     * 业务负责人域账号
     */
    private String bizOwner;

    /**
     * 业务负责人展示名（真实姓名(域账号)）
     */
    private String bizOwnerDesc;

    /**
     * 敏感等级
     */
    private String sensitiveLevel;

    /**
     * 使用场景说明
     */
    private String useSceneDesc;

    /**
     * 是否继承父目录权限
     */
    private Integer isInherited;

    /**
     * 是否拥有权限
     */
    private Integer hasAuth = Enabled.NO.getId();

    /**
     * 权限结束时间
     */
    private String authEndDate;

    /**
     * 目录所需权限维度数据（dimCode -> 允许的取值编码列表）
     */
    private Map<String, List<String>> authMap;

    /**
     * 子目录
     */
    private List<AuthApplyCtgTreeRsp> children = new ArrayList<>();
}
