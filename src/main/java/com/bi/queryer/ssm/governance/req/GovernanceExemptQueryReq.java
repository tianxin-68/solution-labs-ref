package com.bi.queryer.ssm.governance.req;

import lombok.Data;

/**
 * 豁免模块/目录列表查询请求
 */
@Data
public class GovernanceExemptQueryReq {

    /** 豁免范围：view/field */
    private String objectScope;

    /** 分页参数-第几页 */
    private Integer currPageNo = 1;

    /** 分页参数-每页数量 */
    private Integer prePageSize = 10;
}
