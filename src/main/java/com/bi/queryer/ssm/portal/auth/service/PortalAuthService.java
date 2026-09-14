package com.bi.queryer.ssm.portal.auth.service;

import cn.hutool.core.collection.CollUtil;
import com.bi.queryer.ssm.portal.auth.entity.*;
import com.bi.queryer.ssm.portal.auth.vo.req.PortalAuthAddReq;
import com.bi.queryer.ssm.portal.auth.vo.req.PortalAuthBaseReq;
import com.bi.queryer.ssm.portal.auth.vo.req.PortalAuthQueryReq;
import com.bi.queryer.ssm.portal.auth.vo.req.PortalAuthUserListReq;
import com.bi.queryer.ssm.portal.auth.vo.rsp.PortalAuthUserItem;
import com.bi.queryer.ssm.portal.auth.vo.rsp.PortalResourceAuthGetRsp;
import com.bi.queryer.ssm.portal.entity.Portal;
import com.bi.queryer.ssm.portal.entity.PortalMenu;
import com.bi.queryer.ssm.portal.enums.AuthSourceType;
import com.bi.queryer.ssm.portal.enums.FuncType;
import com.bi.queryer.ssm.portal.enums.ResType;
import com.bi.queryer.ssm.portal.enums.RoleType;
import com.bi.queryer.ssm.portal.vo.rsp.PortalRsp;
import com.bi.queryer.sys.base.AbstractTransaction;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.common.SSMResponseMessage;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.util.excelUtil.ExcelHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: contributor
 * @CreateTime: 2024-06-17  17:41
 * @Description: 门户权限服务
 */
@Service
public class PortalAuthService {


    @Autowired
    private BaseDao dao;

    @Autowired
    private PortalRoleService portalRoleService;

    @Autowired
    private PortalRoleUserService portalRoleUserService;

    @Autowired
    private PortalRoleDeptService portalRoleDeptService;

    @Autowired
    private PortalRoleAuthService portalRoleAuthService;

    @Autowired
    private PortalRoleTypeFuncService portalRoleTypeFuncService;

