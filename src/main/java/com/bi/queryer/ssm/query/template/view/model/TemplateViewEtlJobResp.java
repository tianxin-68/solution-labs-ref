package com.bi.queryer.ssm.query.template.view.model;

import lombok.Data;

import java.util.List;

/**
 * @Auther: contributor
 * @Date: 2026/7/2 14:12
 * @Description:
 */
@Data
public class TemplateViewEtlJobResp {
    /**
     * 模板ID
     */
    private String tplId;
    /**
     * 视图ID
     */
    private String viewId;
    /**
     * 数据类型：offline-离线 / near_realtime-准实时 / realtime-实时
     */
    private String dataType;

    /**
     * 数据表名
     */
    private String tableName;

    /**
     * 数据表描述
     */
    private String tableDesc;

    /**
     * 数据表负责人
     */
    private String tableOwner;

    /**
     * 表类型：事实表 / 维表
     */
    private String tableType;

    /**
     * 数据更新依赖的 ETL 作业名（多个逗号分隔）
     */
    private List<String> etlJobs;
}
