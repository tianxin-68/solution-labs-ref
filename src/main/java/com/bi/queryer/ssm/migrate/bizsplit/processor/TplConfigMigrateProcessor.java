package com.bi.queryer.ssm.migrate.bizsplit.processor;

import com.bi.queryer.ssm.migrate.bizsplit.enums.MetricMigrateObjectType;
import com.bi.queryer.ssm.migrate.bizsplit.model.DashboardCopyResult;
import com.bi.queryer.ssm.migrate.bizsplit.model.MetricMigrateMappingEntity;
import com.bi.queryer.ssm.migrate.bizsplit.model.MetricMigrateTarget;
import com.bi.queryer.ssm.migrate.bizsplit.service.DashboardCopyService;
import com.bi.queryer.ssm.migrate.bizsplit.util.MigrateMappingHelper;
import com.bi.queryer.ssm.portal.PortalMenuService;
import com.bi.queryer.ssm.portal.entity.PortalMenu;
import com.bi.queryer.ssm.portal.enums.PortalMenuType;
import com.bi.queryer.sys.exception.BIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 看板节点的复制编排（执行计划 3.2 节）：
 * 同名复用检查 -&gt; 调用 {@link DashboardCopyService} 完成看板内容克隆 -&gt; 挂载新的 {@code PortalMenu} 行 -&gt; 记录映射。
 * 看板内容本身的克隆逻辑由 {@link DashboardCopyService} 的实现方承担，本类只负责调用与挂载。
 */
@Component
public class TplConfigMigrateProcessor {

    private static final Logger log = LoggerFactory.getLogger(TplConfigMigrateProcessor.class);

    @Autowired
    private PortalMenuService portalMenuService;

    @Autowired
    private MigrateMappingHelper mappingHelper;

    @Autowired(required = false)
    private DashboardCopyService dashboardCopyService;

    /**
     * 复制一个看板节点到新业务线（如果同名节点已存在则直接复用）。
     *
     * @param oldMenu       老看板对应的 PortalMenu 行（menuType=analysis_template）
     * @param target        新业务线目标
     * @param newParentMenuId 新看板要挂载到的父级目录 menuId
     * @param newPortalId   新看板所属的门户id（通常与老门户相同；sourceRootName 命中整个门户时为新建/复用的门户id）
     * @param isRoot        oldMenu 是否为本次复制的子树根节点（决定命名后缀是否生效）
     * @param operator      操作人
     * @param collected     本次运行的映射明细累加列表，供 {@code MetricMigrateExecuteRsp} 汇总
     * @return 新看板对应的 PortalMenu.menuId
     */
    public String copyDashboardMenu(PortalMenu oldMenu, MetricMigrateTarget target, String newParentMenuId, String newPortalId,
                                     boolean isRoot, String operator,
                                     List<MetricMigrateMappingEntity> collected) {
        String newName = target.applyBizLineNameRule(oldMenu.getMenuName(), isRoot);

        PortalMenu existing = mappingHelper.findExistingPortalMenu(
                newParentMenuId, PortalMenuType.ANALYSIS_TEMPLATE.getCode(), newName);
        // 排除源看板节点自己：改名规则对这个名字没有实际效果时，按名称查重会把源节点自己也匹配上，
        // 导致"复制"变成复用自己，newId 和 oldId 相同（MigrateMappingHelper#recordMapping 会直接报错）
        if (existing != null && existing.getMenuId().equals(oldMenu.getMenuId())) {
            existing = null;
        }

        if (existing != null) {
            recordDashboardMapping(oldMenu, existing.getMenuId(), existing.getContentRefId(),
                    existing.getMenuName(), target, true, operator, collected);
            return existing.getMenuId();
        }

        if (dashboardCopyService == null) {
            throw new BIException("看板复制能力（DashboardCopyService）尚未接入实现，无法复制看板：" + oldMenu.getMenuName());
        }

        // 传 target.getNewBizLine()（不是 getNewToken()）：DashboardCopyServiceImpl 内部把这个参数当
        // mappingHelper.getMapping/recordMapping 的分区键用（跟 MetricMigrateService/本类自己的
        // recordMapping 调用保持一致），newToken 只是名称子串替换用的片段，两者当前配置下取值相同，
        // 但语义不同——传错的话一旦 newToken 与 newBizLine 出现分歧，映射查找会静默查不到。
        long copyStart = System.currentTimeMillis();
        DashboardCopyResult copyResult = dashboardCopyService.copyDashboard(
                oldMenu.getContentRefId(), target.getOldToken(), target.getNewBizLine(), target.getOldBizLine(),
                oldMenu.getPortalId(), newPortalId);
        if (copyResult == null) {
            return null;
        }
        log.info("[看板工作台目录迁移] 看板[{}]复制耗时{}ms", oldMenu.getMenuName(), System.currentTimeMillis() - copyStart);

        PortalMenu newMenu = PortalMenu.builder()
                .portalId(newPortalId)
                .parentMenuId(newParentMenuId)
                .menuName(newName)
                .menuType(PortalMenuType.ANALYSIS_TEMPLATE.getCode())
                .menuDesc(target.applyNameRule(oldMenu.getMenuDesc(), false))
                .contentRefId(copyResult.getNewAnalysisTplId())
                .sortId(oldMenu.getSortId())
                .build();
        String newMenuId = portalMenuService.addMenu(newMenu);

        recordDashboardMapping(oldMenu, newMenuId, copyResult.getNewAnalysisTplId(), newName, target, false, operator, collected);
        return newMenuId;
    }

    private void recordDashboardMapping(PortalMenu oldMenu, String newMenuId, String newAnalysisTplId, String newName,
                                         MetricMigrateTarget target, boolean isReused, String operator,
                                         List<MetricMigrateMappingEntity> collected) {
        collected.add(mappingHelper.recordMapping(MetricMigrateObjectType.PORTAL_MENU.getCode(),
                oldMenu.getMenuId(), oldMenu.getMenuName(), newMenuId, newName,
                target.getOldToken(), target.getNewBizLine(), oldMenu.getPortalId(), isReused, operator));
        collected.add(mappingHelper.recordMapping(MetricMigrateObjectType.ANALYSIS_TPL.getCode(),
                oldMenu.getContentRefId(), oldMenu.getMenuName(), newAnalysisTplId, newName,
                target.getOldToken(), target.getNewBizLine(), oldMenu.getPortalId(), isReused, operator));
    }
}
