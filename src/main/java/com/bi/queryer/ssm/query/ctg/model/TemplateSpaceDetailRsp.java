package com.bi.queryer.ssm.query.ctg.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 共享空间详情
 */
@Data
public class TemplateSpaceDetailRsp {

    /**
     * 目录ID
     */
    private String id;

    /**
     * 目录上级
     */
    private String parentId;

    /**
     * 目录名称
     */
    private String name;

    /**
     * 人员列表
     */
    private List<TemplateSpaceMember> userList = new ArrayList<>();

    /**
     * 部门列表
     */
    private List<TemplateSpaceMember> deptList = new ArrayList<>();

    // 是否是公域
    private Integer isInPubicDomain;
    // 是否管理员
    private Integer isAdmin;
}
