package com.bi.queryer.ssm.portal.auth.service;

import cn.hutool.core.collection.CollUtil;
import com.bi.queryer.ssm.portal.auth.entity.PortalRole;
import com.bi.queryer.ssm.portal.auth.entity.PortalRoleAuth;
import com.bi.queryer.ssm.portal.enums.RoleType;
import com.bi.queryer.sys.base.AbstractTransaction;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.sys.role.RoleService;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.Guid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: contributor
 * @CreateTime: 2024-06-18  13:56
 * @Description: 门户角色服务类
 */
@Service
public class PortalRoleService {

    @Autowired
    private BaseDao dao;

    @Autowired
    private RoleService roleService;

    /**
     * 获取角色
     * @param resId  资源类型
     * @param roleType 角色类型
     * @return
     */
    public PortalRole queryPortalRole(String resId, String roleType) {

        Map<String, Object> authAddMap = new HashMap<>();
        authAddMap.put("resId", resId);
        authAddMap.put("roleType", roleType);

        //通过资源类型id和角色类型查询角色
        PortalRole portalRole = (PortalRole) dao.queryObject("ssm.portal.role.queryRole", authAddMap);

        if (portalRole == null) {
            throw new BIException("找不到对应的角色配置，请联系数据产品技术支持");
        }

        return portalRole;
    }

    /**
     * 通过角色id集合查询角色类型
     * @param roleIds
     * @return
     */
    public List<String> queryRoleTypesByRoleIds(List<String> roleIds){
        if (CollUtil.isEmpty(roleIds)) {
            return Collections.emptyList();
        }
        List<String> roleTypes = (List<String>)dao.queryObjectList("ssm.portal.role.queryRoleTypesByRoleIds",roleIds);
        return roleTypes;
    }

    public List<PortalRole> queryRolesByRoleIds(List<String> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return Collections.emptyList();
        }
        return dao.queryObjectList("ssm.portal.role.queryByRoleIds", roleIds, PortalRole.class);
    }

    /**
     * 初始化门户角色
     */
    public List<String> batchAddPortalRole(String portalId,String portalName) {

        String userName = UserManager.get().getName();

        List<PortalRole> portalRoleList = new ArrayList<>();
        List<String> roleIds = new ArrayList<>();

        for (RoleType roleType : RoleType.values()) {

            if (RoleType.UNKNOWN == roleType) {
                continue;
            }

            PortalRole portalRole = new PortalRole();

            String roleId = Guid.id();
            roleIds.add(roleId);
            portalRole.setRoleId(roleId);

            portalRole.setRoleName(String.format("%s-%s", portalName, roleType.getName()));
            portalRole.setRoleType(roleType.getCode());
            portalRole.setCreatedBy(userName);
            portalRole.setPortalId(portalId);

            portalRoleList.add(portalRole);

        }

        dao.insert("ssm.portal.role.batchAdd", portalRoleList);

        return roleIds;
    }

    /**
     * 确保指定资源已有 viewer 角色并绑定到 ssm_portal_role_auth；如已存在则直接返回 roleId。
     */
    public String ensureViewerRole(String resId, String resType, String resName) {
        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("resId", resId);
        queryMap.put("roleType", RoleType.PORTAL_VIEWER.getCode());

        String existingRoleId = (String) dao.queryObject("ssm.portal.role.queryViewerRoleId", queryMap);
        if (existingRoleId != null) {
            return existingRoleId;
        }

        String roleId = Guid.id();
        String createdBy = UserManager.get().getName();

        PortalRole role = new PortalRole();
        role.setRoleId(roleId);
        role.setRoleName(resName + "-" + RoleType.PORTAL_VIEWER.getName());
        role.setRoleType(RoleType.PORTAL_VIEWER.getCode());
        role.setPortalId(resId);
        role.setCreatedBy(createdBy);

        PortalRoleAuth roleAuth = new PortalRoleAuth();
        roleAuth.setRoleId(roleId);
        roleAuth.setResId(resId);
        roleAuth.setResType(resType);
        roleAuth.setActiveDurationDays(9999);

        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {
                dao.insert("ssm.portal.role.insertOne", role);
                List<PortalRoleAuth> authList = new ArrayList<>();
                authList.add(roleAuth);
                dao.insert("ssm.portal.role.auth.batchAdd", authList);
            }
        });

        return roleId;
    }

    /**
     * 查询指定资源的 viewer 角色 ID（不存在返回 null）。
     */
    public String queryViewerRoleId(String resId) {
        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("resId", resId);
        queryMap.put("roleType", RoleType.PORTAL_VIEWER.getCode());
        return (String) dao.queryObject("ssm.portal.role.queryViewerRoleId", queryMap);
    }

    /**
     * 是否是门户超级管理员
     * @return
     */
    public boolean isPortalSuperAdmin(){
        String userName = UserManager.get().getName();
        String roleId = SC.v("ssm.portal.super.admin.roleId", "");
        return roleService.queryRoleUserNameExist(roleId, userName);
    }

}
