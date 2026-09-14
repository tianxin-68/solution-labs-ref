package com.bi.queryer.ssm.portal.auth.service;

import cn.hutool.core.collection.CollUtil;
import com.bi.queryer.ssm.portal.auth.entity.PortalRoleAuth;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: contributor
 * @CreateTime: 2024-06-18  15:19
 * @Description: 角色资源权限
 */
@Service
public class PortalRoleAuthService {

    @Autowired
    private BaseDao dao;

    public List<String> queryUserResourceRoleIds(String resId,String resType) {
        User user = UserManager.get();

        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("userName", user.getName());
        queryMap.put("deptId", user.getDeptId());
        queryMap.put("resId", resId);
        queryMap.put("resType", resType);

        List<String> roleIds = (List<String>) dao.queryObjectList("ssm.portal.role.auth.queryUserResourceRoleIds", queryMap);
        return roleIds;
    }

    /**
     * 获取用户所有的角色权限
     * @return
     */
    public List<PortalRoleAuth> queryUserAllRoleAuth() {

        User user = UserManager.get();

        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("userName", user.getName());
        queryMap.put("deptId", user.getDeptId());

        List<PortalRoleAuth> portalRoleAuthList = (List<PortalRoleAuth>) dao.queryObjectList("ssm.portal.role.auth.queryUserAllRoleAuth", queryMap);
        return portalRoleAuthList;
    }

    /**
     * 批量新增角色权限
     */
    public void batchAddPortalRoleAuth(List<String> roleIds,String resId,String resType) {

        if (CollUtil.isEmpty(roleIds)) {
            return;
        }

        List<PortalRoleAuth> portalRoleAuthList = new ArrayList<>();

        for (String roleId : roleIds) {
            PortalRoleAuth portalRoleAuth = new PortalRoleAuth();

            portalRoleAuth.setRoleId(roleId);
            portalRoleAuth.setResId(resId);
            portalRoleAuth.setResType(resType);
            portalRoleAuth.setActiveDurationDays(9999);

            portalRoleAuthList.add(portalRoleAuth);
        }

        dao.insert("ssm.portal.role.auth.batchAdd",portalRoleAuthList);

    }


}
