package com.bi.queryer.ssm.inspection.query.template.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InspectionCfgEntity {

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
     * 查询模式
     */
    private String inspectionQueryMode;

    /**
     * 视图id
     */
    private String viewId;

    /**
     * 视图名称
     */
    private String viewName;

}
