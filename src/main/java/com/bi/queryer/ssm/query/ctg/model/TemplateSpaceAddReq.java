package com.bi.queryer.ssm.query.ctg.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 共享空间新增请求类
 */
@Data
public class TemplateSpaceAddReq {

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
}
