package com.bi.queryer.ssm.governance.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.governance.entity.GovernanceAuditLog;
import com.bi.queryer.ssm.governance.entity.GovernanceExempt;
import com.bi.queryer.ssm.governance.entity.GovernanceViewTask;
import com.bi.queryer.ssm.governance.entity.ViewLabel;
import com.bi.queryer.ssm.governance.enums.GovAction;
import com.bi.queryer.ssm.governance.enums.GovObjectType;
import com.bi.queryer.ssm.governance.enums.GovPolicyType;
import com.bi.queryer.ssm.governance.enums.GovStatus;
import com.bi.queryer.ssm.governance.vo.BatchResult;
import com.bi.queryer.ssm.query.ctg.QueryTemplateCategoryService;
import com.bi.queryer.ssm.query.template.model.TemplateCtgPathEntity;
import com.bi.queryer.ssm.query.template.view.TemplateViewService;
import com.bi.queryer.ssm.query.template.view.model.TemplateViewEntity;
import com.bi.queryer.sys.base.AbstractTransaction;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 视图治理服务：扫描打标 → 通知 → 自动下线（软删除 + 保护期）→ 物理删除；
 * 支持豁免、回滚、立即下线等后台操作。
 */
@Service
public class GovernanceViewService {

    private static final Logger log = LoggerFactory.getLogger(GovernanceViewService.class);

    /** 默认宽限期（天） */
    private static final int DEFAULT_GRACE = 7;
    /** 默认下线保护期（天） */
    private static final int DEFAULT_PROTECT = 30;
    /** 默认豁免周期（天） */
    private static final int DEFAULT_EXEMPT = 30;
    /** 离职快速清理：未访问天数门槛 */
    private static final int OWNER_LEFT_DAYS = 90;
    private static final String VIEW_TYPE_PERSONAL = "personal";
    /** 扫描分页大小（keyset 游标，每页处理后释放，控制内存） */
    private static final int SCAN_PAGE_SIZE = 2000;

    @Autowired
    private BaseDao dao;

    @Autowired
    private GovernanceAuditService auditService;

    @Autowired
    private GovernanceConfigService configService;

    @Autowired
    private GovernanceNotifyService notifyService;

    @Autowired
    private TemplateViewService templateViewService;

    @Autowired
    private QueryTemplateCategoryService categoryService;

    /** 系统操作人标识（下线/删除/还原真实视图时写入 updated_by） */
    private static final String SYSTEM_OPERATOR = "governance-system";

