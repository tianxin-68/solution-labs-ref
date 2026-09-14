package com.bi.queryer.ssm.portal.auth.vo.rsp;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: contributor
 * @CreateTime: 2024-06-18  10:08
 */
@Data
public class PortalAuthQueryRsp {

    /**
     * 权限分配的用户
     */
    public List<String> userNameList = new ArrayList<>();

    /**
     * 权限分配的组织
     */
    public List<String> deptIdList = new ArrayList<>();

}