    /**
     * 权限保存（门户 / 文件夹 / 看板 通用）。
     * portal 类型：校验 portal 存在 + 操作权限，角色必须已预创建。
     * 文件夹 / 看板：懒创建 viewer 角色，无需预先初始化；额外支持继承配置。
     */
    public SSMResponseMessage save(PortalAuthAddReq req) {

        boolean isPortal = ResType.PORTAL.getCode().equalsIgnoreCase(req.getResType());
        String createdBy = UserManager.get().getName();
        String roleId;

        if (isPortal) {
            Portal portal = (Portal) dao.queryObject("ssm.portal.get", req.getResId());
            if (portal == null) {
                throw new BIException("授权的门户不存在，请刷新页面再操作！");
            }
            if (!check(req.getResId(), req.getResType(), FuncType.ADD_AUTH.getCode())) {
                throw new BIException(String.format("您没有%s【授权】的功能权限", portal.getPortalName()));
            }
            roleId = portalRoleService.queryPortalRole(req.getResId(), req.getRoleType()).getRoleId();
        } else {
            // 文件夹 / 看板：viewer 角色懒创建（首次调用自动初始化）
            roleId = portalRoleService.ensureViewerRole(req.getResId(), req.getResType(), req.getResId());
        }

        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {
                portalRoleUserService.deleteRoleUserByRoleId(roleId);
                portalRoleDeptService.deleteRoleDeptByRoleId(roleId);

                if (CollUtil.isNotEmpty(req.getUserNameList())) {
                    List<PortalRoleUser> users = new ArrayList<>();
                    for (String userName : req.getUserNameList()) {
                        users.add(new PortalRoleUser(roleId, userName, createdBy));
                    }
                    portalRoleUserService.insertRoleUser(users);
                }

                if (CollUtil.isNotEmpty(req.getDeptIdList())) {
                    List<PortalRoleDept> depts = new ArrayList<>();
                    for (String deptId : req.getDeptIdList()) {
                        depts.add(new PortalRoleDept(roleId, deptId, createdBy));
                    }
                    portalRoleDeptService.insertRoleDept(depts);
                }

                if (!isPortal && req.getInheritEnabled() != null) {
                    PortalResourceAuthInherit inherit = new PortalResourceAuthInherit();
                    inherit.setPortalId(resolvePortalId(req));
                    inherit.setResId(req.getResId());
                    inherit.setResType(req.getResType());
                    inherit.setInheritEnabled(req.getInheritEnabled());
                    inherit.setCreatedBy(createdBy);
                    inherit.setUpdatedBy(createdBy);
                    dao.insert("ssm.portal.resource.auth.inherit.insertOrUpdate", inherit);
                }
            }
        });

        return SSMResponseMessage.success("新增成功！");
    }

    /**
     * 解析资源所属门户ID：优先取请求参数，未传时从 ssm_portal_menu 反查
     * （文件夹按 menu_id，看板按 content_ref_id）。
     */
    private String resolvePortalId(PortalAuthAddReq req) {
        PortalMenu menu = (PortalMenu) dao.queryObject("ssm.portal.menu.get", req.getResId());
        return menu != null ? menu.getPortalId() : null;
    }

    /**
     * 获取本层授权（门户 / 文件夹 / 看板 通用）。
     * portal 类型：按 roleType 查询；文件夹 / 看板：查询 viewer 角色，并返回继承配置。
     */
    public PortalResourceAuthGetRsp get(PortalAuthQueryReq req) {
        PortalResourceAuthGetRsp rsp = new PortalResourceAuthGetRsp();
        rsp.setResId(req.getResId());
        rsp.setResType(req.getResType());

        if (ResType.PORTAL.getCode().equalsIgnoreCase(req.getResType())) {
            String roleId = portalRoleService.queryPortalRole(req.getResId(), req.getRoleType()).getRoleId();
            rsp.setUserNameList(portalRoleUserService.queryRoleUserByRoleId(roleId));
            rsp.setDeptIdList(portalRoleDeptService.queryRoleDeptByRoleId(roleId));
        } else {
            String roleId = portalRoleService.queryViewerRoleId(req.getResId());
            if (roleId != null) {
                rsp.setUserNameList(portalRoleUserService.queryRoleUserByRoleId(roleId));
                rsp.setDeptIdList(portalRoleDeptService.queryRoleDeptByRoleId(roleId));
            }

            // 返回是否继承：未配置时默认继承（rsp 中 inheritEnabled 默认值为 1）
            Map<String, Object> inheritQuery = new HashMap<>();
            inheritQuery.put("resId", req.getResId());
            inheritQuery.put("resType", req.getResType());
            PortalResourceAuthInherit inherit = (PortalResourceAuthInherit) dao.queryObject(
                    "ssm.portal.resource.auth.inherit.queryByResId", inheritQuery);
            if (inherit != null && inherit.getInheritEnabled() != null) {
                rsp.setInheritEnabled(inherit.getInheritEnabled());
            }

            // 父级信息从菜单树推导（继承按目录结构，不再落库存储）
            PortalMenu menu = (PortalMenu) dao.queryObject("ssm.portal.menu.get", req.getResId());
            if (menu != null && menu.getParentMenuId() != null) {
                rsp.setParentResId(menu.getParentMenuId());
                rsp.setParentResType(menu.getParentMenuId().equals(menu.getPortalId())
                        ? ResType.PORTAL.getCode()
                        : ResType.PORTAL_MENU.getCode());
            }
        }

        // 有效用户数 = 「有权限的用户清单」接口（含继承层）的行数，保证两处展示一致
        PortalAuthUserListReq userListReq = new PortalAuthUserListReq();
        userListReq.setResId(req.getResId());
        userListReq.setResType(req.getResType());
        rsp.setEffectiveUserCount(getUserAuthList(userListReq).size());

        return rsp;
    }

    /**
     * 权限校验
     * @return
     */
    public boolean check(String resId,String resType,String funcCode) {

        boolean hasAuth = false;

        //超级管理员 直接返回有权限
        if (portalRoleService.isPortalSuperAdmin()) {
            return true;
        }


        //查询用户的资源角色id
        List<String> roleIds = portalRoleAuthService.queryUserResourceRoleIds(resId, resType);
        if (CollUtil.isEmpty(roleIds)) {
            return false;
        }

        //查询用户的资源角色类型
        List<String> roleTypes = portalRoleService.queryRoleTypesByRoleIds(roleIds);
        if (CollUtil.isEmpty(roleTypes)) {
            return false;
        }

        //查询用户的资源功能权限
        List<String> funcCodes = portalRoleTypeFuncService.queryRoleFuncCodes(roleTypes);
        if (CollUtil.isEmpty(funcCodes)) {
            return false;
        }

        long funcCodeAuthNum = funcCodes.stream().filter(f -> f.equalsIgnoreCase(funcCode)).count();
        if (funcCodeAuthNum > 0) {
            return true;
        }

        return hasAuth;
    }

    /**
     * 通过功能编码获取用户资源
     * @param funcCode 功能编码
     * @return
     */
    public List<String> getUserResourcesByFuncCode(String funcCode) {
        List<PortalRoleAuth> portalRoleAuthList = portalRoleAuthService.queryUserAllRoleAuth();
        return new ArrayList<>(resolvePortalIdsByFuncCode(portalRoleAuthList, funcCode));
    }

    /**
     * 批量填充门户列表的 hasAuth、isAdmin（非超管场景），循环内不再访问数据库。
     * VIEW 场景下，用户有门户下任意目录/看板节点的权限，即视为有该门户的权限。
     */
    public void applyPortalListAccess(List<PortalRsp> portalList, String funcCode) {
        if (CollUtil.isEmpty(portalList)) {
            return;
        }
        List<PortalRoleAuth> portalRoleAuthList = portalRoleAuthService.queryUserAllRoleAuth();
        Set<String> authPortalIds = resolvePortalIdsByFuncCode(portalRoleAuthList, funcCode);
        if (FuncType.VIEW.getCode().equalsIgnoreCase(funcCode)) {
            authPortalIds.addAll(resolvePortalIdsFromNodeAuth(portalRoleAuthList));
        }
        Set<String> adminPortalIds = resolveAdminPortalIds(portalRoleAuthList);
        for (PortalRsp portalRsp : portalList) {
            portalRsp.setHasAuth(authPortalIds.contains(portalRsp.getPortalId())
                    ? Enabled.YES.getId() : Enabled.NO.getId());
            portalRsp.setIsAdmin(adminPortalIds.contains(portalRsp.getPortalId())
                    ? Enabled.YES.getId() : Enabled.NO.getId());
        }
    }

    /**
     * 把用户有权限的目录/看板节点（portal_menu / analysis_template）反查为所属门户 id 集合。
     */
    private Set<String> resolvePortalIdsFromNodeAuth(List<PortalRoleAuth> portalRoleAuthList) {
        List<String> nodeResIds = portalRoleAuthList.stream()
                .map(PortalRoleAuth::getResId)
                .distinct()
                .collect(Collectors.toList());
        if (nodeResIds.isEmpty()) {
            return Collections.emptySet();
        }
        List<String> portalIds = (List<String>) dao.queryObjectList(
                "ssm.portal.menu.queryPortalIdsByResIds", nodeResIds);
        return portalIds != null ? new HashSet<>(portalIds) : Collections.emptySet();
    }

    private Set<String> resolvePortalIdsByFuncCode(List<PortalRoleAuth> portalRoleAuthList, String funcCode) {
        Set<String> resourceIdSet = new HashSet<>();
        if (CollUtil.isEmpty(portalRoleAuthList)) {
            return resourceIdSet;
        }

        List<String> allRoleIds = new ArrayList<>();
        Map<String, List<String>> resourceRoleMap = new HashMap<>();
        for (PortalRoleAuth portalRoleAuth : portalRoleAuthList) {
            String resId = portalRoleAuth.getResId();
            String roleId = portalRoleAuth.getRoleId();
            resourceRoleMap.computeIfAbsent(resId, k -> new ArrayList<>()).add(roleId);
            allRoleIds.add(roleId);
        }

        Map<String, List<String>> roleFuncMap = new HashMap<>();
        List<PortalRoleFunc> portalRoleFuncList = portalRoleTypeFuncService.queryFuncCodeByRoleIds(allRoleIds);
        for (PortalRoleFunc portalRoleFunc : portalRoleFuncList) {
            roleFuncMap.computeIfAbsent(portalRoleFunc.getRoleId(), k -> new ArrayList<>())
                    .add(portalRoleFunc.getFuncCode());
        }

        for (Map.Entry<String, List<String>> entry : resourceRoleMap.entrySet()) {
            for (String roleId : entry.getValue()) {
                List<String> funcCodeList = roleFuncMap.get(roleId);
                if (CollUtil.isEmpty(funcCodeList)) {
                    continue;
                }
                if (funcCodeList.stream().anyMatch(f -> f.equalsIgnoreCase(funcCode))) {
                    resourceIdSet.add(entry.getKey());
                    break;
                }
            }
        }
        return resourceIdSet;
    }

    private Set<String> resolveAdminPortalIds(List<PortalRoleAuth> portalRoleAuthList) {
        Set<String> adminPortalIds = new HashSet<>();
        if (CollUtil.isEmpty(portalRoleAuthList)) {
            return adminPortalIds;
        }
        List<String> allRoleIds = portalRoleAuthList.stream()
                .map(PortalRoleAuth::getRoleId)
                .distinct()
                .collect(Collectors.toList());
        List<PortalRole> roles = portalRoleService.queryRolesByRoleIds(allRoleIds);
        Set<String> adminRoleIds = roles.stream()
                .filter(r -> RoleType.PORTAL_ADMIN.getCode().equalsIgnoreCase(r.getRoleType()))
                .map(PortalRole::getRoleId)
                .collect(Collectors.toSet());
        for (PortalRoleAuth portalRoleAuth : portalRoleAuthList) {
            if (!ResType.PORTAL.getCode().equalsIgnoreCase(portalRoleAuth.getResType())) {
                continue;
            }
            if (adminRoleIds.contains(portalRoleAuth.getRoleId())) {
                adminPortalIds.add(portalRoleAuth.getResId());
            }
        }
        return adminPortalIds;
    }

    /**
     * 查询用户所有的功能权限
     * @return
     */
    public List<String> getUserAllFuncCode() {

        List<String> funcCodeList = new ArrayList<>();

        List<PortalRoleAuth> portalRoleAuthList = portalRoleAuthService.queryUserAllRoleAuth();
        if (CollUtil.isEmpty(portalRoleAuthList)) {
            return funcCodeList;
        }

        List<String> roleIds = new ArrayList<>();
        portalRoleAuthList.stream().forEach(p->roleIds.add(p.getRoleId()));
        List<String> roleTypes = portalRoleService.queryRoleTypesByRoleIds(roleIds);
        funcCodeList = portalRoleTypeFuncService.queryRoleFuncCodes(roleTypes);

        return funcCodeList;
    }

    /**
     * 当前用户是否为指定门户管理员（含超管）。
     */
    public boolean isPortalAdmin(String portalId) {
        if (portalRoleService.isPortalSuperAdmin()) {
            return true;
        }
        List<String> roleIds = portalRoleAuthService.queryUserResourceRoleIds(portalId, ResType.PORTAL.getCode());
        if (CollUtil.isEmpty(roleIds)) {
            return false;
        }
        List<String> roleTypes = portalRoleService.queryRoleTypesByRoleIds(roleIds);
        return CollUtil.isNotEmpty(roleTypes)
                && roleTypes.stream().anyMatch(t -> RoleType.PORTAL_ADMIN.getCode().equalsIgnoreCase(t));
    }

    /**
     * 从已加载的 allAuth 判断用户是否对指定门户有 VIEW 功能权限（含 admin/worker/viewer 三种角色）。
     * 复用同一批 allAuth，避免重复触发慢查询。
     */
    public boolean hasPortalViewAccessFromAuth(List<PortalRoleAuth> allAuth, String portalId) {
        return resolvePortalIdsByFuncCode(allAuth, FuncType.VIEW.getCode()).contains(portalId);
    }

    /**
     * 从已加载的 allAuth 判断用户是否为指定门户的管理员，复用同一批 allAuth 避免重复查询。
     */
    public boolean isPortalAdminFromAuth(List<PortalRoleAuth> allAuth, String portalId) {
        return resolveAdminPortalIds(allAuth).contains(portalId);
    }

    /**
     * 查询门户下显式关闭继承（inherit_enabled=0）的资源 id 集合。
     * 未配置或 inherit_enabled=1 均视为继承（默认继承）。
     */
    public Set<String> getInheritDisabledResIds(String portalId) {
        List<String> resIds = (List<String>) dao.queryObjectList(
                "ssm.portal.resource.auth.inherit.queryDisabledResIdsByPortalId", portalId);
        return CollUtil.isEmpty(resIds) ? Collections.emptySet() : new HashSet<>(resIds);
    }

    /**
     * 从已加载的 allAuth 提取指定资源类型下有权限的 resId 集合，纯内存操作，无 DB 查询。
     */
    public Set<String> getPermittedResIdsFromAuth(List<PortalRoleAuth> allAuth, Set<String> resTypes) {
        Set<String> permitted = new HashSet<>();
        for (PortalRoleAuth auth : allAuth) {
            //if (resTypes.contains(auth.getResType())) {
            permitted.add(auth.getResId());
            //}
        }
        return permitted;
    }

    // ------------------------------------------------------------------ 文件夹 / 看板 权限管理 ------------------------------------------------------------------

    /**
     * 内存遍历继承链，返回祖先 resId → resType（由近及远，不含本节点）。
     * 借助 portal_id 一次性加载门户全量菜单与继承配置，遍历过程零 DB 查询，
     * 查询次数与链条深度无关（共 3 次常数索引查询）。
     */
    private LinkedHashMap<String, String> collectAncestorChain(String resId, String resType) {
        LinkedHashMap<String, String> ancestors = new LinkedHashMap<>();

        // 定位起始节点所在门户（文件夹按 menu_id，看板按 content_ref_id 兜底）
        PortalMenu portalMenu = (PortalMenu) dao.queryObject("ssm.portal.menu.get", resId);
        String portalId = portalMenu != null ? portalMenu.getPortalId() : null;
        if (portalId == null) {
            List<PortalMenu> rows = (List<PortalMenu>) dao.queryObjectList(
                    "ssm.portal.menu.getPortalMenuInfoByAnalysisTplIds",
                    new ArrayList<>(Collections.singletonList(resId)));
            portalId = CollUtil.isNotEmpty(rows) ? rows.get(0).getPortalId() : null;
        }
        if (portalId == null) {
            return ancestors;
        }

        // 一次加载门户全量菜单（构建父子关系）与继承配置
        Map<String, Object> menuQuery = new HashMap<>();
        menuQuery.put("portalId", portalId);
        List<PortalMenu> menus = (List<PortalMenu>) dao.queryObjectList("ssm.portal.menu.getByPortalId", menuQuery);
        Map<String, PortalMenu> menuById = new HashMap<>();
        Map<String, PortalMenu> menuByContentRef = new HashMap<>();
        if (menus != null) {
            for (PortalMenu m : menus) {
                menuById.put(m.getMenuId(), m);
                if (m.getContentRefId() != null) {
                    menuByContentRef.put(m.getContentRefId(), m);
                }
            }
        }

        List<PortalResourceAuthInherit> configs = (List<PortalResourceAuthInherit>) dao.queryObjectList(
                "ssm.portal.resource.auth.inherit.queryByPortalId", portalId);
        Map<String, PortalResourceAuthInherit> inheritByKey = new HashMap<>();
        if (configs != null) {
            for (PortalResourceAuthInherit c : configs) {
                inheritByKey.put(c.getResId() + "|" + c.getResType(), c);
            }
        }

        // 内存遍历：父级关系完全由菜单树结构决定
        Set<String> visited = new HashSet<>();
        visited.add(resId);
        String curResId = resId;
        String curResType = resType;

        while (true) {
            PortalResourceAuthInherit inherit = inheritByKey.get(curResId + "|" + curResType);

            // 显式关闭继承（单独授权）：停止上溯
            if (inherit != null && Integer.valueOf(0).equals(inherit.getInheritEnabled())) {
                break;
            }

            // 从菜单树结构找直接父级（看板节点按 content_ref_id 定位所在菜单）
            PortalMenu menu = menuById.get(curResId);
            if (menu == null) {
                menu = menuByContentRef.get(curResId);
            }
            if (menu == null || menu.getParentMenuId() == null) {
                break;
            }
            String parentResId = menu.getParentMenuId();
            // 父级类型按 id 判断：等于门户 id 即到顶层，否则是目录
            String parentResType = portalId.equals(parentResId)
                    ? ResType.PORTAL.getCode()
                    : ResType.PORTAL_MENU.getCode();

            if (visited.contains(parentResId)) {
                break; // 防环
            }
            visited.add(parentResId);
            ancestors.put(parentResId, parentResType);

            if (ResType.PORTAL.getCode().equalsIgnoreCase(parentResType)) {
                break; // 已到门户顶层
            }
            curResId = parentResId;
            curResType = parentResType;
        }

        return ancestors;
    }

    /**
     * 查询有权限的用户清单（本层 + 继承链所有祖先层）。
     * 继承链内存遍历 + 批量 SQL，查询次数与链条深度无关。
     * authSource 取值见 {@link AuthSourceType}，同一用户多来源时逗号组合，每个用户一行。
     */
    public List<PortalAuthUserItem> getUserAuthList(PortalAuthUserListReq req) {
        // 本层 viewer 角色（单独查询，用于区分本层 / 继承层来源）
        String localRoleId = portalRoleService.queryViewerRoleId(req.getResId());

        // 祖先层 viewer 角色（批量）
        List<String> ancestorResIds = new ArrayList<>(collectAncestorChain(req.getResId(), req.getResType()).keySet());
        List<String> ancestorRoleIds = ancestorResIds.isEmpty() ? Collections.emptyList()
                : (List<String>) dao.queryObjectList("ssm.portal.role.queryViewerRoleIdsByResIds", ancestorResIds);

        List<String> allRoleIds = new ArrayList<>();
        if (localRoleId != null) {
            allRoleIds.add(localRoleId);
        }
        if (CollUtil.isNotEmpty(ancestorRoleIds)) {
            allRoleIds.addAll(ancestorRoleIds);
        }
        if (allRoleIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Object> params = new HashMap<>();
        params.put("roleIds", allRoleIds);
        params.put("keyword", req.getKeyword());
        params.put("deptId", req.getDeptId());

        // 个人授权用户（含继承层）+ 组织授权转人（含继承层、子部门）
        List<PortalAuthUserItem> individualUsers = dao.queryObjectList(
                "ssm.portal.role.user.queryUserListBatch", params, PortalAuthUserItem.class);
        List<PortalAuthUserItem> deptUsers = dao.queryObjectList(
                "ssm.portal.role.dept.queryDeptUserListBatch", params, PortalAuthUserItem.class);

        // 本层行排前面，保证用户信息（部门/时间）优先取本层，标顺序为 个人 → 组织 → 父层继承
        Comparator<PortalAuthUserItem> localFirst =
                Comparator.comparing(i -> Objects.equals(i.getRoleId(), localRoleId) ? 0 : 1);
        if (individualUsers != null) {
            individualUsers.sort(localFirst);
        }
        if (deptUsers != null) {
            deptUsers.sort(localFirst);
        }

        // 每个用户一行，同一用户多来源时在 authSource 上逗号追加
        Map<String, PortalAuthUserItem> userItemMap = new LinkedHashMap<>();
        if (individualUsers != null) {
            for (PortalAuthUserItem item : individualUsers) {
                mergeAuthSource(userItemMap, item, Objects.equals(item.getRoleId(), localRoleId)
                        ? AuthSourceType.INDIVIDUAL.getCode() : AuthSourceType.PARENT_DIR.getCode());
            }
        }
        if (deptUsers != null) {
            for (PortalAuthUserItem item : deptUsers) {
                mergeAuthSource(userItemMap, item, Objects.equals(item.getRoleId(), localRoleId)
                        ? AuthSourceType.ORG.getCode() : AuthSourceType.PARENT_DIR.getCode());
            }
        }

        List<PortalAuthUserItem> result = new ArrayList<>(userItemMap.values());

        // authSource 过滤：匹配包含该标的行（组合值任一标命中即保留）
        if (req.getAuthSource() != null && !req.getAuthSource().isEmpty()) {
            result.removeIf(item -> !Arrays.asList(item.getAuthSource().split(","))
                    .contains(req.getAuthSource()));
        }

        return result;
    }

    /**
     * 合并授权来源标：同一用户已存在则在 authSource 上追加标（去重），否则新增一行。
     */
    private static void mergeAuthSource(Map<String, PortalAuthUserItem> userItemMap,
                                        PortalAuthUserItem item, String tag) {
        PortalAuthUserItem exist = userItemMap.get(item.getUserName());
        if (exist == null) {
            item.setAuthSource(tag);
            item.setRoleId(null);
            userItemMap.put(item.getUserName(), item);
        } else if (!Arrays.asList(exist.getAuthSource().split(",")).contains(tag)) {
            exist.setAuthSource(exist.getAuthSource() + "," + tag);
        }
    }

    /**
     * 按目录继承关系，汇总此节点所有祖先层的 viewer 授权（组织 + 个人），供前端预填"复制权限"。
     * 继承链内存遍历 + 批量 SQL，查询次数与链条深度无关。
     */
    public PortalResourceAuthGetRsp getParentAuth(PortalAuthBaseReq req) {
        // Phase 1: 内存遍历继承链（由近及远），展示路径需由远及近，反转
        LinkedHashMap<String, String> ancestors = collectAncestorChain(req.getResId(), req.getResType());
        if (ancestors.isEmpty()) {
            return new PortalResourceAuthGetRsp();
        }
        List<String> ancestorResIds = new ArrayList<>(ancestors.keySet());
        Collections.reverse(ancestorResIds);

        // Phase 2: 批量查询权限数据
        List<String> roleIds = (List<String>) dao.queryObjectList(
                "ssm.portal.role.queryViewerRoleIdsByResIds", ancestorResIds);

        List<String> userNames = CollUtil.isEmpty(roleIds) ? new ArrayList<>()
                : (List<String>) dao.queryObjectList("ssm.portal.role.user.queryUsersByRoleIds", roleIds);
        List<String> deptIds = CollUtil.isEmpty(roleIds) ? new ArrayList<>()
                : (List<String>) dao.queryObjectList("ssm.portal.role.dept.queryDeptsByRoleIds", roleIds);

        // Phase 3: 批量查询祖先目录名称，组装 parentResName
        List<String> menuAncestorIds = new ArrayList<>();
        List<String> portalAncestorIds = new ArrayList<>();
        for (String ancestorResId : ancestorResIds) {
            if (ResType.PORTAL.getCode().equalsIgnoreCase(ancestors.get(ancestorResId))) {
                portalAncestorIds.add(ancestorResId);
            } else {
                menuAncestorIds.add(ancestorResId);
            }
        }

        Map<String, String> idToName = new HashMap<>();
        if (!menuAncestorIds.isEmpty()) {
            List<PortalMenu> menus = (List<PortalMenu>) dao.queryObjectList(
                    "ssm.portal.menu.queryNamesByIds", menuAncestorIds);
            if (menus != null) {
                menus.forEach(m -> idToName.put(m.getMenuId(), m.getMenuName()));
            }
        }
        for (String portalId : portalAncestorIds) {
            com.bi.queryer.ssm.portal.entity.Portal portal =
                    (com.bi.queryer.ssm.portal.entity.Portal) dao.queryObject("ssm.portal.get", portalId);
            if (portal != null) {
                idToName.put(portalId, portal.getPortalName());
            }
        }

        // 每个祖先给出从门户开始的全路径（斜线连接），多个祖先用、分隔
        // 祖先链已反转为根在前（门户 → 一级目录 → ...），逐级累积即为全路径
        List<String> ancestorPaths = new ArrayList<>();
        StringBuilder pathBuilder = new StringBuilder();
        for (String ancestorResId : ancestorResIds) {
            if (pathBuilder.length() > 0) {
                pathBuilder.append("/");
            }
            pathBuilder.append(idToName.getOrDefault(ancestorResId, ancestorResId));
            ancestorPaths.add(pathBuilder.toString());
        }
        String parentResName = String.join("、", ancestorPaths);

        // 有效用户数 = 授权个人 ∪ 组织授权展开的人（含子部门），按用户名去重
        Set<String> effectiveUsers = new HashSet<>(userNames != null ? userNames : Collections.emptyList());
        if (CollUtil.isNotEmpty(roleIds)) {
            Map<String, Object> deptUserParams = new HashMap<>();
            deptUserParams.put("roleIds", roleIds);
            List<PortalAuthUserItem> deptUsers = dao.queryObjectList(
                    "ssm.portal.role.dept.queryDeptUserListBatch", deptUserParams, PortalAuthUserItem.class);
            if (CollUtil.isNotEmpty(deptUsers)) {
                deptUsers.forEach(u -> effectiveUsers.add(u.getUserName()));
            }
        }

        PortalResourceAuthGetRsp rsp = new PortalResourceAuthGetRsp();
        rsp.setUserNameList(userNames != null ? userNames : new ArrayList<>());
        rsp.setDeptIdList(deptIds != null ? deptIds : new ArrayList<>());
        rsp.setEffectiveUserCount(effectiveUsers.size());
        rsp.setParentResName(parentResName);
        return rsp;
    }

    /**
     * 导出有权限的用户清单为 Excel。
     */
    public void exportUserAuthList(PortalAuthUserListReq req, HttpServletResponse response) {
        List<PortalAuthUserItem> list = getUserAuthList(req);

        List<String> headers = Arrays.asList("用户名", "部门", "授权来源", "权限开始时间", "权限结束时间");

        List<List<String>> rows = new ArrayList<>();
        for (PortalAuthUserItem item : list) {
            rows.add(Arrays.asList(
                    s(item.getUserName()),
                    s(item.getDeptName()),
                    authSourceText(item.getAuthSource()),
                    s(item.getAuthStartTime()),
                    s(item.getAuthEndTime())
            ));
        }

        ExcelHelper.exportExcel("有权限的用户清单", headers, rows, response);
    }

    private static String s(String v) {
        return v != null ? v : "";
    }

    /** 授权来源转中文名，逗号组合值逐个转换 */
    private static String authSourceText(String authSource) {
        if (authSource == null || authSource.isEmpty()) {
            return "";
        }
        return Arrays.stream(authSource.split(","))
                .map(code -> {
                    AuthSourceType type = AuthSourceType.get(code);
                    return type != null ? type.getName() : code;
                })
                .collect(Collectors.joining(","));
    }

}