    // ============================================================
    // 1. 扫描打标：读取上游 label 全量快照（全量覆盖、无 dt），应用策略生成/刷新治理任务
    // ============================================================
    @SuppressWarnings("unchecked")
    public String scan() {
        // 上游为全量覆盖单日快照，扫描批次记为本次扫描日期
        String dt = DateUtil.today();

        // 硬排除集合（体量小，一次性加载）
        Set<String> portalTplIds = new HashSet<>((List<String>) dao.queryObjectList("ssm.governance.view.queryOnlinePortalTplIds", null));
        List<GovernanceExempt> exemptRules = configService.queryActiveExempt(GovObjectType.VIEW.getCode());

        // 一次性加载存量任务（仅 id/viewId/status 轻量列）到内存，避免逐行查询（N+1）
        List<GovernanceViewTask> existList = (List<GovernanceViewTask>) dao.queryObjectList("ssm.governance.view.queryAllTaskKeys", null);
        Map<String, GovernanceViewTask> existMap = new HashMap<>(existList.size() * 2);
        for (GovernanceViewTask e : existList) {
            existMap.put(e.getViewId(), e);
        }

        int threshold = configService.noVisitThreshold();
        int created = 0, offlined = 0, skipped = 0, total = 0;
        // keyset 分页扫描（按自增 id 游标）：每页处理后释放，内存恒定，不一次性查出全表
        Long lastId = null;
        while (true) {
            Map<String, Object> pageParam = new HashMap<>();
            pageParam.put("lastId", lastId);
            pageParam.put("pageSize", SCAN_PAGE_SIZE);
            List<ViewLabel> page = (List<ViewLabel>) dao.queryObjectList("ssm.governance.view.queryViewLabels", pageParam);
            if (CollUtil.isEmpty(page)) {
                break;
            }
            total += page.size();

            // 本页按 ctgId 批量解析目录路径名（一次查询，避免逐行 N+1）
            Map<String, String> ctgPathMap = resolveCtgPathMap(page);

            List<GovernanceViewTask> toInsert = new ArrayList<>();
            List<GovernanceAuditLog> toAudit = new ArrayList<>();
            for (ViewLabel label : page) {
                boolean quickClean = isOwnerLeftQuickClean(label);
                boolean noVisitHit = label.getDaysNoVisit() != null && label.getDaysNoVisit() >= threshold;
                if (!quickClean && !noVisitHit) {
                    skipped++;
                    continue;
                }
                // owner 白名单：任一 owner 在白名单内则豁免，不进入治理
                if (configService.inOwnerWhitelist(label.getViewOwner())) {
                    skipped++;
                    continue;
                }
                // 按 owner 灰度：viewOwner 可能多人（逗号分隔），任一在名单内即进入治理；未配置名单=全量
                if (!configService.inGrayScopeAny(label.getViewOwner())) {
                    skipped++;
                    continue;
                }
                // 硬排除：挂载门户 / 被在线看板依赖 / 命中豁免目录
                if (isHardExcluded(label, portalTplIds, exemptRules)) {
                    skipped++;
                    continue;
                }

                // 目录路径名（通过 ctgId 解析），解析不到回退模板名
                String viewPath = ctgPathMap.getOrDefault(label.getCtgId(), label.getCtgId());

                GovernanceViewTask exist = existMap.get(label.getViewId());
                if (exist == null) {
                    if (quickClean) {
                        insertAndQuickOffline(label, dt, viewPath);
                        offlined++;
                    } else {
                        toInsert.add(buildPendingTask(label, dt, viewPath));
                        toAudit.add(scanAudit(label));
                        created++;
                    }
                } else if (GovStatus.PENDING.getCode().equals(exist.getStatus())) {
                    if (quickClean) {
                        applyQuickOffline(exist, label);
                        offlined++;
                    } else {
                        refreshScan(exist, label, dt, viewPath);
                    }
                } else {
                    // noticed/exempted/offline/rolled_back/purged 等在途状态，扫描不打断
                    skipped++;
                }
            }
            // 每页批量落库，处理完即释放本页对象
            batchInsertTasks(toInsert);
            auditService.batchLog(toAudit);

            lastId = page.get(page.size() - 1).getId();
            if (page.size() < SCAN_PAGE_SIZE) {
                break;
            }
        }

        String msg = String.format("视图扫描完成 batch=%s：新增待处理 %d，快速下线 %d，跳过 %d，总计 %d",
                dt, created, offlined, skipped, total);
        log.info(msg);
        return msg;
    }

