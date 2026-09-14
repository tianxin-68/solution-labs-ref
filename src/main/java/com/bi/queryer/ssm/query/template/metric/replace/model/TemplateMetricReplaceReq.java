package com.bi.queryer.ssm.query.template.metric.replace.model;

import lombok.Data;

import java.util.List;

/**
 * 指标替换请求。
 */
@Data
public class TemplateMetricReplaceReq {

    /** 替换任务 ID（前端生成，写入 template_field_replace_log） */
    private String replaceTaskId;

    /** 查询模板 id 列表（execute / rollback） */
    private List<String> tplIds;

    /** 看板 id（executeByDashboard，写入日志 tpl_id） */
    private String analysisTplId;

    /** 视图 id（executeByDashboard，写入日志 view_id） */
    private String viewId;
}
