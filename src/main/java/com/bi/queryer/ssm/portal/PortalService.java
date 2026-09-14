package com.bi.queryer.ssm.portal;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.enums.UserProfileTypeEnum;
import com.bi.queryer.ssm.portal.ai.AiSkillPortalCtgClient;
import com.bi.queryer.ssm.portal.ai.vo.AiSkillPortalCtgVO;
import com.bi.queryer.ssm.portal.auth.entity.PortalRoleAuth;
import com.bi.queryer.ssm.portal.auth.service.PortalAuthService;
import com.bi.queryer.ssm.portal.auth.service.PortalRoleAuthService;
import com.bi.queryer.ssm.portal.auth.service.PortalRoleService;
import com.bi.queryer.ssm.portal.auth.vo.req.PortalAuthAddReq;
import com.bi.queryer.ssm.portal.auth.vo.req.PortalAuthQueryReq;
import com.bi.queryer.ssm.portal.auth.vo.rsp.PortalResourceAuthGetRsp;
import com.bi.queryer.ssm.portal.entity.Portal;
import com.bi.queryer.ssm.portal.entity.PortalMenu;
import com.bi.queryer.ssm.portal.enums.*;
import com.bi.queryer.ssm.portal.template.AnalysisTemplateService;
import com.bi.queryer.ssm.portal.template.entity.UserProfileEntity;
import com.bi.queryer.ssm.portal.template.enums.AnalysisTemplateOnlineStatus;
import com.bi.queryer.ssm.portal.template.vo.AnalysisTemplateBaseVO;
import com.bi.queryer.ssm.portal.vo.req.*;
import com.bi.queryer.ssm.portal.vo.rsp.*;
import com.bi.queryer.ssm.query.ctg.QueryTemplateCategoryService;
import com.bi.queryer.ssm.query.ctg.enums.QueryTemplateCategoryType;
import com.bi.queryer.ssm.query.ctg.model.QueryTemplateCategory;
import com.bi.queryer.sys.base.AbstractTransaction;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.common.SSMResponseMessage;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.Guid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;

/**
 * @Author: contributor
 * @CreateTime: 2024-06-17  14:47
 * @Description: 门户相关的服务实现类
 */
@Service
public class PortalService {

    /**
     * 新建门户时自动创建的专题分析报告挂载目录（空目录，上传报告时选此父节点）
     */
    private static final String DEFAULT_DYNAMIC_ANALYSIS_REPORT_FOLDER_NAME = "专题分析报告";

    @Autowired
    private BaseDao dao;

    @Autowired
    private PortalRoleService portalRoleService;

    @Autowired
    private PortalRoleAuthService portalRoleAuthService;

    @Autowired
    private PortalAuthService portalAuthService;

    @Autowired
    private PortalMenuService portalMenuService;

    @Autowired
    private AnalysisTemplateService analysisTemplateService;
    @Autowired
    private QueryTemplateCategoryService queryTemplateCategoryService;

    @Autowired
    private AiSkillPortalCtgClient aiSkillPortalCtgClient;


