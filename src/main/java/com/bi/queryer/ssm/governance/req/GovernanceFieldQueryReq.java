package com.bi.queryer.ssm.governance.req;

import lombok.Data;

/**
 * 指标/维度治理列表查询请求
 */
@Data
public class GovernanceFieldQueryReq {

    /** 状态 */
    private String status;

    /** 对象类型：field/dim */
    private String type;

    /** 负责人 */
    private String owner;

    /** 搜索关键字（名称 / 编码） */
    private String keyword;

    /** 分页参数-第几页 */
    private Integer currPageNo = 1;

    /** 分页参数-每页数量 */
    private Integer prePageSize = 10;
}
