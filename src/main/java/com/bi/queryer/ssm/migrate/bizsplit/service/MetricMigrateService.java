package com.bi.queryer.ssm.migrate.bizsplit.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.bi.queryer.ssm.migrate.bizsplit.enums.MetricMigrateObjectType;
import com.bi.queryer.ssm.migrate.bizsplit.model.MetricMigrateExecuteReq;
import com.bi.queryer.ssm.migrate.bizsplit.model.MetricMigrateExecuteRsp;
import com.bi.queryer.ssm.migrate.bizsplit.model.MetricMigrateMappingEntity;
import com.bi.queryer.ssm.migrate.bizsplit.model.MetricMigrateTarget;
import com.bi.queryer.ssm.migrate.bizsplit.model.QueryTemplateCopyResult;
import com.bi.queryer.ssm.migrate.bizsplit.processor.TplConfigMigrateProcessor;
import com.bi.queryer.ssm.migrate.bizsplit.util.MigrateMappingHelper;
import com.bi.queryer.ssm.portal.PortalMenuService;
import com.bi.queryer.ssm.portal.auth.entity.PortalRoleDept;
import com.bi.queryer.ssm.portal.auth.entity.PortalRoleUser;
import com.bi.queryer.ssm.portal.auth.service.PortalAuthService;
import com.bi.queryer.ssm.portal.auth.service.PortalRoleAuthService;
import com.bi.queryer.ssm.portal.auth.service.PortalRoleDeptService;
import com.bi.queryer.ssm.portal.auth.service.PortalRoleService;
import com.bi.queryer.ssm.portal.auth.service.PortalRoleUserService;
import com.bi.queryer.ssm.portal.auth.vo.req.PortalAuthQueryReq;
import com.bi.queryer.ssm.portal.auth.vo.rsp.PortalResourceAuthGetRsp;
import com.bi.queryer.ssm.portal.entity.Portal;
import com.bi.queryer.ssm.portal.entity.PortalMenu;
import com.bi.queryer.ssm.portal.enums.PortalMenuType;
import com.bi.queryer.ssm.portal.enums.ResType;
import com.bi.queryer.ssm.portal.enums.RoleType;
import com.bi.queryer.ssm.query.ctg.QueryTemplateCategoryService;
import com.bi.queryer.ssm.query.ctg.model.QueryTemplateCategory;
import com.bi.queryer.ssm.query.ctg.model.TemplateSpaceEntity;
import com.bi.queryer.ssm.query.template.model.TemplateEntity;
import com.bi.queryer.ssm.query.template.model.TemplateRsp;
import com.bi.queryer.sys.base.AbstractTransaction;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.Guid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 看板工作台目录迁移（业务线拆分）的核心编排服务。
 * 对应执行计划 doc/02设计/看板工作台目录迁移-业务线拆分执行计划.md 第4、6节：
 * 按 {@link MetricMigrateTarget#getSourceRootName()} 精确定位复制根节点，深度遍历整棵子树，
 * 对每个节点先做映射表幂等检查、再做同名复用检查，命中则复用已有对象，否则按节点类型分流创建
 * （3.1 纯目录直接插入 / 3.2 看板委托 {@link TplConfigMigrateProcessor} / 3.3 共享空间与查询模板本类直接处理），
 * 直接写正式表，不做影子表/预演/提交两阶段；同一入口可反复调用，只会增量同步尚未迁移的部分。
 */
@Service
public class MetricMigrateService {

    private static final Logger log = LoggerFactory.getLogger(MetricMigrateService.class);

    @Autowired
    private BaseDao dao;

    @Autowired
    private PortalMenuService portalMenuService;

    @Autowired
    private QueryTemplateCategoryService ctgService;

    @Autowired
    private MigrateMappingHelper mappingHelper;

    @Autowired
    private TplConfigMigrateProcessor dashboardProcessor;

    @Autowired(required = false)
    private QueryTemplateCopyService queryTemplateCopyService;

    @Autowired
    private PortalRoleService portalRoleService;

    @Autowired
    private PortalRoleAuthService portalRoleAuthService;

    @Autowired
    private PortalAuthService portalAuthService;

    @Autowired
    private PortalRoleUserService portalRoleUserService;

    @Autowired
    private PortalRoleDeptService portalRoleDeptService;

    public MetricMigrateExecuteRsp execute(MetricMigrateExecuteReq req) {
        if (CollUtil.isEmpty(req.getTargets())) {
            throw new BIException("迁移目标列表不能为空");
        }

        String operator = operatorName();
        List<MetricMigrateMappingEntity> collected = new ArrayList<>();

        for (MetricMigrateTarget target : req.getTargets()) {
            long targetStart = System.currentTimeMillis();
            Portal sourcePortal = portalMenuService.getPortalById(target.getPortalId());
            if (sourcePortal != null && target.getSourceRootName().equals(sourcePortal.getPortalName())) {
                // sourceRootName 命中的是门户名称本身：整个门户（连同其菜单树）一起复制，见执行计划"门户本身也要复制"
                copyWholePortal(sourcePortal, target, operator, collected);
            } else {
                PortalMenu sourceRoot = findSourceRoot(target.getPortalId(), target.getSourceRootName());
                // 分两遍走同一棵子树：第一遍跳过看板节点，只同步目录/共享空间（含递归，捕获任意深度/任意分支
                // 下的共享空间），保证看板引用到的共享空间映射在处理任何看板之前就已经全部建好——不能像过去
                // 那样只在"同一层级"把共享空间排到看板前面，因为看板和它引用的共享空间未必是同级节点
                // （可能分属不同分支、不同深度），那种排序只能保证同级顺序，保证不了跨分支/跨深度的顺序。
                // 第二遍再正常走一次：目录/共享空间此时已存在，直接查映射复用（SPACE_CTG 内部仍会做一次
                // 幂等的增量检查，见 processMenuNode 对已存在 SPACE_CTG 的处理），看板则在这一遍真正创建。
                // 两遍分别打点耗时，方便判断慢在"目录/共享空间同步"还是"看板创建"。
                long pass1Start = System.currentTimeMillis();
                processMenuNode(sourceRoot, sourceRoot.getParentMenuId(), target.getPortalId(), target, true, operator, collected, true);
                long pass1Cost = System.currentTimeMillis() - pass1Start;

                long pass2Start = System.currentTimeMillis();
                processMenuNode(sourceRoot, sourceRoot.getParentMenuId(), target.getPortalId(), target, true, operator, collected, false);
                long pass2Cost = System.currentTimeMillis() - pass2Start;

                log.info("[看板工作台目录迁移] target[{}] 目录/共享空间同步耗时{}ms，看板创建耗时{}ms",
                        target.getSourceRootName(), pass1Cost, pass2Cost);
            }
            log.info("[看板工作台目录迁移] target[{}] 处理完成，累计耗时{}ms",
                    target.getSourceRootName(), System.currentTimeMillis() - targetStart);
        }

        int reusedCount = (int) collected.stream().filter(m -> m.getIsReused() != null && m.getIsReused() == 1).count();
        return MetricMigrateExecuteRsp.builder()
                .totalProcessed(collected.size())
                .createdCount(collected.size() - reusedCount)
                .reusedCount(reusedCount)
                .mappings(collected)
                .build();
    }

    private String operatorName() {
        User user = UserManager.get();
        return user == null ? "system" : user.getName();
    }

    /**
     * 按 sourceRootName 在指定门户下精确匹配复制根节点（第4节：不做子串模糊扫描）。
     * 注意：门户元数据在 {@code ssm_portal} 表（{@code portal_name}），门户下的目录才在 {@code ssm_portal_menu} 表，
     * 门户本身没有对应的目录节点——如果 sourceRootName 传的是门户名称而不是门户下某个目录/看板的名称，
     * 这里必然查不到任何匹配，需要给出比"未找到"更明确的诊断信息，而不是让调用方误以为是名字打错了。
     */
    private PortalMenu findSourceRoot(String portalId, String sourceRootName) {
        Map<String, Object> params = new HashMap<>();
        params.put("portalId", portalId);
        List<PortalMenu> menus = dao.queryObjectList("ssm.portal.menu.getByPortalId", params, PortalMenu.class);
        List<PortalMenu> matched = new ArrayList<>();
        for (PortalMenu menu : menus) {
            if (sourceRootName.equals(menu.getMenuName())) {
                matched.add(menu);
            }
        }
        if (matched.isEmpty()) {
            Portal portal = portalMenuService.getPortalById(portalId);
            if (portal != null && sourceRootName.equals(portal.getPortalName())) {
                throw new BIException("[" + sourceRootName + "]是门户名称（存于 ssm_portal 表），门户本身没有对应的目录节点数据（目录存于 ssm_portal_menu 表）："
                        + "sourceRootName 必须是该门户下具体某个目录/看板的名称，不能是门户名称本身");
            }
            throw new BIException("门户[" + portalId + "]下未找到名为[" + sourceRootName + "]的复制来源目录");
        }
        if (matched.size() > 1) {
            throw new BIException("门户[" + portalId + "]下名为[" + sourceRootName + "]的目录不唯一，无法确定复制来源");
        }
        return matched.get(0);
    }

    /**
     * sourceRootName 命中门户名称本身时的整门户复制：复制/复用一个新 {@code ssm_portal} 行，
     * 再把老门户下所有顶层目录节点（{@code parent_menu_id == portal_id} 的约定，见
     * {@link PortalMenuService#addMenu}）都复制到新门户下。门户本身即为本次复制的"根"，
     * 所以门户名称按 {@code isRoot=true} 计算（可能带 nameSuffix），门户下的顶层目录则不再是根节点。
     */
    private void copyWholePortal(Portal oldPortal, MetricMigrateTarget target, String operator,
                                  List<MetricMigrateMappingEntity> collected) {
        MetricMigrateMappingEntity existing = mappingHelper.getMapping(
                MetricMigrateObjectType.PORTAL.getCode(), oldPortal.getPortalId(), target.getNewBizLine());
        String newPortalId;
        if (existing != null) {
            newPortalId = existing.getNewId();
        } else {
            String newPortalName = target.applyNameRule(oldPortal.getPortalName(), true);
            Portal existingByName = mappingHelper.findExistingPortal(newPortalName);
            // 排除源门户自己：改名规则如果对这个名字没有实际效果（oldToken 没出现、nameSuffix 又是空），
            // 按名称查重会把源门户自己也匹配上，不能当成"已存在的副本"复用，否则 newId 和 oldId 会相同
            if (existingByName != null && existingByName.getPortalId().equals(oldPortal.getPortalId())) {
                existingByName = null;
            }
            boolean reused = existingByName != null;
            newPortalId = reused ? existingByName.getPortalId() : createPortal(oldPortal, newPortalName, operator);
            collected.add(mappingHelper.recordMapping(MetricMigrateObjectType.PORTAL.getCode(),
                    oldPortal.getPortalId(), oldPortal.getPortalName(), newPortalId, newPortalName,
                    target.getOldToken(), target.getNewBizLine(), newPortalId, reused, operator));
        }

        List<PortalMenu> rootMenus = dao.queryObjectList("ssm.portal.menu.getByParentId", oldPortal.getPortalId(), PortalMenu.class);
        // 同 execute()：先跑一遍跳过看板的目录/共享空间同步（覆盖所有顶层分支），再跑一遍正常处理看板，
        // 避免看板引用的共享空间恰好落在其它顶层分支/更深层，导致看板先于它依赖的共享空间被处理到
        for (PortalMenu rootMenu : rootMenus) {
            processMenuNode(rootMenu, newPortalId, newPortalId, target, false, operator, collected, true);
        }
        for (PortalMenu rootMenu : rootMenus) {
            processMenuNode(rootMenu, newPortalId, newPortalId, target, false, operator, collected, false);
        }
    }

    /**
     * 新建一个门户不只是插入 {@code ssm_portal} 一行：参考 {@link com.bi.queryer.ssm.portal.PortalService#add}，
     * 门户要能正常使用还需要初始化角色（{@link PortalRoleService#batchAddPortalRole}）、
     * 角色权限（{@link PortalRoleAuthService#batchAddPortalRoleAuth}），并把老门户已有的管理员/协作者名单
     * 沿用过来，否则新门户建出来后没有任何人能管理它。
     * 不调用 {@code PortalService} 里"新增默认「专题分析报告」/「技能集」目录"的逻辑——
     * 老门户自己的这两个目录会随顶层菜单树一起被复制过来，再建一遍会产生重复目录。
     */
    private String createPortal(Portal oldPortal, String newPortalName, String operator) {
        String newPortalId = Guid.id();
        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {
                Portal newPortal = new Portal();
                newPortal.setPortalId(newPortalId);
                newPortal.setPortalName(newPortalName);
                newPortal.setPortalDesc(oldPortal.getPortalDesc());
                Double maxSortId = (Double) dao.queryObject("ssm.portal.queryMaxSortId", null);
                newPortal.setSortId((maxSortId == null ? 0 : maxSortId) + 1);
                newPortal.setCreatedBy(operator);
                dao.insert("ssm.portal.add", newPortal);

                List<String> roleIds = portalRoleService.batchAddPortalRole(newPortalId, newPortalName);
                portalRoleAuthService.batchAddPortalRoleAuth(roleIds, newPortalId, ResType.PORTAL.getCode());

                // 看板（门户）权限复制先注释掉，新门户的管理员/协作者名单暂不沿用老门户
//                copyPortalRoleAuth(oldPortal.getPortalId(), newPortalId, RoleType.PORTAL_ADMIN, operator);
//                copyPortalRoleAuth(oldPortal.getPortalId(), newPortalId, RoleType.PORTAL_WORKER, operator);
            }
        });
        return newPortalId;
    }

    /** 把老门户某个角色类型（管理员/协作者）下已授权的用户/部门，原样搬到新门户对应角色下 */
    private void copyPortalRoleAuth(String oldPortalId, String newPortalId, RoleType roleType, String operator) {
        PortalAuthQueryReq queryReq = new PortalAuthQueryReq();
        queryReq.setResId(oldPortalId);
        queryReq.setResType(ResType.PORTAL.getCode());
        queryReq.setRoleType(roleType.getCode());
        PortalResourceAuthGetRsp oldAuth = portalAuthService.get(queryReq);

        String newRoleId = portalRoleService.queryPortalRole(newPortalId, roleType.getCode()).getRoleId();

        if (CollUtil.isNotEmpty(oldAuth.getUserNameList())) {
            List<PortalRoleUser> users = new ArrayList<>();
            for (String userName : oldAuth.getUserNameList()) {
                users.add(new PortalRoleUser(newRoleId, userName, operator));
            }
            portalRoleUserService.insertRoleUser(users);
        }
        if (CollUtil.isNotEmpty(oldAuth.getDeptIdList())) {
            List<PortalRoleDept> depts = new ArrayList<>();
            for (String deptId : oldAuth.getDeptIdList()) {
                depts.add(new PortalRoleDept(newRoleId, deptId, operator));
            }
            portalRoleDeptService.insertRoleDept(depts);
        }
    }

    /**
     * 门户目录树节点分流处理（第6.2节 b 步）。
     *
     * @param skipDashboards true 表示第一遍（只同步目录/共享空间，跳过看板本身，见 {@link #execute} 的两遍调用说明）；
     *                       false 表示第二遍（正常处理全部节点类型，包括真正创建看板）
     */
    private String processMenuNode(PortalMenu oldNode, String newParentMenuId, String newPortalId, MetricMigrateTarget target,
                                    boolean isRoot, String operator,
                                    List<MetricMigrateMappingEntity> collected, boolean skipDashboards) {
        PortalMenuType type = PortalMenuType.get(oldNode.getMenuType());

        if (type == PortalMenuType.DYNAMIC_ANALYSIS_REPORT) {
            // 动态分析报告本身（静态 HTML/ZIP 内容）不迁移——是老业务线自己上传的报告文件，复制过去对新业务线
            // 没有意义；但外层的「专题分析报告」目录（DYNAMIC_ANALYSIS_REPORT_CTG）结构照常复制，
            // 只是复制出来是个空目录（不含下面这些报告文件）
            return null;
        }

        if (type == PortalMenuType.ANALYSIS_TEMPLATE && skipDashboards) {
            // 第一遍先跳过看板：看板在 ssm_portal_menu 树里是叶子（不需要继续递归），等第二遍再真正处理
            return null;
        }

        MetricMigrateMappingEntity existing = mappingHelper.getMapping(
                MetricMigrateObjectType.PORTAL_MENU.getCode(), oldNode.getMenuId(), target.getNewBizLine());
        if (existing != null) {
            if (type == PortalMenuType.SPACE_CTG || type == PortalMenuType.CONFIG_USED_SPACE_CTG) {
                // 共享空间目录树/模板的真正归属在 ssd_query_template_ctg，不在 ssm_portal_menu；
                // portal_menu 这一层的映射只代表"引用节点已挂过"，不能代表 ctg 树本身已经完全同步过。
                // copyCtgTree 内部按 ctg_id 自己做幂等+增量判断，所以这里即使 portal_menu 已存在也要重新进入。
                copyCtgTree(oldNode.getContentRefId(), target, isRoot, operator, collected);
            } else {
                // 已处理过：直接沿用映射，但仍需向下递归，捕获老树后续新增的子节点（增量执行）
                recurseMenuChildren(oldNode, existing.getNewId(), newPortalId, target, operator, collected, skipDashboards);
            }
            return existing.getNewId();
        }

        String newNodeId;
        if (type == PortalMenuType.ANALYSIS_TEMPLATE) {
            newNodeId = dashboardProcessor.copyDashboardMenu(oldNode, target, newParentMenuId, newPortalId, isRoot, operator, collected);
        } else if (type == PortalMenuType.SPACE_CTG || type == PortalMenuType.CONFIG_USED_SPACE_CTG) {
            newNodeId = copySpaceMenuNode(oldNode, target, newParentMenuId, newPortalId, isRoot, operator, collected);
        } else {
            newNodeId = copyPlainMenuNode(oldNode, target, newParentMenuId, newPortalId, isRoot, operator, collected);
        }

        if (newNodeId == null) {
            return null;
        }
        recurseMenuChildren(oldNode, newNodeId, newPortalId, target, operator, collected, skipDashboards);
        return newNodeId;
    }

    /** 看板、共享空间菜单节点在 ssm_portal_menu 树里是叶子，其"下级"内容分别落在看板内容表/ctg树，不在此递归 */
    private void recurseMenuChildren(PortalMenu oldNode, String newParentMenuId, String newPortalId, MetricMigrateTarget target,
                                      String operator, List<MetricMigrateMappingEntity> collected, boolean skipDashboards) {
        PortalMenuType type = PortalMenuType.get(oldNode.getMenuType());
        if (type == PortalMenuType.ANALYSIS_TEMPLATE || type == PortalMenuType.SPACE_CTG
                || type == PortalMenuType.CONFIG_USED_SPACE_CTG) {
            return;
        }
        List<PortalMenu> children = dao.queryObjectList("ssm.portal.menu.getByParentId", oldNode.getMenuId(), PortalMenu.class);
        if (CollUtil.isEmpty(children)) {
            return;
        }
        for (PortalMenu child : children) {
            processMenuNode(child, newParentMenuId, newPortalId, target, false, operator, collected, skipDashboards);
        }
    }

    /** 3.1 纯目录/内容引用节点复制：只插入新的 PortalMenu 行，contentRefId 保持指向原内容 */
    private String copyPlainMenuNode(PortalMenu oldNode, MetricMigrateTarget target, String newParentMenuId, String newPortalId,
                                      boolean isRoot, String operator,
                                      List<MetricMigrateMappingEntity> collected) {
        String newName = target.applyBizLineNameRule(oldNode.getMenuName(), isRoot);
        PortalMenu existingByName = mappingHelper.findExistingPortalMenu(newParentMenuId, oldNode.getMenuType(), newName);
        // 排除源节点自己（见 copyWholePortal 同类注释）：改名对这个名字没有实际效果时，按名称查重会把
        // 源节点自己也匹配上，导致复制变成"复用自己"，newId 和 oldId 相同
        if (existingByName != null && existingByName.getMenuId().equals(oldNode.getMenuId())) {
            existingByName = null;
        }
        boolean reused = existingByName != null;
        String newId;
        if (reused) {
            newId = existingByName.getMenuId();
        } else {
            PortalMenu newMenu = PortalMenu.builder()
                    .portalId(newPortalId)
                    .parentMenuId(newParentMenuId)
                    .menuName(newName)
                    .menuType(oldNode.getMenuType())
                    .menuDesc(target.applyNameRule(oldNode.getMenuDesc(), false))
                    .contentRefId(oldNode.getContentRefId())
                    .sortId(oldNode.getSortId())
                    .build();
            newId = portalMenuService.addMenu(newMenu);
        }
        collected.add(mappingHelper.recordMapping(MetricMigrateObjectType.PORTAL_MENU.getCode(),
                oldNode.getMenuId(), oldNode.getMenuName(), newId, newName,
                target.getOldToken(), target.getNewBizLine(), oldNode.getPortalId(), reused, operator));
        return newId;
    }

    /** 3.3 共享空间/查询模板目录节点复制：先复制（或复用）ctg 目录树+模板+ACL，再挂载 PortalMenu 行 */
    private String copySpaceMenuNode(PortalMenu oldNode, MetricMigrateTarget target, String newParentMenuId, String newPortalId,
                                      boolean isRoot, String operator,
                                      List<MetricMigrateMappingEntity> collected) {
        String newMenuName = target.applyBizLineNameRule(oldNode.getMenuName(), isRoot);
        PortalMenu existingMenu = mappingHelper.findExistingPortalMenu(newParentMenuId, oldNode.getMenuType(), newMenuName);
        // 排除源节点自己（见 copyWholePortal 同类注释）
        if (existingMenu != null && existingMenu.getMenuId().equals(oldNode.getMenuId())) {
            existingMenu = null;
        }
        boolean menuReused = existingMenu != null;

        String newCtgId = menuReused
                ? existingMenu.getContentRefId()
                : copyCtgTree(oldNode.getContentRefId(), target, isRoot, operator, collected);

        String newMenuId;
        if (menuReused) {
            newMenuId = existingMenu.getMenuId();
        } else {
            PortalMenu newMenu = PortalMenu.builder()
                    .portalId(newPortalId)
                    .parentMenuId(newParentMenuId)
                    .menuName(newMenuName)
                    .menuType(oldNode.getMenuType())
                    .menuDesc(target.applyNameRule(oldNode.getMenuDesc(), false))
                    .contentRefId(newCtgId)
                    .sortId(oldNode.getSortId())
                    .build();
            newMenuId = portalMenuService.addMenu(newMenu);
        }
        collected.add(mappingHelper.recordMapping(MetricMigrateObjectType.PORTAL_MENU.getCode(),
                oldNode.getMenuId(), oldNode.getMenuName(), newMenuId, newMenuName,
                target.getOldToken(), target.getNewBizLine(), oldNode.getPortalId(), menuReused, operator));
        return newMenuId;
    }

    /**
     * 复制（或复用）共享空间目录树根节点，新根挂在共享空间总目录下，与其它共享空间同级。
     * @param isRoot 该共享空间节点是否为本次迁移子树的根节点（与其对应的 PortalMenu 节点保持一致，决定命名后缀是否生效）
     */
    private String copyCtgTree(String oldCtgId, MetricMigrateTarget target, boolean isRoot, String operator,
                                List<MetricMigrateMappingEntity> collected) {
        QueryTemplateCategory oldRoot = ctgService.getCategoryById(oldCtgId);
        if (oldRoot == null) {
            throw new BIException("共享空间目录不存在：" + oldCtgId);
        }

        MetricMigrateMappingEntity existingRootMapping = mappingHelper.getMapping(
                MetricMigrateObjectType.SPACE_CTG.getCode(), oldCtgId, target.getNewBizLine());
        if (existingRootMapping != null) {
            // 根目录已处理过：仍需递归子目录+模板，捕获增量新增内容
            copyCtgChildrenAndTemplates(oldRoot, existingRootMapping.getNewId(), target, operator, collected);
            return existingRootMapping.getNewId();
        }

        boolean isNewRoot;
        String newRootId;
        String newRootName = target.applyBizLineNameRule(oldRoot.getName(), isRoot);
        QueryTemplateCategory existingByName = mappingHelper.findExistingCtg(BIConsts.TEMPLATE_CTG_SPACE_ID, newRootName);
        // 排除源目录自己（见 copyWholePortal 同类注释）
        if (existingByName != null && existingByName.getId().equals(oldRoot.getId())) {
            existingByName = null;
        }
        if (existingByName != null) {
            newRootId = existingByName.getId();
            isNewRoot = false;
        } else {
            QueryTemplateCategory newCtg = new QueryTemplateCategory();
            newCtg.setName(newRootName);
            newCtg.setDescription(oldRoot.getDescription());
            newCtg.setParentId(BIConsts.TEMPLATE_CTG_SPACE_ID);
            newCtg.setSortId(oldRoot.getSortId());
            newCtg.setCreatedBy(operator);
            newRootId = ctgService.addCategory(newCtg);
            isNewRoot = true;
        }
        collected.add(mappingHelper.recordMapping(MetricMigrateObjectType.SPACE_CTG.getCode(),
                oldRoot.getId(), oldRoot.getName(), newRootId, newRootName,
                target.getOldToken(), target.getNewBizLine(), null, !isNewRoot, operator));

        if (isNewRoot) {
            //copySpaceAcl(oldCtgId, newRootId);
        }
        copyCtgChildrenAndTemplates(oldRoot, newRootId, target, operator, collected);
        return newRootId;
    }

    /**
     * {@code ctgService.addCategory} 对 SPACE 类型分类会自动把当前迁移操作人插入为 owner_role=admin 的 ACL 行
     * （见 {@link QueryTemplateCategoryService#addCategory}）——如果不清理，这里再叠加老空间的 ACL 列表，
     * 要么产生重复行（操作人恰好也在老空间成员里），要么让操作人变成一个老空间里本来没有的"多余管理员"。
     * 参考 {@code TemplateSpaceService#editSpace} 的"先删后插"做法，先清空自动插入的行，
     * 保证新空间的 ACL 与老空间完全一致。
     */
    private void copySpaceAcl(String oldCtgId, String newCtgId) {
        dao.delete("ssm.template.ctg.deleteSpaceByCtgId", newCtgId);
        List<TemplateSpaceEntity> oldAcl = dao.queryObjectList("ssm.template.ctg.querySpaceListByCtgId", oldCtgId, TemplateSpaceEntity.class);
        if (CollUtil.isEmpty(oldAcl)) {
            return;
        }
        List<TemplateSpaceEntity> newAcl = new ArrayList<>();
        for (TemplateSpaceEntity acl : oldAcl) {
            TemplateSpaceEntity copy = new TemplateSpaceEntity();
            BeanUtil.copyProperties(acl, copy);
            copy.setPkid(Guid.id());
            copy.setSpaceCtgId(newCtgId);
            newAcl.add(copy);
        }
        dao.insert("ssm.template.ctg.batchAddTplSpace", newAcl);
    }

    /** 复制某 ctg 节点下的查询模板，并递归其子目录（子目录不是子树根节点，不追加 nameSuffix） */
    private void copyCtgChildrenAndTemplates(QueryTemplateCategory oldNode, String newCtgId, MetricMigrateTarget target,
                                              String operator, List<MetricMigrateMappingEntity> collected) {
        copyTemplatesUnderCtg(oldNode.getId(), newCtgId, target, operator, collected);

        List<QueryTemplateCategory> children = ctgService.getCategoriesWithAuth().stream()
                .filter(c -> oldNode.getId().equals(c.getParentId()))
                .collect(java.util.stream.Collectors.toList());
        for (QueryTemplateCategory child : children) {
            MetricMigrateMappingEntity existing = mappingHelper.getMapping(
                    MetricMigrateObjectType.SPACE_CTG.getCode(), child.getId(), target.getNewBizLine());
            if (existing != null) {
                copyCtgChildrenAndTemplates(child, existing.getNewId(), target, operator, collected);
                continue;
            }
            String childNewName = target.applyBizLineNameRule(child.getName(), false);
            QueryTemplateCategory existingByName = mappingHelper.findExistingCtg(newCtgId, childNewName);
            // 排除源目录自己（见 copyWholePortal 同类注释）
            if (existingByName != null && existingByName.getId().equals(child.getId())) {
                existingByName = null;
            }
            boolean reused = existingByName != null;
            String childNewId;
            if (reused) {
                childNewId = existingByName.getId();
            } else {
                QueryTemplateCategory newChild = new QueryTemplateCategory();
                newChild.setName(childNewName);
                newChild.setDescription(child.getDescription());
                newChild.setParentId(newCtgId);
                newChild.setSortId(child.getSortId());
                newChild.setCreatedBy(operator);
                childNewId = ctgService.addCategory(newChild);
            }
            collected.add(mappingHelper.recordMapping(MetricMigrateObjectType.SPACE_CTG.getCode(),
                    child.getId(), child.getName(), childNewId, childNewName,
                    target.getOldToken(), target.getNewBizLine(), null, reused, operator));
            copyCtgChildrenAndTemplates(child, childNewId, target, operator, collected);
        }
    }

    private void copyTemplatesUnderCtg(String oldCtgId, String newCtgId, MetricMigrateTarget target,
                                        String operator, List<MetricMigrateMappingEntity> collected) {
        List<TemplateRsp> templates = dao.queryObjectList(
                "ssm.template.ctg.queryTemplateByCtgIds", Collections.singletonList(oldCtgId), TemplateRsp.class);
        if (CollUtil.isEmpty(templates)) {
            return;
        }
        for (TemplateRsp oldTpl : templates) {
            MetricMigrateMappingEntity existing = mappingHelper.getMapping(
                    MetricMigrateObjectType.QUERY_TPL.getCode(), oldTpl.getTplId(), target.getNewBizLine());
            if (existing != null) {
                // 之前已经处理过这个老模板：不用重新复制/重新记映射，但要确保它现在挂在这次的目标目录下——
                // 模板可能是被 copyQueryTemplate 内部自己判重复用过来的，ctgId 未必跟这次的 newCtgId 一致
                updateTemplateCtg(existing.getNewId(), newCtgId);
                continue;
            }

            String newName = target.applyBizLineNameRule(oldTpl.getTplName(), false);
            TemplateEntity existingByName = mappingHelper.findExistingTemplate(newCtgId, newName);
            // 排除源模板自己（见 copyWholePortal 同类注释）
            if (existingByName != null && existingByName.getTplId().equals(oldTpl.getTplId())) {
                existingByName = null;
            }
            boolean reused = existingByName != null;
            String newTplId;
            List<QueryTemplateCopyResult.ViewIdMapping> viewMappings;
            if (reused) {
                newTplId = existingByName.getTplId();
                viewMappings = Collections.emptyList();
            } else {
                if (queryTemplateCopyService == null) {
                    throw new BIException("查询模板复制能力（QueryTemplateCopyService）尚未接入实现，无法复制模板：" + oldTpl.getTplName());
                }
                long copyStart = System.currentTimeMillis();
                QueryTemplateCopyResult result = queryTemplateCopyService.copyQueryTemplate(
                        oldTpl.getTplId(), target.getOldToken(), target.getNewToken());
                if (result == null) {
                    return;
                }
                log.info("[看板工作台目录迁移] 共享空间批量复制查询模板[{}]耗时{}ms",
                        oldTpl.getTplName(), System.currentTimeMillis() - copyStart);
                newTplId = result.getNewTplId();
                viewMappings = result.getViewIdMappings() == null ? Collections.emptyList() : result.getViewIdMappings();
            }
            // 不管是按名字复用到的、还是 copyQueryTemplate 复制/内部判重返回的，都要确保最终挂在这次的目标目录下——
            // 查询模板复制接口只管内容克隆，不管目录归属，目录归属由这里统一兜底。
            updateTemplateCtg(newTplId, newCtgId);

            collected.add(mappingHelper.recordMapping(MetricMigrateObjectType.QUERY_TPL.getCode(),
                    oldTpl.getTplId(), oldTpl.getTplName(), newTplId, newName,
                    target.getOldToken(), target.getNewBizLine(), null, reused, operator));

            for (QueryTemplateCopyResult.ViewIdMapping vm : viewMappings) {
                collected.add(mappingHelper.recordMapping(MetricMigrateObjectType.QUERY_TPL_VIEW.getCode(),
                        vm.getOldViewId(), null, vm.getNewViewId(), null,
                        target.getOldToken(), target.getNewBizLine(), null, reused, operator));
            }
        }
    }

    private void updateTemplateCtg(String tplId, String ctgId) {
        Map<String, Object> updateParams = new HashMap<>();
        updateParams.put("ctgId", ctgId);
        updateParams.put("tplIdList", Collections.singletonList(tplId));
        dao.update("ssm.template.ctg.updateTemplateCtg", updateParams);
    }
}
