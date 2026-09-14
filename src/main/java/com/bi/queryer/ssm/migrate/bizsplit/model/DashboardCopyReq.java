package com.bi.queryer.ssm.migrate.bizsplit.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Auther: contributor
 * @Date: 2026/8/12 17:12
 * @Description:
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardCopyReq {
    private String analysisTplId;
    private String oldBizLine;
    private String newBizLine;
    private String oldBizLineForName;
}
