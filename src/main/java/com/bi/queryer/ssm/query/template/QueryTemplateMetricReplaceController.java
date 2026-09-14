package com.bi.queryer.ssm.query.template;

import com.bi.queryer.ssm.query.template.metric.replace.model.TemplateMetricReplaceReq;
import com.bi.queryer.sys.common.SSMResponseMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.*;

/**
 * 查询模板指标替换：按 {@code template_metric_replace} 映射批量更新视图配置。
 */
@RestController
@Scope("prototype")
@Slf4j
@RequestMapping("ssd/template/metric/replace")
public class QueryTemplateMetricReplaceController {

    @Autowired
    private QueryTemplateMetricReplaceService queryTemplateMetricReplaceService;

    /**
     * 对指定查询模板执行指标替换，并按视图写入 {@code template_field_replace_log}。
     */
    @RequestMapping(value = "execute", method = RequestMethod.POST)
    public SSMResponseMessage<Void> execute(@RequestBody TemplateMetricReplaceReq req) {
        try {
            queryTemplateMetricReplaceService.replaceMetricsForTemplate(req.getTplIds(), req.getReplaceTaskId());
            return SSMResponseMessage.success("");
        } catch (Exception e) {
            log.error("execute error", e);
            return SSMResponseMessage.operationFailed("execute error");
        }
    }

    /**
     * 按看板解析查询模板并批量执行指标替换，日志 {@code tpl_id} 记看板 id。
     */
    @RequestMapping(value = "executeByDashboard", method = RequestMethod.POST)
    public SSMResponseMessage<Void> executeByDashboard(@RequestBody TemplateMetricReplaceReq req) {
        try {
            queryTemplateMetricReplaceService.replaceMetricsForDashboard(
                    req.getAnalysisTplId(), req.getViewId(), req.getReplaceTaskId());
            return SSMResponseMessage.success("");
        } catch (Exception e) {
            log.error("executeByDashboard error", e);
            return SSMResponseMessage.operationFailed("executeByDashboard error");
        }
    }

    /**
     * 按备份表将视图配置还原到替换前内容。
     */
    @RequestMapping(value = "rollback", method = RequestMethod.POST)
    public SSMResponseMessage<Void> rollback(@RequestBody TemplateMetricReplaceReq req) {
        try {
            queryTemplateMetricReplaceService.rollbackTemplateConfigFromBak(req.getTplIds());
            return SSMResponseMessage.success("");
        } catch (Exception e) {
            log.error("rollback error", e);
            return SSMResponseMessage.operationFailed("rollback error");
        }
    }
}
