package com.bi.queryer.ssm.migrate.bizsplit.controller;

import com.bi.queryer.ssm.migrate.bizsplit.model.DashboardCopyReq;
import com.bi.queryer.ssm.migrate.bizsplit.model.DashboardCopyResult;
import com.bi.queryer.ssm.migrate.bizsplit.model.MetricMigrateExecuteReq;
import com.bi.queryer.ssm.migrate.bizsplit.model.MetricMigrateExecuteRsp;
import com.bi.queryer.ssm.migrate.bizsplit.service.DashboardCopyService;
import com.bi.queryer.ssm.migrate.bizsplit.service.MetricMigrateService;
import com.bi.queryer.sys.common.SSMResponseMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 看板工作台目录迁移（业务线拆分）手动触发入口。
 * 见 doc/02设计/看板工作台目录迁移-业务线拆分执行计划.md：
 * 直接写正式表，不做影子表/预演/提交两阶段；同一入口首次/增量通用，人工调用，不接入定时任务。
 */
@RestController
@Scope("prototype")
@RequestMapping("ssm/migrate/metricMigrate")
@Slf4j
public class MetricMigrateController {

    @Autowired
    private MetricMigrateService migrateService;

    @Autowired(required = false)
    private DashboardCopyService dashboardCopyService;

    /** 执行一次迁移（首次或增量通用）。重复调用只会增量同步尚未迁移的部分 */
    @PostMapping("execute")
    public SSMResponseMessage<MetricMigrateExecuteRsp> execute(@RequestBody MetricMigrateExecuteReq req) {
        try {
            long start = System.currentTimeMillis();
            MetricMigrateExecuteRsp rsp = migrateService.execute(req);
            log.info("[看板工作台目录迁移] 完成，本次处理{}个对象（新建{}个，复用{}个），耗时{}ms",
                    rsp.getTotalProcessed(), rsp.getCreatedCount(), rsp.getReusedCount(), System.currentTimeMillis() - start);
            return SSMResponseMessage.success("迁移执行完成", rsp);
        } catch (Exception e) {
            log.error("[看板工作台目录迁移] 执行失败", e);
            return SSMResponseMessage.operationFailed("迁移执行失败");
        }
    }

    /**
     * 单独复制看板内容本身，不挂菜单树、不写迁移映射表（{@code ssm_migrate_bizsplit_mapping}）——
     * 只是直接调用 {@link DashboardCopyService#copyDashboard} 的轻量/调试入口，方便单独验证某个看板的
     * 复制效果，不用跑一遍完整的 {@link #execute} 目录树迁移。复制出来的新看板不会出现在任何门户目录里，
     * 需要的话要自己再手动挂载；后续再跑 {@code /execute} 时也不会认出这次复制过的看板，会重新复制一份。
     * <p>
     * 复用 {@code MetricMigrateExecuteReq}/{@code MetricMigrateTarget} 的入参结构：
     * {@code target.sourceRootName} 是要复制的看板在 {@code target.portalId} 下的菜单名称（精确匹配，
     * 只在 {@code menuType=analysis_template} 里找）；新老看板挂在同一个门户下（没有单独的"新门户id"概念）。
     */
    @PostMapping("dashboard/copy")
    public SSMResponseMessage<DashboardCopyResult> copyDashboard(@RequestBody DashboardCopyReq req) {
        try {
            DashboardCopyResult copyResult = dashboardCopyService.copyDashboard(req.getAnalysisTplId(),
                    req.getOldBizLine(), req.getNewBizLine(), req.getOldBizLineForName(),"c3c16c767e7446e3a03edc2ece5e4ec2", null);
            return SSMResponseMessage.success("看板复制完成", copyResult);
        } catch (Exception e) {
            log.error("[看板复制] 执行失败", e);
            return SSMResponseMessage.operationFailed("看板复制失败");
        }
    }
}
