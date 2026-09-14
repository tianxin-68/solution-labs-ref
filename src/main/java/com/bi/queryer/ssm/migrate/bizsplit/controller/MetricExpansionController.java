package com.bi.queryer.ssm.migrate.bizsplit.controller;

import com.bi.queryer.ssm.migrate.bizsplit.model.MetricExpansionExecuteReq;
import com.bi.queryer.ssm.migrate.bizsplit.model.MetricExpansionExecuteRsp;
import com.bi.queryer.ssm.migrate.bizsplit.model.MetricExpansionPromoteRsp;
import com.bi.queryer.ssm.migrate.bizsplit.model.MetricExpansionReplaceTplReq;
import com.bi.queryer.ssm.migrate.bizsplit.model.MetricExpansionReplaceTplRsp;
import com.bi.queryer.ssm.migrate.bizsplit.model.MetricExpansionUpdateTplReq;
import com.bi.queryer.ssm.migrate.bizsplit.model.MetricExpansionUpdateTplRsp;
import com.bi.queryer.ssm.migrate.bizsplit.service.MetricExpansionService;
import com.bi.queryer.sys.common.SSMResponseMessage;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 指标膨胀/替换 HTTP 入口。
 *
 * 职责：
 * - execute：膨胀结果写入影子库；UT 页面经正式读路径可见
     * - promote：应用上线，影子写入新正式 cfg 并切换视图 cfg_id（批内 5 线程并行）
 * - rollback：回滚上线，删除上线新 cfg/dtl 并恢复视图 cfg_id
 *
 * 请求路径前缀：/ssm/template/metric/expansion
 */
@RestController
@Scope("prototype")
@Slf4j
@RequestMapping("ssm/template/metric/expansion")
public class MetricExpansionController {

    @Autowired
    private MetricExpansionService metricExpansionService;

    /**
     * 按批量 viewIds 执行指标膨胀，结果写入影子库（同一视图重复执行则覆盖更新影子）。
     *
     * @param req 执行请求
     * @return 每个 viewId 对应一条结果
     */
    @RequestMapping(value = "execute", method = RequestMethod.POST)
    public SSMResponseMessage<List<MetricExpansionExecuteRsp>> execute(
            @RequestBody MetricExpansionExecuteReq req) {
        try {
            int viewCount = req != null && req.getViewIds() != null ? req.getViewIds().size() : 0;
            System.out.println("[MetricExpansion] execute 开始, viewCount=" + viewCount
                    + ", sourceBusinessline=" + (req != null ? req.getSourceBusinessline() : null)
                    + ", targetBusinessline=" + (req != null ? req.getTargetBusinessline() : null));
            long startMs = System.currentTimeMillis();

            List<MetricExpansionExecuteRsp> results = metricExpansionService.execute(req);

            int successCount = 0;
            int skippedCount = 0;
            int failedCount = 0;
            for (MetricExpansionExecuteRsp item : results) {
                if (StrUtil.isNotEmpty(item.getErrorMessage())) {
                    failedCount++;
                } else if (Boolean.TRUE.equals(item.getSkipped())) {
                    skippedCount++;
                } else {
                    successCount++;
                }
            }
            System.out.println("[MetricExpansion] execute 完成, 耗时=" + (System.currentTimeMillis() - startMs)
                    + "ms, 总数=" + results.size() + ", 成功=" + successCount
                    + ", 跳过=" + skippedCount + ", 失败=" + failedCount);
            return SSMResponseMessage.success("ok", results);
        } catch (Exception e) {
            System.out.println("[MetricExpansion] execute 异常: " + e.getMessage());
            log.error("metric expansion execute error", e);
            return SSMResponseMessage.operationFailed(e.getMessage());
        }
    }

    /**
     * 应用上线：将影子配置写入新正式 cfg/dtl，并将视图 cfg_id 切换到新 cfg。
     * 入参均可不填：body 可空；未传 viewIds 时上线影子表全部视图。
     *
     * @param req 可空
     * @return 逐视图上线结果
     */
    @RequestMapping(value = "promote", method = RequestMethod.POST)
    public SSMResponseMessage<List<MetricExpansionPromoteRsp>> promote(
            @RequestBody(required = false) MetricExpansionExecuteReq req) {
        try {
            List<String> viewIds = req == null ? null : req.getViewIds();
            int viewCount = viewIds != null ? viewIds.size() : 0;
            System.out.println("[MetricExpansion] promote 开始, viewCount="
                    + (viewCount > 0 ? viewCount : "全部影子视图"));
            long startMs = System.currentTimeMillis();

            List<MetricExpansionPromoteRsp> results = metricExpansionService.promote(viewIds);

            int successCount = 0;
            int skippedCount = 0;
            int failedCount = 0;
            for (MetricExpansionPromoteRsp item : results) {
                if (StrUtil.isNotEmpty(item.getErrorMessage())) {
                    failedCount++;
                } else if (Boolean.TRUE.equals(item.getSkipped())) {
                    skippedCount++;
                } else {
                    successCount++;
                }
            }
            System.out.println("[MetricExpansion] promote 完成, 耗时=" + (System.currentTimeMillis() - startMs)
                    + "ms, 总数=" + results.size() + ", 成功=" + successCount
                    + ", 跳过=" + skippedCount + ", 失败=" + failedCount);
            return SSMResponseMessage.success("ok", results);
        } catch (Exception e) {
            System.out.println("[MetricExpansion] promote 异常: " + e.getMessage());
            log.error("metric expansion promote error", e);
            return SSMResponseMessage.operationFailed(e.getMessage());
        }
    }