    /**
     * 门户新增
     *
     * @return
     */
    public SSMResponseMessage<String> add(PortalAddReq portalAddReq) {

        String portalId = Guid.id();

        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {

                //新增门户基础表
                addPortal(portalAddReq, portalId);

                //新增门户角色表
                List<String> roleIds = portalRoleService.batchAddPortalRole(portalId, portalAddReq.getPortalName());

                //初始化门户角色权限表
                portalRoleAuthService.batchAddPortalRoleAuth(roleIds, portalId, ResType.PORTAL.getCode());

                auth(portalId, portalAddReq);

                // 挂载查询模板公共空间
                portalMenuService.batchUpdateMenu(portalId, PortalMenuType.SPACE_CTG, portalAddReq.getQueryTplSpaceCtgList());

                // 挂载看板配置使用的公共空间
                portalMenuService.batchUpdateMenu(portalId, PortalMenuType.CONFIG_USED_SPACE_CTG, portalAddReq.getConfigUsedSpaceCtgList());

                // 默认「专题分析报告」目录（{@link PortalMenuType#DYNAMIC_ANALYSIS_REPORT_CTG}，其下挂 {@link PortalMenuType#DYNAMIC_ANALYSIS_REPORT}）
                addDefaultDynamicAnalysisReportFolder(portalId);

                // 默认「技能集」目录（{@link PortalMenuType#SKILL_CTG}）
                addDefaultSkillCtg(portalId, portalAddReq.getPortalName());
            }
        });

        return SSMResponseMessage.success("新增成功！", portalId);

    }

    private void addDefaultDynamicAnalysisReportFolder(String portalId) {
        PortalMenu folder = PortalMenu.builder()
                .portalId(portalId)
                .menuName(DEFAULT_DYNAMIC_ANALYSIS_REPORT_FOLDER_NAME)
                .menuType(PortalMenuType.DYNAMIC_ANALYSIS_REPORT_CTG.getCode())
                .parentMenuId(null)
                .menuDesc(null)
                .contentRefId(null)
                .build();
        portalMenuService.addMenu(folder);
    }

    private void addDefaultSkillCtg(String portalId, String portalName) {
        PortalMenu folder = PortalMenu.builder()
                .portalId(portalId)
                .menuName(portalName + "技能集")
                .menuType(PortalMenuType.AI_SKILL_CTG.getCode())
                .parentMenuId(null)
                .menuDesc(null)
                .contentRefId(null)
                .build();
        portalMenuService.addMenu(folder);
    }

    private void auth(String portalId, PortalAddReq req) {
        // 增加协作者授权授权
        PortalAuthAddReq portalWorkerAuth = new PortalAuthAddReq();
        portalWorkerAuth.setResId(portalId);
        portalWorkerAuth.setResType(ResType.PORTAL.getCode());
        portalWorkerAuth.setRoleType(RoleType.PORTAL_WORKER.getCode());
        portalWorkerAuth.setUserNameList(req.getWorkerUserNameList());
        portalAuthService.save(portalWorkerAuth);

        // 增加管理员授权授权
        PortalAuthAddReq portalAdminAuth = new PortalAuthAddReq();
        portalAdminAuth.setResId(portalId);
        portalAdminAuth.setResType(ResType.PORTAL.getCode());
        portalAdminAuth.setRoleType(RoleType.PORTAL_ADMIN.getCode());
        portalAdminAuth.setUserNameList(req.getAdminUserNameList());
        portalAuthService.save(portalAdminAuth);
    }


    /**
     * 门户更新
     *
     * @return
     */
    public SSMResponseMessage<String> update(PortalUpdateReq updateReq) {

        final String portalId = updateReq.getPortalId();
        final String userName = UserManager.get().getName();

        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {
                //更新门户基础表
                updatePortal(updateReq, userName);
                //授权
                auth(portalId, updateReq);
                // 挂载查询模板公共空间
                portalMenuService.batchUpdateMenu(portalId, PortalMenuType.SPACE_CTG, updateReq.getQueryTplSpaceCtgList());
                // 挂载配置使用的查询模板公共空间
                portalMenuService.batchUpdateMenu(portalId, PortalMenuType.CONFIG_USED_SPACE_CTG, updateReq.getConfigUsedSpaceCtgList());
            }
        });

        return SSMResponseMessage.success("更新成功！", portalId);

    }

    /**
     * 新增门户基础表
     *
     * @param portalAddReq
     * @param portalId
     */
    public void addPortal(PortalAddReq portalAddReq, String portalId) {
        Portal portal = new Portal();
        portal.setPortalId(portalId);

        portal.setPortalName(portalAddReq.getPortalName());
        portal.setPortalDesc(portalAddReq.getPortalDesc());

        Double sortId = portalAddReq.getSortId();
        if (sortId == null) {
            sortId = (Double) dao.queryObject("ssm.portal.queryMaxSortId", null);
            sortId++;

        }
        portal.setSortId(sortId);

        dao.insert("ssm.portal.add", portal);
    }

    // 移动顺序
    public boolean move(String menuId, String targetMenuId, String dragType) {
        final Portal portal = get(menuId);
        if (portal == null) {
            // 不是门户目录，返回false
            return false;
        }

        checkState(portalRoleService.isPortalSuperAdmin(), "你不是超管，无权限操作！");

        final Portal targetPortal = get(targetMenuId);
        checkState(targetPortal != null, "目标菜单不存在！");

        DragType dgType = DragType.get(dragType);
        if (DragType.AFTER == dgType) {
            portal.setSortId(targetPortal.getSortId() + 1);
        } else {
            portal.setSortId(targetPortal.getSortId());
        }

        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {
                Map<String, Object> paramMap = new HashMap<>();
                paramMap.put("sortId", targetPortal.getSortId());
                paramMap.put("dragType", dgType.getCode());

                //将同一层级，大于移动节点的排序+1
                dao.update("ssm.portal.incrementMenuSortId", paramMap);

                dao.update("ssm.portal.updateMenuSortId", portal);
            }
        });

        return true;
    }

    //更新
    public void updatePortal(PortalUpdateReq updateReq, String userName) {
        Portal portal = new Portal();
        portal.setPortalId(updateReq.getPortalId());

        portal.setPortalName(updateReq.getPortalName());
        portal.setPortalDesc(updateReq.getPortalDesc());
        portal.setUpdatedBy(userName);

        dao.insert("ssm.portal.update", portal);
    }

    /**
     * 查询门户列表
     *
     * @return
     */
    public List<PortalRsp> list(String funcCode, boolean isFilterEmptyPortal) {

        List<PortalRsp> portalList = new ArrayList<>();
        portalList = (List<PortalRsp>) dao.queryObjectList("ssm.portal.list", null);

        if (CollUtil.isNotEmpty(portalList)) {

            if (isFilterEmptyPortal) {
                List<String> hasOnlineAnalysisTemplatePortalIds = (List<String>) dao.queryObjectList("ssm.portal.getPortalIdWithOnlineAnalysisTemplate", null);
                if (CollUtil.isEmpty(hasOnlineAnalysisTemplatePortalIds)) {
                    return new ArrayList<>();
                }

                //去掉没有在线分析模版的门户
                portalList = portalList
                        .stream()
                        .filter(portalRsp -> hasOnlineAnalysisTemplatePortalIds.contains(portalRsp.getPortalId()))
                        .collect(Collectors.toList());
            }

            boolean superAdmin = portalRoleService.isPortalSuperAdmin();
            if (superAdmin) {
                for (PortalRsp portalRsp : portalList) {
                    portalRsp.setIsAdmin(Enabled.YES.getId());
                }
            } else {
                portalAuthService.applyPortalListAccess(portalList, funcCode);
            }

            UserProfileEntity entity = getDefaultPortal();
            if (entity != null) {
                for (PortalRsp portalRsp : portalList) {
                    if (Objects.equals(entity.getEntityId(), portalRsp.getPortalId())) {
                        portalRsp.setIsDefaultPortal(Enabled.YES.getId());
                    }
                }
            }

        }

        return portalList;
    }

    /**
     * 查询所有的门户
     *
     * @return
     */
    public List<PortalRsp> listAll() {
        List<PortalRsp> portalList = new ArrayList<>();
        portalList = (List<PortalRsp>) dao.queryObjectList("ssm.portal.list", null);
        return portalList;
    }

    /**
     * 获取门户信息
     *
     * @param portalId
     * @return
     */
    public Portal get(String portalId) {
        Portal portal = (Portal) dao.queryObject("ssm.portal.get", portalId);
        return portal;
    }

    /**
     * 通过内容引用id获取门户信息
     *
     * @param contentRefId
     * @return
     */
    public Portal getByContentRefId(String contentRefId) {
        Portal portal = (Portal) dao.queryObject("ssm.portal.getByContentRefId", contentRefId);
        return portal;
    }

    /**
     * 获取门户详情
     *
     * @param portalId
     * @return
     */
    public SSMResponseMessage<PortalDetailRsp> getDetail(String portalId) {
        Portal portal = (Portal) dao.queryObject("ssm.portal.get", portalId);
        checkState(portal != null, "门户不存在");
        PortalDetailRsp portalDetailRsp = new PortalDetailRsp();
        portalDetailRsp.setPortalId(portal.getPortalId());
        portalDetailRsp.setPortalName(portal.getPortalName());
        portalDetailRsp.setPortalDesc(portal.getPortalDesc());
        portalDetailRsp.setSortId(portal.getSortId());

        //管理员
        PortalAuthQueryReq adminAuthQueryReq = new PortalAuthQueryReq();
        adminAuthQueryReq.setResId(portalId);
        adminAuthQueryReq.setRoleType(RoleType.PORTAL_ADMIN.getCode());
        adminAuthQueryReq.setResType(ResType.PORTAL.getCode());
        PortalResourceAuthGetRsp portalAdmins = portalAuthService.get(adminAuthQueryReq);
        portalDetailRsp.setAdminUserNameList(portalAdmins.getUserNameList());

        //协作者
        PortalAuthQueryReq workAuthQueryReq = new PortalAuthQueryReq();
        workAuthQueryReq.setResId(portalId);
        workAuthQueryReq.setRoleType(RoleType.PORTAL_WORKER.getCode());
        workAuthQueryReq.setResType(ResType.PORTAL.getCode());
        PortalResourceAuthGetRsp portalWorks = portalAuthService.get(workAuthQueryReq);
        portalDetailRsp.setWorkerUserNameList(portalWorks.getUserNameList());

        //共享空间
        portalDetailRsp.setQueryTplSpaceCtgList(getSpaceCtgList(portalId, PortalMenuType.SPACE_CTG));
        portalDetailRsp.setConfigUsedSpaceCtgList(getSpaceCtgList(portalId, PortalMenuType.CONFIG_USED_SPACE_CTG));
        return SSMResponseMessage.success("查询成功！", portalDetailRsp);
    }

    /**
     * 获取门户管理员列表
     *
     * @param portalId
     * @return
     */
    public List<String> getPortalAdmins(String portalId) {
        PortalAuthQueryReq adminAuthQueryReq = new PortalAuthQueryReq();
        adminAuthQueryReq.setResId(portalId);
        adminAuthQueryReq.setRoleType(RoleType.PORTAL_ADMIN.getCode());
        adminAuthQueryReq.setResType(ResType.PORTAL.getCode());
        PortalResourceAuthGetRsp portalAdmins = portalAuthService.get(adminAuthQueryReq);

        return portalAdmins.getUserNameList();
    }

    private List<PortalMenuContentReq> getSpaceCtgList(String portalId, PortalMenuType menuType) {
        List<PortalMenu> portalMenuList = portalMenuService.getMenuListByPortalId(portalId, menuType);
        Map<String, QueryTemplateCategory> queryTemplateCtgMap = buildQueryTemplateCtgMap(portalMenuList);
        List<PortalMenuContentReq> spaceCtgList = portalMenuList.stream().map(v -> {
            PortalMenuContentReq req = new PortalMenuContentReq();
            req.setContentId(v.getContentRefId());
            QueryTemplateCategory portalMenuRsp = queryTemplateCtgMap.get(v.getContentRefId());
            req.setContentName(portalMenuRsp == null ? v.getMenuName() : portalMenuRsp.getName());
            return req;
        }).collect(Collectors.toList());
        return spaceCtgList;
    }

    /**
     * 查询门户下的菜单 (校验查看权限)
     *
     * @param portalId
     * @return
     */
    public SSMResponseMessage<List<PortalMenuRsp>> getMenuTree(String portalId) {

        List<PortalMenu> portalMenuList = portalMenuService.getMenuList(portalId);

        if (CollUtil.isEmpty(portalMenuList)) {
            return SSMResponseMessage.success("查询成功！", Collections.emptyList());
        }

        Map<String, List<PortalMenu>> portalMenuMap = new HashMap<>();
        Map<String, AnalysisTemplateBaseVO> analysisTemplateMap = new HashMap<>();
        buildMenuTreeMetaData(portalMenuList, portalMenuMap, analysisTemplateMap);
        Map<String, QueryTemplateCategory> queryTemplateCtgMap = buildQueryTemplateCtgMap(portalMenuList);
        Map<String, List<AiSkillPortalCtgVO>> aiSkillCtgMap = buildAiSkillCtgMap(portalMenuList);

        // 超管直接返回，跳过后续所有 DB 查询
        if (portalRoleService.isPortalSuperAdmin()) {
            List<PortalMenuRsp> menuTreeData = buildSubNode(portalId, portalMenuMap, analysisTemplateMap,
                    Enabled.YES.getId(), Enabled.YES.getId(), queryTemplateCtgMap, aiSkillCtgMap, true);
            return SSMResponseMessage.success("查询成功！", menuTreeData);
        }

        // 一次查询获取用户所有权限，后续判断全部内存操作，避免重复触发慢 LIKE 查询
        List<PortalRoleAuth> allAuth = portalRoleAuthService.queryUserAllRoleAuth();

        // 门户管理员：返回完整树（需管理所有节点）
        if (portalAuthService.isPortalAdminFromAuth(allAuth, portalId)) {
            List<PortalMenuRsp> menuTreeData = buildSubNode(portalId, portalMenuMap, analysisTemplateMap,
                    Enabled.YES.getId(), Enabled.YES.getId(), queryTemplateCtgMap, aiSkillCtgMap, true);
            return SSMResponseMessage.success("查询成功！", menuTreeData);
        }

        // 门户查看者：门户级权限沿树向下继承；普通用户：仅有节点级授权。
        // 两者统一走剪枝：关闭继承（单独授权）的节点必须有直接权限才显示。
        boolean portalView = portalAuthService.hasPortalViewAccessFromAuth(allAuth, portalId);

        Set<String> permittedResIds = portalAuthService.getPermittedResIdsFromAuth(allAuth, Collections.emptySet());

        // 按门户一次查出关闭继承的节点集合
        Set<String> inheritDisabledResIds = portalAuthService.getInheritDisabledResIds(portalId);

        // 先构建全量树，再按「直接授权 + 父级权限继承」剪枝
        List<PortalMenuRsp> menuTreeData = buildSubNode(portalId, portalMenuMap, analysisTemplateMap,
                Enabled.YES.getId(), Enabled.YES.getId(), queryTemplateCtgMap, aiSkillCtgMap, true);
        menuTreeData.removeIf(node -> !pruneMenuTree(node, permittedResIds, inheritDisabledResIds, portalView));

        return SSMResponseMessage.success("查询成功！", menuTreeData);
    }

    /**
     * 递归剪枝。节点的有效权限 = 直接授权 || (未关闭继承 && 父级有有效权限)，默认继承。
     * ANALYSIS_TEMPLATE — 有有效权限保留（hasAuth=1），否则删除。
     * PORTAL_CTG / DYNAMIC_ANALYSIS_REPORT_CTG — 有有效权限或有可见子节点则保留，
     * 有效权限时 hasAuth=1，仅靠子节点可见时 hasAuth=0（作为路径容器展示）。
     * 其他类型 — 保留，hasAuth 不变。
     * 返回 true 表示该节点应保留。
     */
    private boolean pruneMenuTree(PortalMenuRsp node, Set<String> permittedResIds,
                                  Set<String> inheritDisabledResIds, boolean parentHasAuth) {
        boolean direct = permittedResIds.contains(node.getMenuId());
        boolean inheritsParent = !inheritDisabledResIds.contains(node.getMenuId());
        boolean effective = direct || (inheritsParent && parentHasAuth);

        // 先剪枝子节点，向下传递本节点的有效权限
        if (CollUtil.isNotEmpty(node.getChildren())) {
            node.getChildren().removeIf(child ->
                    !pruneMenuTree(child, permittedResIds, inheritDisabledResIds, effective));
        }
        boolean hasChild = CollUtil.isNotEmpty(node.getChildren());
        if (effective || hasChild) {
            node.setHasAuth(effective ? Enabled.YES.getId() : Enabled.NO.getId());
            return true;
        }
        return false;
    }

    /**
     * 构建菜单树的元信息
     */
    public void buildMenuTreeMetaData( List<PortalMenu> portalMenuList,Map<String,List<PortalMenu>> portalMenuMap,
                                       Map<String,AnalysisTemplateBaseVO> analysisTemplateMap) {
        //看板id集合
        List<String> analysisTemplateIds = new ArrayList<>();
        for (PortalMenu portalMenu : portalMenuList) {
            String parentId = portalMenu.getParentMenuId();
            List<PortalMenu> childMenuList = portalMenuMap.get(parentId);
            if (childMenuList == null) {
                childMenuList = new ArrayList<>();
            }
            childMenuList.add(portalMenu);
            portalMenuMap.put(parentId, childMenuList);

            PortalMenuType menuType = PortalMenuType.get(portalMenu.getMenuType());

            if (PortalMenuType.ANALYSIS_TEMPLATE == menuType) {
                analysisTemplateIds.add(portalMenu.getContentRefId());
            }
        }

        //查询看板的信息
        if (CollUtil.isNotEmpty(analysisTemplateIds)) {
            List<AnalysisTemplateBaseVO> analysisTemplateVOList = analysisTemplateService.getAnalysisTemplateBaseInfo(analysisTemplateIds);
            for (AnalysisTemplateBaseVO analysisTemplateVO : analysisTemplateVOList) {
                analysisTemplateMap.put(analysisTemplateVO.getAnalysisTplId(), analysisTemplateVO);
            }
        }
    }

    /**
     * 获取工作台下的菜单树 (校验编辑权限)
     * @return
     */
    public SSMResponseMessage<List<PortalMenuRsp>> getStudioMenuTree() {

        List<PortalMenuRsp> menuTreeData = new ArrayList<>();

        long t1 = System.currentTimeMillis();
        List<PortalRsp> portalRspList = list(FuncType.EDIT.getCode(),false);
        long t2 = System.currentTimeMillis();

       // System.out.println("查询门户耗时："+(t2-t1));

        //查询所有的菜单
        List<PortalMenu> portalMenuList = portalMenuService.getMenuList(null);

        long t3 = System.currentTimeMillis();
      //  System.out.println("查询菜单耗时："+(t3-t2));

        //构建菜单的父子关系
        Map<String,List<PortalMenu>> portalMenuMap = new HashMap<>();
        Map<String,AnalysisTemplateBaseVO> analysisTemplateMap = new HashMap<>();

        buildMenuTreeMetaData(portalMenuList,portalMenuMap,analysisTemplateMap);

        long t4 = System.currentTimeMillis();
      //  System.out.println("构建菜单树元信息耗时："+(t4-t3));

        //根节点
        PortalMenuRsp root = PortalMenuRsp.builder()
                .menuName(BIConsts.PORTAL_MENU_ROOT_NAME)
                .menuId(BIConsts.PORTAL_MENU_ROOT_ID)
                .menuType(PortalMenuType.COMMON.getCode())
                .hasAuth(Enabled.YES.getId())
                .build();

        //查询模板目录树结构
        Map<String, QueryTemplateCategory> queryTemplateCtgMap = buildQueryTemplateCtgMap(portalMenuList);
        Map<String, List<AiSkillPortalCtgVO>> aiSkillCtgMap = buildAiSkillCtgMap(portalMenuList);

        List<PortalMenuRsp> rootChildren = new ArrayList<>();
        for (PortalRsp portalRsp : portalRspList) {
            PortalMenuRsp portalMenuRsp = PortalMenuRsp.builder()
                    .menuId(portalRsp.getPortalId())
                    .menuName(portalRsp.getPortalName())
                    .menuType(PortalMenuType.COMMON.getCode())
                    .portalId(portalRsp.getPortalId())
                    .hasAuth(portalRsp.getHasAuth())
                    .menuDesc(portalRsp.getPortalDesc())
                    .parentMenuId(BIConsts.PORTAL_MENU_ROOT_ID)
                    .build();

            List<PortalMenuRsp> portalChildren = buildSubNode(portalRsp.getPortalId(), portalMenuMap, analysisTemplateMap,
                    portalRsp.getHasAuth(), Enabled.NO.getId(), queryTemplateCtgMap, aiSkillCtgMap, false);

            portalMenuRsp.setChildren(portalChildren);

            rootChildren.add(portalMenuRsp);
        }

        long t5 = System.currentTimeMillis();
      //  System.out.println("构建菜单树耗时："+(t5-t4));

        root.setChildren(rootChildren);
        menuTreeData.add(root);

        return SSMResponseMessage.success("查询成功！", menuTreeData);
    }

    public List<String> getStudioSpaceList() {
        // 查询有查看权限的门户
        List<PortalRsp> portalRspList = list(FuncType.VIEW.getCode(), false);

        List<String> authPortalIds = portalRspList.stream().filter(v->Enabled.YES.getId().equals(v.getHasAuth())).map(PortalRsp::getPortalId).collect(Collectors.toList());

        //查询所有的菜单
        List<PortalMenu> portalMenuList = portalMenuService.getMenuList(null);

        List<String> queryTemplateCtgIds = new ArrayList<>();
        for (PortalMenu portalMenu : portalMenuList) {
            if (!authPortalIds.contains(portalMenu.getPortalId())) {
                continue;
            }
            PortalMenuType menuType = PortalMenuType.get(portalMenu.getMenuType());
            if (PortalMenuType.SPACE_CTG == menuType || PortalMenuType.CONFIG_USED_SPACE_CTG == menuType) {
                queryTemplateCtgIds.add(portalMenu.getContentRefId());
            }
        }
        return queryTemplateCtgIds;
    }

    // 构建查询模板共享空间map
    private Map<String, QueryTemplateCategory> buildQueryTemplateCtgMap(List<PortalMenu> portalMenuList) {
        List<String> queryTemplateCtgIds = new ArrayList<>();
        for (PortalMenu portalMenu : portalMenuList) {
            PortalMenuType menuType = PortalMenuType.get(portalMenu.getMenuType());
            if (PortalMenuType.SPACE_CTG == menuType || PortalMenuType.CONFIG_USED_SPACE_CTG == menuType) {
                queryTemplateCtgIds.add(portalMenu.getContentRefId());
            }
        }

        if (CollUtil.isEmpty(queryTemplateCtgIds)) {
            return Collections.emptyMap();
        }

        List<QueryTemplateCategory> categories = queryTemplateCategoryService.getAllSpaceCategories();
        if (CollUtil.isEmpty(categories)) {
            return Collections.emptyMap();
        }

        categories = categories.stream().filter(c -> QueryTemplateCategoryType.SPACE.getId().equals(c.getParentId())).collect(Collectors.toList());
        //List<PortalMenuRsp> portalMenuRspList = travelCategory(categories);
        return categories.stream().collect(Collectors.toMap(QueryTemplateCategory::getId, k -> k));
    }

    /**
     * 批量拉取各门户技能集目录（仅含 {@link PortalMenuType#AI_SKILL_CTG} 的门户）。
     */
    private Map<String, List<AiSkillPortalCtgVO>> buildAiSkillCtgMap(List<PortalMenu> portalMenuList) {
        if (CollUtil.isEmpty(portalMenuList)) {
            return Collections.emptyMap();
        }
        List<String> portalIds = portalMenuList.stream()
                .filter(m -> PortalMenuType.AI_SKILL_CTG == PortalMenuType.get(m.getMenuType()))
                .map(PortalMenu::getPortalId)
                .distinct()
                .collect(Collectors.toList());
        return aiSkillPortalCtgClient.listBatchByPortalIds(portalIds);
    }

    /**
     * 将 AI 技能集目录树转为菜单节点。
     */
    private List<PortalMenuRsp> travelAiSkillCategory(List<AiSkillPortalCtgVO> categories, int hasAuth,
                                                      String portalId, String parentMenuId) {
        if (CollUtil.isEmpty(categories)) {
            return Collections.emptyList();
        }
        List<PortalMenuRsp> result = new ArrayList<>(categories.size());
        for (AiSkillPortalCtgVO category : categories) {
            if (category == null || StrUtil.isBlank(category.getCtgId())) {
                continue;
            }
            List<PortalMenuRsp> children = travelAiSkillCategory(category.getChildren(), hasAuth, portalId, category.getCtgId());
            PortalMenuContentRsp content = null;
            if (category.getSkillCount() != null) {
                content = PortalMenuContentRsp.builder().skillCount(category.getSkillCount()).build();
            }
            PortalMenuRsp menuRsp = PortalMenuRsp.builder()
                    .menuId(category.getCtgId())
                    .hasAuth(hasAuth)
                    .portalId(portalId)
                    .menuName(category.getCtgName())
                    .contentRefId(category.getCtgId())
                    .parentMenuId(parentMenuId)
                    .menuType(PortalMenuType.AI_SKILL_CTG.getCode())
                    .content(content)
                    .children(children)
                    .build();
            result.add(menuRsp);
        }
        return result;
    }

    //转换目录结构
    public List<PortalMenuRsp> travelCategory(List<QueryTemplateCategory> categories, int hasAuth, int hasEditAuth, String portalId) {
        if (CollUtil.isEmpty(categories)) {
            return Collections.emptyList();
        }

        List<PortalMenuRsp> result = new ArrayList<>(categories.size());
        for (QueryTemplateCategory category : categories) {
            List<PortalMenuRsp> children = travelCategory(category.getChildren(), hasAuth, hasEditAuth, portalId);
            PortalMenuRsp menuRsp = PortalMenuRsp.builder()
                    .menuId(category.getId())
                    .hasAuth(hasAuth)
                    .portalId(portalId)
                    .menuName(category.getName())
                    .contentRefId(category.getId())
                    .children(children)
                    .hasEditAuth(hasEditAuth)
                    .menuType(PortalMenuType.QUERY_TEMPLATE_CTG.getCode())
                    .build();
            result.add(menuRsp);
        }
        return result;
    }

    /**
     * 构建菜单子节点
     *
     * @param hideEmptyPortalDirectories 为 true 时（门户 getMenuTree）：{@link PortalMenuType#PORTAL_CTG}、
     * {@link PortalMenuType#DYNAMIC_ANALYSIS_REPORT_CTG}、{@link PortalMenuType#AI_SKILL_CTG} 在子节点为空时不加入树
     */
    public List<PortalMenuRsp> buildSubNode(String parentId, Map<String,List<PortalMenu>> portalMenuMap,
                             Map<String,AnalysisTemplateBaseVO> analysisTemplateMap,Integer hasAuth,Integer isQueryOnline,
                             Map<String, QueryTemplateCategory> queryTemplateCtgMap,
                             Map<String, List<AiSkillPortalCtgVO>> aiSkillCtgMap,
                             boolean hideEmptyPortalDirectories) {

        List<PortalMenu> childList = portalMenuMap.get(parentId);
        if (CollUtil.isEmpty(childList)) {
            return Collections.emptyList();
        }

        List<PortalMenuRsp> result = new ArrayList<>(childList.size());
        for (PortalMenu portalMenu : childList) {

            PortalMenuRsp portalMenuRsp = PortalMenuRsp.builder()
                    .portalId(portalMenu.getPortalId())
                    .menuId(portalMenu.getMenuId())
                    .menuName(portalMenu.getMenuName())
                    .menuDesc(portalMenu.getMenuDesc())
                    .menuType(portalMenu.getMenuType())
                    .parentMenuId(portalMenu.getParentMenuId())
                    .hasAuth(hasAuth)
                    .contentRefId(portalMenu.getContentRefId()).build();

            PortalMenuType menuType = PortalMenuType.get(portalMenu.getMenuType());
            if (PortalMenuType.ANALYSIS_TEMPLATE == menuType) {

                AnalysisTemplateBaseVO analysisTemplateVO = analysisTemplateMap.get(portalMenu.getContentRefId());

                if (analysisTemplateVO != null) {

                    //只查已上线的
                    if (Enabled.value(isQueryOnline) && AnalysisTemplateOnlineStatus.ONLINE != AnalysisTemplateOnlineStatus.codeOf(analysisTemplateVO.getOnlineStatus())) {
                        continue;
                    }

                    PortalMenuContentRsp portalMenuContentRsp = PortalMenuContentRsp.builder()
                            .publishStatus(analysisTemplateVO.getPublishStatus())
                            .hasDraft(analysisTemplateVO.getHasDraft())
                            .build();
                    portalMenuRsp.setContent(portalMenuContentRsp);
                    portalMenuRsp.setMenuPath(String.format("%s-%s", BIConsts.PORTAL_MENU_ROOT_NAME, portalMenu.getPortalName()));
                }

            }

            List<PortalMenuRsp> children = new ArrayList<>();
            if (PortalMenuType.SPACE_CTG == menuType) {
                // 加上共享空间的目录
                QueryTemplateCategory menuRsp = queryTemplateCtgMap.get(portalMenu.getContentRefId());
                if (menuRsp != null) {
                    portalMenuRsp.setMenuName(menuRsp.getName());
                    portalMenuRsp.setHasEditAuth(menuRsp.getIsAdmin());

                    children.addAll(travelCategory(menuRsp.getChildren(), hasAuth, menuRsp.getIsAdmin(), portalMenu.getPortalId()));
                }
            } else if (PortalMenuType.PORTAL_CTG == menuType) {
                children = buildSubNode(portalMenu.getMenuId(), portalMenuMap, analysisTemplateMap, hasAuth, isQueryOnline,
                        queryTemplateCtgMap, aiSkillCtgMap, hideEmptyPortalDirectories);
            } else if (PortalMenuType.DYNAMIC_ANALYSIS_REPORT_CTG == menuType) {
                // 专题分析报告目录：子节点为 dynamic_analysis_report 等，递归构建
                children = buildSubNode(portalMenu.getMenuId(), portalMenuMap, analysisTemplateMap, hasAuth, isQueryOnline,
                        queryTemplateCtgMap, aiSkillCtgMap, hideEmptyPortalDirectories);
            } else if (PortalMenuType.AI_SKILL_CTG == menuType) {
                List<AiSkillPortalCtgVO> skillCtgs = aiSkillCtgMap.getOrDefault(portalMenu.getPortalId(), Collections.emptyList());
                children.addAll(travelAiSkillCategory(skillCtgs, hasAuth, portalMenu.getPortalId(), portalMenu.getMenuId()));
            } else if (PortalMenuType.CONFIG_USED_SPACE_CTG == menuType) {
                if (Enabled.value(isQueryOnline)) {
                    // 线上不展示配置使用的共享空间
                    continue;
                } else {
                    // 加上配置使用共享空间的目录
                    QueryTemplateCategory menuRsp = queryTemplateCtgMap.get(portalMenu.getContentRefId());
                    if (menuRsp != null) {
                        portalMenuRsp.setMenuName(menuRsp.getName());
                        portalMenuRsp.setHasEditAuth(menuRsp.getIsAdmin());

                        children.addAll(travelCategory(menuRsp.getChildren(), hasAuth, menuRsp.getIsAdmin(), portalMenu.getPortalId()));
                    }
                }
            }
            portalMenuRsp.setChildren(children);

            if (hideEmptyPortalDirectories
                    && (PortalMenuType.PORTAL_CTG == menuType
                    || PortalMenuType.DYNAMIC_ANALYSIS_REPORT_CTG == menuType
                    || PortalMenuType.AI_SKILL_CTG == menuType)
                    && CollUtil.isEmpty(children)) {
                continue;
            }

            if (menuType == PortalMenuType.AI_SKILL_CTG) {
                portalMenuRsp.setChildren(Collections.emptyList());
            }

            result.add(portalMenuRsp);
        }
        return result;
    }

    /**
     * 获取用户模块权限
     * @return
     */
    public SSMResponseMessage<List<String>> getUserPortalModuleAuth() {

        List<String> moduleAuthList = new ArrayList<>();

        //默认都有多维分析的权限
        moduleAuthList.add(PortalModuleType.SSM.getCode());

        //超管
        if (portalRoleService.isPortalSuperAdmin()) {
            moduleAuthList.add(PortalModuleType.PORTAL.getCode());
            moduleAuthList.add(PortalModuleType.STUDIO.getCode());
        } else {

            List<String> funcCodeList = portalAuthService.getUserAllFuncCode();

            //编辑权限 -开通工作台
            long editFuncNum = funcCodeList.stream().filter(f -> f.equalsIgnoreCase(FuncType.EDIT.getCode())).count();
            if (editFuncNum > 0) {
                moduleAuthList.add(PortalModuleType.STUDIO.getCode());
            }

            //查看权限 -开通业务门户
            long viewFuncNum = funcCodeList.stream().filter(f -> f.equalsIgnoreCase(FuncType.VIEW.getCode())).count();
            if (viewFuncNum > 0) {
                moduleAuthList.add(PortalModuleType.PORTAL.getCode());
            }
        }

        return SSMResponseMessage.success("获取成功!", moduleAuthList);
    }

    //设置和修改默认门户
    public Boolean setDefaultPortal(PortalSetDefaultReq req) {
        String userName = UserManager.get().getName();
        UserProfileEntity entity = getDefaultPortal();
        if (entity != null) {
            entity.setUpdatedBy(userName);
            entity.setEntityId(req.getPortalId());
            entity.setEntityType(req.getPortalName());
            dao.update("ssm.user.profile.update", entity);
        } else {
            entity = new UserProfileEntity();
            entity.setCreatedBy(userName);
            entity.setUserName(userName);
            entity.setEntityId(req.getPortalId());
            entity.setEntityName(req.getPortalName());
            entity.setEntityType(UserProfileTypeEnum.DEFAULT_PORTAL.getCode());
            dao.insert("ssm.user.profile.add", entity);
        }
        return true;
    }


    // 获取默认门户
    private UserProfileEntity getDefaultPortal() {
        String userName = UserManager.get().getName();
        UserProfileEntity param = new UserProfileEntity();
        param.setEntityType(UserProfileTypeEnum.DEFAULT_PORTAL.getCode());
        param.setUserName(userName);
        return dao.queryObject("ssm.user.profile.get", param, UserProfileEntity.class);
    }

}
