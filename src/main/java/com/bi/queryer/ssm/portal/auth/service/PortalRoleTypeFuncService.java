package com.bi.queryer.ssm.portal.auth.service;

import com.bi.queryer.ssm.portal.auth.entity.PortalRoleFunc;
import com.bi.queryer.sys.base.BaseDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Author: contributor
 * @CreateTime: 2024-06-18  15:45
 * @Description: 角色功能服务类
 */
@Service
public class PortalRoleTypeFuncService {

    @Autowired
    private BaseDao dao;

    public List<String> queryRoleFuncCodes(List<String> roleTypes) {
        List<String> funcCodes = (List<String>) dao.queryObjectList("ssm.portal.role.type.func.queryRoleFuncCodes", roleTypes);
        return funcCodes;
    }

    public List<PortalRoleFunc> queryFuncCodeByRoleIds(List<String> roleIds) {
        List<PortalRoleFunc> portalRoleFuncList = (List<PortalRoleFunc>) dao.queryObjectList("ssm.portal.role.type.func.queryFuncCodeByRoleIds", roleIds);
        return portalRoleFuncList;
    }

}
