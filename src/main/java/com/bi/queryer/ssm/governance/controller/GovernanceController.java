package com.bi.queryer.ssm.governance.controller;

import com.bi.queryer.ssm.governance.entity.GovernanceAuditLog;
import com.bi.queryer.ssm.governance.entity.GovernanceExempt;
import com.bi.queryer.ssm.governance.entity.GovernanceFieldTask;
import com.bi.queryer.ssm.governance.entity.GovernanceViewTask;
import com.bi.queryer.ssm.governance.req.*;
import com.bi.queryer.ssm.governance.service.GovernanceAuditService;
import com.bi.queryer.ssm.governance.service.GovernanceConfigService;
import com.bi.queryer.ssm.governance.service.GovernanceFieldService;
import com.bi.queryer.ssm.governance.service.GovernanceViewService;
import com.bi.queryer.ssm.governance.vo.BatchResult;
import com.bi.queryer.ssm.governance.vo.GovernanceFieldVO;
import com.bi.queryer.ssm.governance.vo.GovernanceViewVO;
import com.bi.queryer.sys.common.SSMResponseMessage;
import com.bi.queryer.sys.role.RoleService;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 多维内容治理后台接口：视图治理、指标/维度治理、审计日志、豁免配置。
 */
@RestController
@Scope("prototype")
@RequestMapping("ssm/governance")
@Slf4j
public class GovernanceController {

    @Autowired
    private GovernanceViewService viewService;

    @Autowired
    private GovernanceFieldService fieldService;

    @Autowired
    private GovernanceAuditService auditService;

    @Autowired
    private GovernanceConfigService configService;

    @Autowired
    private RoleService roleService;

    // ===================== 视图治理 =====================

    /** 视图治理-分页列表 */
    @RequestMapping(value = "view/list", method = RequestMethod.POST)
    public SSMResponseMessage<PageInfo<GovernanceViewVO>> viewList(@RequestBody GovernanceViewQueryReq req) {
        PageInfo<GovernanceViewTask> page = viewService.list(req.getStatus(), req.getOwner(),
                req.getKeyword(), selfOwnerScope(), req.getCurrPageNo(), req.getPrePageSize());
        return SSMResponseMessage.success("查询成功", mapPage(page, GovernanceViewVO::from));
    }

    /** 视图治理-详情 */
    @RequestMapping(value = "view/detail", method = RequestMethod.GET)
    public SSMResponseMessage<GovernanceViewVO> viewDetail(@RequestParam Long id) {
        return SSMResponseMessage.success("查询成功", GovernanceViewVO.from(viewService.getById(id)));
    }

    /** 视图治理-申请延迟30天 */
    @RequestMapping(value = "view/exempt", method = RequestMethod.POST)
    public SSMResponseMessage<Void> viewExempt(@RequestParam Long id) {
        viewService.exempt(id);
        return SSMResponseMessage.success("已设为豁免");
    }

    /** 视图治理-保护期内回滚恢复 */
    @RequestMapping(value = "view/rollback", method = RequestMethod.POST)
    public SSMResponseMessage<Void> viewRollback(@RequestParam Long id) {
        viewService.rollback(id);
        return SSMResponseMessage.success("回滚成功");
    }

    /** 视图治理-立即下线（跳过宽限期） */
    @RequestMapping(value = "view/offline", method = RequestMethod.POST)
    public SSMResponseMessage<Void> viewOffline(@RequestParam Long id) {
        viewService.offlineNow(id);
        return SSMResponseMessage.success("已下线");
    }

    /** 视图治理-批量豁免 */
    @RequestMapping(value = "view/exempt/batch", method = RequestMethod.POST)
    public SSMResponseMessage<BatchResult> viewExemptBatch(@RequestBody List<Long> ids) {
        return SSMResponseMessage.success("操作完成", viewService.batchExempt(ids));
    }

    /** 视图治理-批量回滚 */
    @RequestMapping(value = "view/rollback/batch", method = RequestMethod.POST)
    public SSMResponseMessage<BatchResult> viewRollbackBatch(@RequestBody List<Long> ids) {
        return SSMResponseMessage.success("操作完成", viewService.batchRollback(ids));
    }

    /** 视图治理-批量立即下线 */
    @RequestMapping(value = "view/offline/batch", method = RequestMethod.POST)
    public SSMResponseMessage<BatchResult> viewOfflineBatch(@RequestBody List<Long> ids) {
        return SSMResponseMessage.success("操作完成", viewService.batchOfflineNow(ids));
    }

    // ===================== 指标/维度治理 =====================

    /** 指标/维度治理-分页列表 */
    @RequestMapping(value = "field/list", method = RequestMethod.POST)
    public SSMResponseMessage<PageInfo<GovernanceFieldVO>> fieldList(@RequestBody GovernanceFieldQueryReq req) {
        PageInfo<GovernanceFieldTask> page = fieldService.list(req.getStatus(), req.getType(),
                req.getOwner(), req.getKeyword(), selfOwnerScope(), req.getCurrPageNo(), req.getPrePageSize());
        return SSMResponseMessage.success("查询成功", mapPage(page, GovernanceFieldVO::from));
    }

    /** 数据范围：超管返回 null（不限制，看全部），普通用户返回本人账号（只看自己负责的） */
    private String selfOwnerScope() {
        User user = UserManager.get();
        if (user == null || roleService.isAdminRole()) {
            return null;
        }
        return user.getName();
    }

