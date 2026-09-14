package com.bi.queryer.ssm.inspection.query.template.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 巡检模版日志表
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InspectionLogEntity {

    /**
     * 巡检主体ID
     */
    private String inspectionSubjectId;

    /**
     * 巡检主体名称
     */
    private String inspectionSubjectName;

    /**
     * 巡检主体类型 查询模版 query_template
     */
    private String inspectionSubjectType;

    /**
     * 巡检主体负责人
     */
    private String inspectionSubjectOwner;

    /**
     * 视图id
     */
    private String viewId;

    /**
     * 视图名称
     */
    private String viewName;

    /**
     * 巡检结果 成功: success 失败：fail
     */
    private String execResult;

    /**
     * 巡检开始时间
     */
    private String beginTime;

    /**
     * 巡检结束时间
     */
    private String endTime;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 批次号
     */
    private Long batchNo;

    /**
     * 数据源 Key，如 Doris_Master
     */
    private String dsKey;

}
