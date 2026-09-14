package com.bi.queryer.ssm.governance.req;

import lombok.Data;

/**
 * 治理审计日志列表查询请求
 */
@Data
public class GovernanceAuditQueryReq {

    /** 对象类型：view/field */
    private String objectType;

    /** 对象ID */
    private String objectId;

    /** 操作人 */
    private String operator;

    /** 搜索关键字（名称 / ID） */
    private String keyword;

    /** 分页参数-第几页 */
    private Integer currPageNo = 1;

    /** 分页参数-每页数量 */
    private Integer prePageSize = 10;
}