    /** 指标/维度治理-详情 */
    @RequestMapping(value = "field/detail", method = RequestMethod.GET)
    public SSMResponseMessage<GovernanceFieldVO> fieldDetail(@RequestParam Long id) {
        return SSMResponseMessage.success("查询成功", GovernanceFieldVO.from(fieldService.getById(id)));
    }

    /** 指标/维度治理-申请延迟30天（SSM 仅通知，不执行下线） */
    @RequestMapping(value = "field/exempt", method = RequestMethod.POST)
    public SSMResponseMessage<Void> fieldExempt(@RequestBody GovernanceExemptReq req) {
        fieldService.exempt(req.getId());
        return SSMResponseMessage.success("已设为豁免");
    }

    /** 指标/维度治理-批量豁免 */
    @RequestMapping(value = "field/exempt/batch", method = RequestMethod.POST)
    public SSMResponseMessage<BatchResult> fieldExemptBatch(@RequestBody List<Long> ids) {
        return SSMResponseMessage.success("操作完成", fieldService.batchExempt(ids));
    }

    // ===================== 审计日志 =====================

    /** 审计日志-分页查询 */
    @RequestMapping(value = "audit/list", method = RequestMethod.POST)
    public SSMResponseMessage<PageInfo<GovernanceAuditLog>> auditList(@RequestBody GovernanceAuditQueryReq req) {
        return SSMResponseMessage.success("查询成功", auditService.list(req.getObjectType(), req.getObjectId(),
                req.getOperator(), req.getKeyword(), req.getCurrPageNo(), req.getPrePageSize()));
    }

    // ===================== 豁免模块/目录 =====================

    /** 豁免配置-分页列表 */
    @RequestMapping(value = "config/exempt/list", method = RequestMethod.POST)
    public SSMResponseMessage<PageInfo<GovernanceExempt>> exemptList(@RequestBody GovernanceExemptQueryReq req) {
        return SSMResponseMessage.success("查询成功",
                configService.listExempt(req.getObjectScope(), req.getCurrPageNo(), req.getPrePageSize()));
    }

    /** 豁免配置-新增 */
    @RequestMapping(value = "config/exempt/add", method = RequestMethod.POST)
    public SSMResponseMessage<Void> exemptAdd(@RequestBody GovernanceExempt exempt) {
        configService.addExempt(exempt);
        return SSMResponseMessage.success("已添加");
    }

    /** 豁免配置-启用/停用 */
    @RequestMapping(value = "config/exempt/toggle", method = RequestMethod.POST)
    public SSMResponseMessage<Void> exemptToggle(@RequestParam Long id, @RequestParam Integer isActive) {
        configService.updateExemptActive(id, isActive);
        return SSMResponseMessage.success("已更新");
    }

    /** 分页对象的内容映射为出参 VO，保留分页元信息 */
    private <S, T> PageInfo<T> mapPage(PageInfo<S> src, Function<S, T> mapper) {
        List<T> list = src.getList().stream().map(mapper).collect(Collectors.toList());
        PageInfo<T> dst = new PageInfo<>(list);
        dst.setTotal(src.getTotal());
        dst.setPageNum(src.getPageNum());
        dst.setPageSize(src.getPageSize());
        dst.setPages(src.getPages());
        return dst;
    }

    // ===================== 灰度 =====================

    /** 判断当前登录用户是否在灰度名单内（未配置灰度名单=全量放开，返回 true） */
    @RequestMapping(value = "gray/user/check", method = RequestMethod.GET)
    public SSMResponseMessage<Boolean> grayUserCheck() {
        User user = UserManager.get();
        String userName = user == null ? null : user.getName();
        return SSMResponseMessage.success("查询成功", configService.inGrayScope(userName));
    }

    // ===================== 定时任务入口（调度平台按 cron 触发，时间点在调度平台配置） =====================

    /**
     * 治理流水线统一入口，按域聚合执行整条流水线：
     * <p>job=view ：豁免重评估 → 扫描打标 → 通知 → 到期自动下线 → 保护期满物理删除。
     * <p>job=field：豁免重评估 → 扫描打标 → 通知（含重复推送）。
     */
    @RequestMapping("job/run")
    public SSMResponseMessage<String> jobRun(@RequestParam String job) {
        long t1 = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        switch (job) {
            case "view":
                step(sb, "豁免重评估", viewService::recheckExempt);
                step(sb, "扫描打标", viewService::scan);
                step(sb, "通知", viewService::notifyPending);
                step(sb, "自动下线", viewService::autoOffline);
                step(sb, "物理删除", viewService::purgeExpired);
                break;
            case "field":
                step(sb, "豁免重评估", fieldService::recheckExempt);
                step(sb, "扫描打标", fieldService::scan);
                step(sb, "通知", fieldService::notifyPending);
                break;
            default:
                return SSMResponseMessage.operationFailed("未知治理任务：" + job + "（仅支持 view / field）");
        }
        String result = sb.toString();
        log.info("[治理Job] {} 完成，耗时 {}ms：{}", job, System.currentTimeMillis() - t1, result);
        return SSMResponseMessage.success(result);
    }

    /** 执行单个步骤，独立容错：单步异常只记录并继续后续步骤 */
    private void step(StringBuilder sb, String name, Supplier<String> task) {
        try {
            sb.append(name).append("：").append(task.get()).append("；");
        } catch (Exception e) {
            log.error("[治理Job] 步骤[{}]执行异常：{}", name, e.getMessage(), e);
            sb.append(name).append(" 异常：").append(e.getMessage()).append("；");
        }
    }
}
