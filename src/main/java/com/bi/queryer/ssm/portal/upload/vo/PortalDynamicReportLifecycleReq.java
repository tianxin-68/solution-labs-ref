package com.bi.queryer.ssm.portal.upload.vo;

import lombok.Data;

/**
 * 报告启动/重启/停止请求
 */
@Data
public class PortalDynamicReportLifecycleReq {

    private String reportId;
    private String userName;
}
