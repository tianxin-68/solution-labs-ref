package com.bi.queryer.ssm.portal.auth.service;

import cn.hutool.core.collection.CollUtil;
import com.bi.queryer.ssm.portal.auth.entity.PortalRoleDept;
import com.bi.queryer.sys.base.BaseDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Author: contributor
 * @CreateTime: 2024-06-18  13:59
 * @Description: 门户角色组织服务
 */
@Service
public class PortalRoleDeptService {

    @Autowired
    private BaseDao dao;


    /**
     * 通过角色id删除角色组织
     *
     * @param roleId
     */
    public void deleteRoleDeptByRoleId(String roleId) {
        dao.delete("ssm.portal.role.dept.deleteRoleDeptByRoleId", roleId);
    }


    /**
     * 新增角色组织
     *
     * @param portalRoleDeptList
     */
    public void insertRoleDept(List<PortalRoleDept> portalRoleDeptList) {
        if (CollUtil.isEmpty(portalRoleDeptList)) {
            return;
        }

        dao.insert("ssm.portal.role.dept.insertRoleDept", portalRoleDeptList);
    }

    /**
     * 通过角色id查询角色组织
     *
     * @param roleId
     * @return
     */
    public List<String> queryRoleDeptByRoleId(String roleId) {
        List<String> deptIdList = (List<String>) dao.queryObjectList("ssm.portal.role.dept.queryRoleDeptByRoleId", roleId);
        return deptIdList;
    }

}