    /**
     * 回滚上线：将视图 cfg_id 恢复为 promote 前的 old_cfg_id。
     *
     * @param req 仅需 viewIds
     * @return 逐视图回滚结果
     */
    @RequestMapping(value = "rollback", method = RequestMethod.POST)
    public SSMResponseMessage<List<MetricExpansionPromoteRsp>> rollback(
            @RequestBody MetricExpansionExecuteReq req) {
        try {
            List<String> viewIds = req == null ? null : req.getViewIds();
            int viewCount = viewIds != null ? viewIds.size() : 0;
            System.out.println("[MetricExpansion] rollback 开始, viewCount=" + viewCount);
            long startMs = System.currentTimeMillis();

            List<MetricExpansionPromoteRsp> results = metricExpansionService.rollbackPromote(viewIds);

            int successCount = 0;
            int skippedCount = 0;
            int failedCount = 0;
            for (MetricExpansionPromoteRsp item : results) {
                if (StrUtil.isNotEmpty(item.getErrorMessage())) {
                    failedCount++;
                } else if (Boolean.TRUE.equals(item.getSkipped())) {
                    skippedCount++;
                } else {
                    successCount++;
                }
            }
            System.out.println("[MetricExpansion] rollback 完成, 耗时=" + (System.currentTimeMillis() - startMs)
                    + "ms, 总数=" + results.size() + ", 成功=" + successCount
                    + ", 跳过=" + skippedCount + ", 失败=" + failedCount);
            return SSMResponseMessage.success("ok", results);
        } catch (Exception e) {
            System.out.println("[MetricExpansion] rollback 异常: " + e.getMessage());
            log.error("metric expansion rollback error", e);
            return SSMResponseMessage.operationFailed(e.getMessage());
        }
    }

    /**
     * 门户替换：按业务线复制模板并膨胀全部视图，直接写入正式库。
     *
     * 入参 tplId、sourceBusinessline、targetBusinessline 均必填。
     * 不写影子表，详见 MetricExpansionService.replaceTemplateViews。
     *
     * @param req 源模板 id 及源/目标业务线
     * @return 新模板 id 与全部视图的 oldViewId → newViewId 映射
     */
    @RequestMapping(value = "replace/template", method = RequestMethod.POST)
    public SSMResponseMessage<MetricExpansionReplaceTplRsp> replaceTemplate(
            @RequestBody MetricExpansionReplaceTplReq req) {
        try {
            MetricExpansionReplaceTplRsp rsp = metricExpansionService.replaceTemplateViews(req);
            return SSMResponseMessage.success("ok", rsp);
        } catch (Exception e) {
            log.error("metric expansion replace template error", e);
            return SSMResponseMessage.operationFailed(e.getMessage());
        }
    }

    /**
     * 已生成门户模板原地重跑：tplId / viewId / cfgId 不变，直接更新正式 cfg，不写影子表。
     *
     * 入参 tplIds、sourceBusinessline、targetBusinessline 均必填；viewIds 可选。
     * 须曾通过 replace/template 生成且存在 field_mapping 血缘。
     *
     * @param req 目标模板 id 集合及源/目标业务线
     * @return 逐视图更新结果
     */
    @RequestMapping(value = "replace/template/update", method = RequestMethod.POST)
    public SSMResponseMessage<MetricExpansionUpdateTplRsp> updateReplacedTemplate(
            @RequestBody MetricExpansionUpdateTplReq req) {
        try {
            int tplCount = req != null && req.getTplIds() != null ? req.getTplIds().size() : 0;
            System.out.println("[MetricExpansion] replace/template/update 开始, tplCount=" + tplCount
                    + ", sourceBusinessline=" + (req != null ? req.getSourceBusinessline() : null)
                    + ", targetBusinessline=" + (req != null ? req.getTargetBusinessline() : null));
            long startMs = System.currentTimeMillis();

            MetricExpansionUpdateTplRsp rsp = metricExpansionService.updateReplacedTemplateViews(req);

            int updatedCount = 0;
            int failedCount = 0;
            if (rsp.getViewResults() != null) {
                for (MetricExpansionUpdateTplRsp.ViewResult item : rsp.getViewResults()) {
                    if (StrUtil.isNotEmpty(item.getErrorMessage())) {
                        failedCount++;
                    } else if (Boolean.TRUE.equals(item.getUpdated())) {
                        updatedCount++;
                    }
                }
            }
            System.out.println("[MetricExpansion] replace/template/update 完成, 耗时="
                    + (System.currentTimeMillis() - startMs) + "ms, 更新=" + updatedCount + ", 失败=" + failedCount);
            return SSMResponseMessage.success("ok", rsp);
        } catch (Exception e) {
            System.out.println("[MetricExpansion] replace/template/update 异常: " + e.getMessage());
            log.error("metric expansion update replaced template error", e);
            return SSMResponseMessage.operationFailed(e.getMessage());
        }
    }
}
