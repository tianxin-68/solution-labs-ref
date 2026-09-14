package com.bi.queryer.ssm.portal.auth.service;

import cn.hutool.core.collection.CollUtil;
import com.bi.queryer.ssm.portal.auth.entity.PortalRoleUser;
import com.bi.queryer.sys.base.BaseDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Author: contributor
 * @CreateTime: 2024-06-18  13:59
 * @Description: 门户角色人员服务
 */
@Service
public class PortalRoleUserService {

    @Autowired
    private BaseDao dao;

    /**
     * 通过角色id删除角色人员
     * @param roleId
     */
    public void deleteRoleUserByRoleId(String roleId){
        dao.delete("ssm.portal.role.user.deleteRoleUserByRoleId",roleId);
    }

    /**
     *新增角色人员
     * @param portalRoleUserList
     */
    public void insertRoleUser(List<PortalRoleUser> portalRoleUserList){
        if(CollUtil.isEmpty(portalRoleUserList)){
            return;
        }

        dao.insert("ssm.portal.role.user.insertRoleUser",portalRoleUserList);
    }

    /**
     * 通过角色id查询角色人员
     * @param roleId
     * @return
     */
    public List<String> queryRoleUserByRoleId(String roleId) {
        List<String> userNameList = (List<String>) dao.queryObjectList("ssm.portal.role.user.queryRoleUserByRoleId", roleId);
        return userNameList;
    }

}