    /** 本页 ctgId → 目录路径名（ctg_name_path），一次查询批量解析 */
    private Map<String, String> resolveCtgPathMap(List<ViewLabel> page) {
        Set<String> ctgIds = new HashSet<>();
        for (ViewLabel l : page) {
            if (StrUtil.isNotBlank(l.getCtgId())) {
                ctgIds.add(l.getCtgId());
            }
        }
        if (ctgIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<TemplateCtgPathEntity> paths = categoryService.getCtgPath(new ArrayList<>(ctgIds));
        Map<String, String> map = new HashMap<>(paths.size() * 2);
        for (TemplateCtgPathEntity p : paths) {
            map.put(p.getCtgId(), p.getCtgNamePath());
        }
        return map;
    }

    /** 分批量插入（避免单条 SQL 过长） */
    private void batchInsertTasks(List<GovernanceViewTask> tasks) {
        if (CollUtil.isEmpty(tasks)) {
            return;
        }
        for (List<GovernanceViewTask> chunk : CollUtil.split(tasks, 1000)) {
            dao.insert("ssm.governance.view.batchInsert", chunk);
        }
    }

    private GovernanceAuditLog scanAudit(ViewLabel label) {
        return GovernanceAuditLog.builder()
                .objectType(GovObjectType.VIEW.getCode())
                .objectId(label.getViewId())
                .objectName(viewDisplayName(label.getTplName(), label.getViewName()))
                .action(GovAction.SCAN_HIT)
                .toStatus(GovStatus.PENDING.getCode())
                .operator(GovAction.OPERATOR_SYSTEM)
                .remark(label.getDaysNoVisit() + " 天无查询")
                .build();
    }

    private boolean isHardExcluded(ViewLabel label, Set<String> portalTplIds,
                                   List<GovernanceExempt> exemptRules) {
        if (label.getIsPortal() != null && label.getIsPortal() == 1) {
            return true;
        }
        if (StrUtil.isNotBlank(label.getTplId()) && portalTplIds.contains(label.getTplId())) {
            return true;
        }
        for (GovernanceExempt rule : exemptRules) {
            if (StrUtil.isBlank(rule.getModuleName())) {
                continue;
            }
            // ctg_id 为空 = 整个模块豁免；非空 = 仅豁免该目录
            boolean moduleHit = rule.getModuleName().equals(label.getTplName())
                    || rule.getModuleName().equals(extractModule(label.getViewName()));
            if (StrUtil.isBlank(rule.getCtgId())) {
                if (moduleHit) {
                    return true;
                }
            } else if (rule.getCtgId().equals(label.getCtgId())) {
                return true;
            }
        }
        return false;
    }

    private String extractModule(String path) {
        if (StrUtil.isBlank(path)) {
            return "";
        }
        int idx = path.indexOf('/');
        return idx > 0 ? path.substring(0, idx).trim() : path.trim();
    }

    private boolean isOwnerLeftQuickClean(ViewLabel label) {
        return label.getIsOwnerAllLeft() != null && label.getIsOwnerAllLeft() == 1
                && label.getDaysNoVisit() != null && label.getDaysNoVisit() >= OWNER_LEFT_DAYS
                && VIEW_TYPE_PERSONAL.equalsIgnoreCase(label.getViewType());
    }

    private GovernanceViewTask buildBaseTask(ViewLabel label, String dt, String viewPath) {
        return GovernanceViewTask.builder()
                .viewId(label.getViewId())
                .cfgId(label.getCfgId())
                .viewName(label.getViewName())
                .viewType(label.getViewType())
                .tplType(label.getTplType())
                .tplId(label.getTplId())
                .ctgId(label.getCtgId())
                .tplName(label.getTplName())
                .viewPath(viewPath)
                .viewOwner(label.getViewOwner())
                .isPublicDomain(label.getIsPublicDomain())
                .isPortal(label.getIsPortal())
                .isOwnerAllLeft(label.getIsOwnerAllLeft())
                .daysNoVisit(label.getDaysNoVisit())
                .lastVisitTime(label.getLastVisitTime())
                .batchNo(dt)
                .build();
    }

    private GovernanceViewTask buildPendingTask(ViewLabel label, String dt, String viewPath) {
        GovernanceViewTask task = buildBaseTask(label, dt, viewPath);
        task.setPolicyType(GovPolicyType.NO_VISIT.getCode());
        task.setPolicyName(GovPolicyType.NO_VISIT.getName(configService.noVisitThreshold()));
        task.setHitReason(label.getDaysNoVisit() + " 天无查询");
        task.setStatus(GovStatus.PENDING.getCode());
        task.setIsActive(1);
        return task;
    }

    private void refreshScan(GovernanceViewTask exist, ViewLabel label, String dt, String viewPath) {
        GovernanceViewTask task = buildBaseTask(label, dt, viewPath);
        task.setId(exist.getId());
        task.setPolicyType(GovPolicyType.NO_VISIT.getCode());
        task.setPolicyName(GovPolicyType.NO_VISIT.getName(configService.noVisitThreshold()));
        task.setHitReason(label.getDaysNoVisit() + " 天无查询");
        dao.update("ssm.governance.view.refreshScan", task);
    }

    private void insertAndQuickOffline(ViewLabel label, String dt, String viewPath) {
        GovernanceViewTask task = buildBaseTask(label, dt, viewPath);
        task.setPolicyType(GovPolicyType.OWNER_LEFT_NO_VISIT.getCode());
        task.setPolicyName(GovPolicyType.OWNER_LEFT_NO_VISIT.getName(configService.noVisitThreshold()));
        task.setHitReason("责任人全部离职 且 " + label.getDaysNoVisit() + " 天无查询（个人视图快速清理）");
        task.setStatus(GovStatus.OFFLINE.getCode());
        task.setIsActive(0);
        task.setOfflineTime(new Date());
        task.setProtectExpireDate(DateUtil.offsetDay(new Date(), DEFAULT_PROTECT));
        // 删除真实视图 + 治理任务落库，同一事务保证原子（直接下线，跳过通知，进入保护期支持回滚）
        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {
                deleteView(task.getViewId());
                dao.insert("ssm.governance.view.insert", task);
            }
        });
        audit(task, GovAction.AUTO_OFFLINE, GovStatus.PENDING.getCode(), GovStatus.OFFLINE.getCode(),
                GovAction.OPERATOR_SYSTEM, "快速清理直接下线，进入保护期");
    }

    private void applyQuickOffline(GovernanceViewTask exist, ViewLabel label) {
        exist.setStatus(GovStatus.OFFLINE.getCode());
        exist.setPolicyType(GovPolicyType.OWNER_LEFT_NO_VISIT.getCode());
        exist.setPolicyName(GovPolicyType.OWNER_LEFT_NO_VISIT.getName(configService.noVisitThreshold()));
        exist.setIsActive(0);
        exist.setOfflineTime(new Date());
        exist.setProtectExpireDate(DateUtil.offsetDay(new Date(), DEFAULT_PROTECT));
        // 删除真实视图 + 治理任务更新，同一事务保证原子（直接下线，跳过通知，进入保护期支持回滚）
        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {
                deleteView(exist.getViewId());
                dao.update("ssm.governance.view.updateById", exist);
            }
        });
        audit(exist, GovAction.AUTO_OFFLINE, GovStatus.PENDING.getCode(), GovStatus.OFFLINE.getCode(),
                GovAction.OPERATOR_SYSTEM, "快速清理直接下线，进入保护期");
    }

    // ============================================================
    // 2. 通知：pending 按 owner 聚合推送，转 noticed，计算计划下线日期
    // ============================================================
    @SuppressWarnings("unchecked")
    public String notifyPending() {
        List<GovernanceViewTask> pendingList = (List<GovernanceViewTask>)
                dao.queryObjectList("ssm.governance.view.queryByStatus", GovStatus.PENDING.getCode());
        if (CollUtil.isEmpty(pendingList)) {
            return "无待通知视图任务";
        }
        int graceDays = DEFAULT_GRACE;
        Date planOffline = DateUtil.offsetDay(new Date(), graceDays);
        pendingList.forEach(t -> t.setPlanOfflineDate(planOffline));

        // 1) 按单个 owner 聚合发送（viewOwner 可能多人逗号分隔），灰度名单外的责任人不推送
        Map<String, List<GovernanceViewTask>> byOwner = new HashMap<>();
        List<GovernanceViewTask> targets = new ArrayList<>();
        for (GovernanceViewTask t : pendingList) {
            boolean hit = false;
            for (String owner : splitOwners(t.getViewOwner())) {
                if (!configService.inGrayScope(owner)) {
                    continue;
                }
                byOwner.computeIfAbsent(owner, k -> new ArrayList<>()).add(t);
                hit = true;
            }
            // 无任一责任人在灰度名单内 → 不通知、不流转
            if (hit) {
                targets.add(t);
            }
        }
        for (Map.Entry<String, List<GovernanceViewTask>> e : byOwner.entrySet()) {
            String content = notifyService.buildViewNoticeContent(e.getKey(), e.getValue());
            notifyService.notifyUser(e.getKey(), content);
        }

        // 2) 状态流转：每个被通知的任务只更新一次
        int noticedCount = 0;
        for (GovernanceViewTask t : targets) {
            GovernanceViewTask upd = GovernanceViewTask.builder()
                    .id(t.getId())
                    .status(GovStatus.NOTICED.getCode())
                    .noticeTime(new Date())
                    .planOfflineDate(planOffline)
                    .build();
            dao.update("ssm.governance.view.updateById", upd);
            audit(t, GovAction.NOTICE, GovStatus.PENDING.getCode(), GovStatus.NOTICED.getCode(),
                    GovAction.OPERATOR_SYSTEM, "计划下线日期：" + DateUtil.formatDate(planOffline));
            noticedCount++;
        }
        return "视图通知完成，转为已通知 " + noticedCount + " 个，涉及负责人 " + byOwner.size() + " 人";
    }

    /** 拆分逗号分隔的多责任人，去重去空白 */
    private List<String> splitOwners(String ownersCsv) {
        if (StrUtil.isBlank(ownersCsv)) {
            return Collections.emptyList();
        }
        List<String> owners = new ArrayList<>();
        for (String o : ownersCsv.split(",")) {
            String name = StrUtil.trim(o);
            if (StrUtil.isNotBlank(name) && !owners.contains(name)) {
                owners.add(name);
            }
        }
        return owners;
    }

    // ============================================================
    // 3. 自动下线：noticed 且到达计划下线日 → offline（软删除 + 保护期）
    // ============================================================
    @SuppressWarnings("unchecked")
    public String autoOffline() {
        List<GovernanceViewTask> dueList = (List<GovernanceViewTask>)
                dao.queryObjectList("ssm.governance.view.queryNoticedDue", null);
        if (CollUtil.isEmpty(dueList)) {
            return "无到期待下线视图任务";
        }
        int protectDays = DEFAULT_PROTECT;

        int count = 0, failed = 0;
        for (GovernanceViewTask t : dueList) {
            try {
                doOffline(t, GovAction.AUTO_OFFLINE, GovAction.OPERATOR_SYSTEM, protectDays, "宽限期满自动下线");
                count++;
            } catch (Exception e) {
                failed++;
                log.error("[视图治理] 自动下线失败 viewId={}：{}", t.getViewId(), e.getMessage(), e);
            }
        }
        return "视图自动下线完成，成功 " + count + " 个，失败 " + failed + " 个，进入 " + protectDays + " 天保护期";
    }

    private void doOffline(GovernanceViewTask t, String action, String operator, int protectDays, String remark) {
        String from = t.getStatus();
        GovernanceViewTask upd = GovernanceViewTask.builder()
                .id(t.getId())
                .status(GovStatus.OFFLINE.getCode())
                .isActive(0)
                .offlineTime(new Date())
                .protectExpireDate(DateUtil.offsetDay(new Date(), protectDays))
                .build();
        // 删除真实视图 + 治理任务状态更新，同一事务保证原子
        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {
                deleteView(t.getViewId());
                dao.update("ssm.governance.view.updateById", upd);
            }
        });
        audit(t, action, from, GovStatus.OFFLINE.getCode(), operator, remark);
    }

    // ============================================================
    // 4. 物理删除：offline 且保护期满且无反馈 → purged（不可恢复）
    // ============================================================
    @SuppressWarnings("unchecked")
    public String purgeExpired() {
        List<GovernanceViewTask> expiredList = (List<GovernanceViewTask>)
                dao.queryObjectList("ssm.governance.view.queryProtectExpired", null);
        if (CollUtil.isEmpty(expiredList)) {
            return "无保护期满待删除视图任务";
        }
        int count = 0, failed = 0;
        for (GovernanceViewTask t : expiredList) {
            try {
                // 保护期满不可恢复：彻底删除 cfg 配置 + 配置扩展（视图主行已在下线时删除），并删除治理任务行
                dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
                    @Override
                    public void execute() {
                        purgeViewCfg(t.getViewId(), t.getCfgId(), t.getTplId());
                        dao.delete("ssm.governance.view.deleteById", t.getId());
                    }
                });
                audit(t, GovAction.PURGE, GovStatus.OFFLINE.getCode(), GovStatus.PURGED.getCode(),
                        GovAction.OPERATOR_SYSTEM, "保护期满无反馈，物理删除（含治理任务）");
                count++;
            } catch (Exception e) {
                failed++;
                log.error("[视图治理] 物理删除失败 viewId={}：{}", t.getViewId(), e.getMessage(), e);
            }
        }
        return "视图物理删除完成，成功 " + count + " 个，失败 " + failed + " 个";
    }

    // ============================================================
    // 5. 豁免到期重评估：exempted 且到期 → pending 重入
    // ============================================================
    @SuppressWarnings("unchecked")
    public String recheckExempt() {
        List<GovernanceViewTask> list = (List<GovernanceViewTask>)
                dao.queryObjectList("ssm.governance.view.queryExemptExpired", null);
        if (CollUtil.isEmpty(list)) {
            return "无到期豁免视图任务";
        }
        int count = 0;
        for (GovernanceViewTask t : list) {
            GovernanceViewTask upd = GovernanceViewTask.builder()
                    .id(t.getId())
                    .status(GovStatus.PENDING.getCode())
                    .build();
            dao.update("ssm.governance.view.updateById", upd);
            audit(t, GovAction.RECHECK, GovStatus.EXEMPTED.getCode(), GovStatus.PENDING.getCode(),
                    GovAction.OPERATOR_SYSTEM, "豁免到期，重入待处理");
            count++;
        }
        return "视图豁免到期重评估完成，共 " + count + " 个";
    }

    // ============================================================
    // 后台操作：豁免 / 回滚 / 立即下线
    // ============================================================
    public void exempt(Long id) {
        String operator = currentUser();
        GovernanceViewTask t = getById(id);
        if (t == null) {
            throw new IllegalArgumentException("视图治理任务不存在：" + id);
        }
        int exemptDays = DEFAULT_EXEMPT;
        GovernanceViewTask upd = GovernanceViewTask.builder()
                .id(id)
                .status(GovStatus.EXEMPTED.getCode())
                .exemptTime(new Date())
                .exemptExpireDate(DateUtil.offsetDay(new Date(), exemptDays))
                .build();
        dao.update("ssm.governance.view.updateById", upd);
        audit(t, GovAction.EXEMPT, t.getStatus(), GovStatus.EXEMPTED.getCode(), operator,
                exemptDays + " 天后重新评估");
    }

    public void rollback(Long id) {
        String operator = currentUser();
        GovernanceViewTask t = getById(id);
        if (t == null) {
            throw new IllegalArgumentException("视图治理任务不存在：" + id);
        }
        if (!GovStatus.OFFLINE.getCode().equals(t.getStatus())) {
            throw new IllegalStateException("仅已下线（保护期内）视图可回滚，当前状态：" + t.getStatus());
        }
        // 下线时仅删了视图主行（cfg 保留），回滚用治理任务字段重建视图主行
        TemplateViewEntity entity = buildRestoreEntity(t, operator);
        GovernanceViewTask upd = GovernanceViewTask.builder()
                .id(id)
                .status(GovStatus.ROLLED_BACK.getCode())
                .isActive(1)
                .rollbackTime(new Date())
                .build();
        // 重建真实视图 + 治理任务更新，同一事务保证原子
        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {
                templateViewService.restoreViewNoTx(entity);
                dao.update("ssm.governance.view.updateById", upd);
            }
        });
        audit(t, GovAction.ROLLBACK, GovStatus.OFFLINE.getCode(), GovStatus.ROLLED_BACK.getCode(),
                operator, "保护期内回滚恢复");
    }

    /** 用治理任务字段重建视图实体（cfg 未删，仅需重建视图主行） */
    private TemplateViewEntity buildRestoreEntity(GovernanceViewTask t, String operator) {
        TemplateViewEntity entity = new TemplateViewEntity();
        entity.setViewId(t.getViewId());
        entity.setCfgId(t.getCfgId());
        entity.setViewName(t.getViewName());
        entity.setViewType(t.getViewType());
        entity.setTplId(t.getTplId());
        entity.setIsActive(Enabled.YES.getId());
        entity.setCreatedBy(StrUtil.isBlank(operator) ? SYSTEM_OPERATOR : operator);
        entity.setUpdatedBy(StrUtil.isBlank(operator) ? SYSTEM_OPERATOR : operator);
        return entity;
    }

    public void offlineNow(Long id) {
        String operator = currentUser();
        GovernanceViewTask t = getById(id);
        if (t == null) {
            throw new IllegalArgumentException("视图治理任务不存在：" + id);
        }
        if (!GovStatus.PENDING.getCode().equals(t.getStatus()) && !GovStatus.NOTICED.getCode().equals(t.getStatus())) {
            throw new IllegalStateException("仅待处理/已通知视图可立即下线，当前状态：" + t.getStatus());
        }
        doOffline(t, GovAction.OFFLINE_NOW, operator, DEFAULT_PROTECT, "立即下线，跳过宽限期");
    }

    // ============================================================
    // 批量操作：逐条独立事务/容错，单条失败不影响其余
    // ============================================================
    public BatchResult batchExempt(List<Long> ids) {
        return batch(ids, this::exempt);
    }

    public BatchResult batchRollback(List<Long> ids) {
        return batch(ids, this::rollback);
    }

    public BatchResult batchOfflineNow(List<Long> ids) {
        return batch(ids, this::offlineNow);
    }

    private BatchResult batch(List<Long> ids, java.util.function.Consumer<Long> op) {
        BatchResult result = new BatchResult();
        if (CollUtil.isEmpty(ids)) {
            return result;
        }
        result.setTotal(ids.size());
        for (Long id : ids) {
            try {
                op.accept(id);
                result.markSuccess();
            } catch (Exception e) {
                log.warn("[视图治理] 批量操作失败 id={}：{}", id, e.getMessage());
                result.markFail(id, e.getMessage());
            }
        }
        return result;
    }

    // ============================================================
    // 查询
    // ============================================================
    public GovernanceViewTask getById(Long id) {
        return (GovernanceViewTask) dao.queryObject("ssm.governance.view.getById", id);
    }

    @SuppressWarnings("unchecked")
    public PageInfo<GovernanceViewTask> list(String status, String owner, String keyword, String currentOwner,
                                             int pageNum, int pageSize) {
        Map<String, Object> param = new HashMap<>();
        param.put("status", status);
        param.put("owner", owner);
        param.put("keyword", keyword);
        param.put("currentOwner", currentOwner);
        param.put("startNum", (pageNum - 1) * pageSize);
        param.put("pageSize", pageSize);
        List<GovernanceViewTask> list = (List<GovernanceViewTask>) dao.queryObjectList("ssm.governance.view.list", param);
        Integer total = dao.queryCount("ssm.governance.view.listCount", param);
        PageInfo<GovernanceViewTask> page = new PageInfo<>(list);
        page.setTotal(total == null ? 0 : total);
        return page;
    }

    // ============================================================
    // 内部工具
    /**
     * 删除真实视图：仅删除视图主行，<strong>保留 cfg 配置</strong>（便于回滚仅重建视图行即可）。
     */
    private void deleteView(String viewId) {
        dao.delete("ssm.template.view.delete", viewId);
    }

    /**
     * 保护期满彻底清理：删除视图主行（幂等兜底）+ cfg 配置 + 配置扩展，不可恢复。
     * 若清理后该查询模板已无任何视图，则连带删除查询模板。
     */
    private void purgeViewCfg(String viewId, String cfgId, String tplId) {
        dao.delete("ssm.template.view.delete", viewId);
        if (StrUtil.isNotBlank(cfgId)) {
            dao.delete("ssm.template.cfg.delete", cfgId);
            dao.delete("ssm.query.template.cfg.dtl.delete", cfgId);
        }
        if (StrUtil.isNotBlank(tplId)) {
            Integer remaining = dao.queryCount("ssm.template.view.countByTplId", tplId);
            if (remaining == null || remaining == 0) {
                dao.delete("ssm.query.deleteTemplate", tplId);
                log.info("[视图治理] 模板 {} 已无任何视图，连带删除查询模板", tplId);
            }
        }
    }

    private void audit(GovernanceViewTask t, String action, String from, String to, String operator, String remark) {
        auditService.log(GovObjectType.VIEW.getCode(), t.getViewId(),
                viewDisplayName(t.getTplName(), t.getViewName()), action, from, to, operator, remark);
    }

    /** 审计对象名称：模板名称_视图名称 */
    private String viewDisplayName(String tplName, String viewName) {
        return StrUtil.nullToEmpty(tplName) + "_" + StrUtil.nullToEmpty(viewName);
    }

    /** 当前登录用户名，无登录上下文时回退 system */
    private String currentUser() {
        User user = UserManager.get();
        return user != null && StrUtil.isNotBlank(user.getName()) ? user.getName() : GovAction.OPERATOR_SYSTEM;
    }
}
