package com.bi.queryer.ssm.changenotice.entity;

import lombok.Data;

/**
 * 模板字段替换日志，对应表 template_field_replace_log。
 */
@Data
public class TemplateFieldReplaceLog {

    /** 视图 ID */
    private String viewId;

    /** 替换任务 ID */
    private String replaceTaskId;
}
