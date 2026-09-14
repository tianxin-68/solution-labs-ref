package com.bi.queryer.ssm.governance.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.governance.entity.FieldLabel;
import com.bi.queryer.ssm.governance.entity.GovernanceAuditLog;
import com.bi.queryer.ssm.governance.entity.GovernanceExempt;
import com.bi.queryer.ssm.governance.entity.GovernanceFieldTask;
import com.bi.queryer.ssm.governance.vo.BatchResult;
import com.bi.queryer.ssm.governance.enums.GovAction;
import com.bi.queryer.ssm.governance.enums.GovObjectType;
import com.bi.queryer.ssm.governance.enums.GovPolicyType;
import com.bi.queryer.ssm.governance.enums.GovStatus;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 指标/维度治理服务：扫描打标 → 通知（含重复推送）→ 豁免重评估。
 * <p>SSM 仅识别和通知，<strong>不在系统中执行下线</strong>，字段是否保留由业务方决策。
 */
@Service
public class GovernanceFieldService {

    private static final Logger log = LoggerFactory.getLogger(GovernanceFieldService.class);

    private static final int DEFAULT_EXEMPT = 30;
    /** noticed 重复推送间隔（天） */
    private static final int DEFAULT_REPEAT = 30;
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

    // ============================================================
    // 1. 扫描打标
    // ============================================================
    @SuppressWarnings("unchecked")
    public String scan() {
        // 上游为全量覆盖单日快照，扫描批次记为本次扫描日期
        String dt = DateUtil.today();

        List<GovernanceExempt> exemptRules = configService.queryActiveExempt(GovObjectType.FIELD.getCode());

        // 一次性加载存量任务（仅 id/编码/status 轻量列）到内存，避免逐行查询（N+1）
        List<GovernanceFieldTask> existList = (List<GovernanceFieldTask>) dao.queryObjectList("ssm.governance.field.queryAllTaskKeys", null);
        Map<String, GovernanceFieldTask> existMap = new HashMap<>(existList.size() * 2);
        for (GovernanceFieldTask e : existList) {
            existMap.put(e.getWhitePaperCode(), e);
        }

        int threshold = configService.noVisitThreshold();
        int created = 0, skipped = 0, total = 0;
        // keyset 分页扫描（按自增 id 游标）：每页处理后释放，内存恒定，不一次性查出全表
        Long lastId = null;
        while (true) {
            Map<String, Object> pageParam = new HashMap<>();
            pageParam.put("lastId", lastId);
            pageParam.put("pageSize", SCAN_PAGE_SIZE);
            List<FieldLabel> page = (List<FieldLabel>) dao.queryObjectList("ssm.governance.field.queryFieldLabels", pageParam);
            if (CollUtil.isEmpty(page)) {
                break;
            }
            total += page.size();

            List<GovernanceFieldTask> toInsert = new ArrayList<>();
            List<GovernanceAuditLog> toAudit = new ArrayList<>();
            for (FieldLabel label : page) {
                if (label.getDaysNoVisit() == null || label.getDaysNoVisit() < threshold) {
                    skipped++;
                    continue;
                }
                // 按 owner 灰度：未配置名单=全量；配置后仅名单内 owner 进入治理
                if (!configService.inGrayScopeAny(label.getWpOwner())) {
                    skipped++;
                    continue;
                }
                if (isExcluded(label, exemptRules)) {
                    skipped++;
                    continue;
                }
                GovernanceFieldTask exist = existMap.get(label.getWhitePaperCode());
                if (exist == null) {
                    toInsert.add(buildPendingTask(label, dt));
                    toAudit.add(scanAudit(label));
                    created++;
                } else if (GovStatus.PENDING.getCode().equals(exist.getStatus())) {
                    refreshScan(exist, label, dt);
                } else {
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

        String msg = String.format("指标/维度扫描完成 batch=%s：新增待处理 %d，跳过 %d，总计 %d",
                dt, created, skipped, total);
        log.info(msg);
        return msg;
    }

    /** 分批量插入（避免单条 SQL 过长） */
    private void batchInsertTasks(List<GovernanceFieldTask> tasks) {
        if (CollUtil.isEmpty(tasks)) {
            return;
        }
        for (List<GovernanceFieldTask> chunk : CollUtil.split(tasks, 1000)) {
            dao.insert("ssm.governance.field.batchInsert", chunk);
        }
    }

    private GovernanceAuditLog scanAudit(FieldLabel label) {
        return GovernanceAuditLog.builder()
                .objectType(auditObjectType(label.getWpType()))
                .objectId(label.getWhitePaperCode())
                .objectName(label.getWpName())
                .action(GovAction.SCAN_HIT)
                .toStatus(GovStatus.PENDING.getCode())
                .operator(GovAction.OPERATOR_SYSTEM)
                .remark("近 " + label.getDaysNoVisit() + " 天无查询")
                .build();
    }

    private boolean isExcluded(FieldLabel label, List<GovernanceExempt> exemptRules) {
        String ctgPath = StrUtil.nullToEmpty(label.getCtgPath());
        for (GovernanceExempt rule : exemptRules) {
            // 指定目录豁免：目录名命中 ctg_path；整个模块豁免：ctg_path 以模块名开头
            if (StrUtil.isNotBlank(rule.getCtgName())) {
                if (ctgPath.contains(rule.getCtgName())) {
                    return true;
                }
            } else if (StrUtil.isNotBlank(rule.getModuleName()) && ctgPath.startsWith(rule.getModuleName())) {
                return true;
            }
        }
        return false;
    }

    private GovernanceFieldTask buildBaseTask(FieldLabel label, String dt) {
        return GovernanceFieldTask.builder()
                .whitePaperCode(label.getWhitePaperCode())
                .objectType(StrUtil.isNotBlank(label.getWpType()) ? label.getWpType() : GovObjectType.FIELD.getCode())
                .ctgPath(label.getCtgPath())
                .wpName(label.getWpName())
                .wpOwner(label.getWpOwner())
                .wpLevel(label.getWpLevel())
                .daysNoVisit(label.getDaysNoVisit())
                .lastVisitTime(label.getLastVisitTime())
                .batchNo(dt)
                .build();
    }

    private GovernanceFieldTask buildPendingTask(FieldLabel label, String dt) {
        GovernanceFieldTask task = buildBaseTask(label, dt);
        task.setPolicyType(GovPolicyType.FIELD_NO_VISIT.getCode());
        task.setPolicyName(GovPolicyType.FIELD_NO_VISIT.getName(configService.noVisitThreshold()));
        task.setHitReason("近 " + label.getDaysNoVisit() + " 天无查询");
        task.setStatus(GovStatus.PENDING.getCode());
        task.setNoticeCount(0);
        return task;
    }

    private void refreshScan(GovernanceFieldTask exist, FieldLabel label, String dt) {
        GovernanceFieldTask task = buildBaseTask(label, dt);
        task.setId(exist.getId());
        task.setPolicyType(GovPolicyType.FIELD_NO_VISIT.getCode());
        task.setPolicyName(GovPolicyType.FIELD_NO_VISIT.getName(configService.noVisitThreshold()));
        task.setHitReason("近 " + label.getDaysNoVisit() + " 天无查询");
        dao.update("ssm.governance.field.refreshScan", task);
    }

    // ============================================================
    // 2. 通知：pending 首次通知；noticed 超过间隔重复推送
    // ============================================================
    @SuppressWarnings("unchecked")
    public String notifyPending() {
        List<GovernanceFieldTask> list = (List<GovernanceFieldTask>)
                dao.queryObjectList("ssm.governance.field.queryNeedNotify", DEFAULT_REPEAT);
        if (CollUtil.isEmpty(list)) {
            return "无待通知指标/维度任务";
        }
        // 1) 按单个 owner 聚合发送（wpOwner 可能多人逗号分隔），灰度名单外的责任人不推送
        Map<String, List<GovernanceFieldTask>> byOwner = new HashMap<>();
        List<GovernanceFieldTask> targets = new ArrayList<>();
        for (GovernanceFieldTask t : list) {
            boolean hit = false;
            for (String owner : splitOwners(t.getWpOwner())) {
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
        for (Map.Entry<String, List<GovernanceFieldTask>> e : byOwner.entrySet()) {
            String content = notifyService.buildFieldNoticeContent(e.getKey(), e.getValue());
            notifyService.notifyUser(e.getKey(), content);
        }

        // 2) 状态流转：每个被通知的任务只更新一次
        int noticedCount = 0;
        for (GovernanceFieldTask t : targets) {
            boolean firstNotice = GovStatus.PENDING.getCode().equals(t.getStatus());
            GovernanceFieldTask upd = GovernanceFieldTask.builder()
                    .id(t.getId())
                    .status(GovStatus.NOTICED.getCode())
                    .lastNoticeTime(new Date())
                    .noticeTime(firstNotice ? new Date() : t.getNoticeTime())
                    .noticeCount((t.getNoticeCount() == null ? 0 : t.getNoticeCount()) + 1)
                    .build();
            dao.update("ssm.governance.field.updateById", upd);
            audit(t, GovAction.NOTICE, t.getStatus(), GovStatus.NOTICED.getCode(),
                    GovAction.OPERATOR_SYSTEM, firstNotice ? "首次通知" : "重复推送");
            noticedCount++;
        }
        return "指标/维度通知完成，共推送 " + noticedCount + " 个，涉及负责人 " + byOwner.size() + " 人";
    }

    /** 拆分逗号分隔的多责任人，去重去空白 */
    private List<String> splitOwners(String ownersCsv) {
        if (StrUtil.isBlank(ownersCsv)) {
            return java.util.Collections.emptyList();
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
    // 3. 豁免到期重评估
    // ============================================================
    @SuppressWarnings("unchecked")
    public String recheckExempt() {
        List<GovernanceFieldTask> list = (List<GovernanceFieldTask>)
                dao.queryObjectList("ssm.governance.field.queryExemptExpired", null);
        if (CollUtil.isEmpty(list)) {
            return "无到期豁免指标/维度任务";
        }
        int count = 0;
        for (GovernanceFieldTask t : list) {
            GovernanceFieldTask upd = GovernanceFieldTask.builder()
                    .id(t.getId())
                    .status(GovStatus.PENDING.getCode())
                    .build();
            dao.update("ssm.governance.field.updateById", upd);
            audit(t, GovAction.RECHECK, GovStatus.EXEMPTED.getCode(), GovStatus.PENDING.getCode(),
                    GovAction.OPERATOR_SYSTEM, "豁免到期，重入待处理");
            count++;
        }
        return "指标/维度豁免到期重评估完成，共 " + count + " 个";
    }

    // ============================================================
    // 后台操作：豁免
    // ============================================================
    public void exempt(Long id) {
        String operator = currentUser();
        GovernanceFieldTask t = getById(id);
        if (t == null) {
            throw new IllegalArgumentException("指标/维度治理任务不存在：" + id);
        }
        int exemptDays = DEFAULT_EXEMPT;
        GovernanceFieldTask upd = GovernanceFieldTask.builder()
                .id(id)
                .status(GovStatus.EXEMPTED.getCode())
                .exemptTime(new Date())
                .exemptExpireDate(DateUtil.offsetDay(new Date(), exemptDays))
                .build();
        dao.update("ssm.governance.field.updateById", upd);
        audit(t, GovAction.EXEMPT, t.getStatus(), GovStatus.EXEMPTED.getCode(), operator,
                exemptDays + " 天后重新评估");
    }

    /** 批量豁免：逐条容错，单条失败不影响其余 */
    public BatchResult batchExempt(List<Long> ids) {
        BatchResult result = new BatchResult();
        if (CollUtil.isEmpty(ids)) {
            return result;
        }
        result.setTotal(ids.size());
        for (Long id : ids) {
            try {
                exempt(id);
                result.markSuccess();
            } catch (Exception e) {
                log.warn("[指标/维度治理] 批量豁免失败 id={}：{}", id, e.getMessage());
                result.markFail(id, e.getMessage());
            }
        }
        return result;
    }

    // ============================================================
    // 查询
    // ============================================================
    public GovernanceFieldTask getById(Long id) {
        return (GovernanceFieldTask) dao.queryObject("ssm.governance.field.getById", id);
    }

    @SuppressWarnings("unchecked")
    public PageInfo<GovernanceFieldTask> list(String status, String type, String owner, String keyword,
                                               String currentOwner, int pageNum, int pageSize) {
        Map<String, Object> param = new HashMap<>();
        param.put("status", status);
        param.put("type", type);
        param.put("owner", owner);
        param.put("keyword", keyword);
        param.put("currentOwner", currentOwner);
        param.put("startNum", (pageNum - 1) * pageSize);
        param.put("pageSize", pageSize);
        List<GovernanceFieldTask> list = (List<GovernanceFieldTask>) dao.queryObjectList("ssm.governance.field.list", param);
        Integer total = dao.queryCount("ssm.governance.field.listCount", param);
        fillUsedProducts(list);
        PageInfo<GovernanceFieldTask> page = new PageInfo<>(list);
        page.setTotal(total == null ? 0 : total);
        return page;
    }

    private void fillUsedProducts(List<GovernanceFieldTask> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        List<String> fieldCodes = list.stream()
                .map(GovernanceFieldTask::getWhitePaperCode)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
        if (fieldCodes.isEmpty()) {
            return;
        }
        List<Map> rows = (List<Map>) dao.queryObjectList("ssm.governance.field.listUsageStatsByFieldCodes", fieldCodes);
        if (CollUtil.isEmpty(rows)) {
            return;
        }
        Map<String, String> usedProductsMap = new HashMap<>();
        for (Map row : rows) {
            String fieldCode = (String) row.get("fieldCode");
            List<String> products = new ArrayList<>();
            if (toLong(row.get("queryViewCnt")) > 0)  products.add("多维视图");
            if (toLong(row.get("dashboardCnt")) > 0)  products.add("多维看板");
            if (toLong(row.get("skillCount")) > 0)    products.add("技能中心");
            if (toLong(row.get("pushTaskCount")) > 0) products.add("多维推送");
            if (!products.isEmpty()) {
                usedProductsMap.put(fieldCode, String.join(",", products));
            }
        }
        for (GovernanceFieldTask t : list) {
            String v = usedProductsMap.get(t.getWhitePaperCode());
            if (v != null) {
                t.setUsedProducts(v);
            }
        }
    }

    private long toLong(Object val) {
        if (val == null) return 0L;
        if (val instanceof Number) return ((Number) val).longValue();
        try { return Long.parseLong(val.toString()); } catch (Exception e) { return 0L; }
    }

    // ============================================================
    private void audit(GovernanceFieldTask t, String action, String from, String to, String operator, String remark) {
        auditService.log(auditObjectType(t.getObjectType()), t.getWhitePaperCode(), t.getWpName(), action, from, to, operator, remark);
    }

    /** 审计 objectType：指标/维度用 wpType（metric/dim）填，缺省回退 field */
    private String auditObjectType(String wpType) {
        return StrUtil.isNotBlank(wpType) ? wpType : GovObjectType.FIELD.getCode();
    }

    /** 当前登录用户名，无登录上下文时回退 system */
    private String currentUser() {
        User user = UserManager.get();
        return user != null && StrUtil.isNotBlank(user.getName()) ? user.getName() : GovAction.OPERATOR_SYSTEM;
    }
}
