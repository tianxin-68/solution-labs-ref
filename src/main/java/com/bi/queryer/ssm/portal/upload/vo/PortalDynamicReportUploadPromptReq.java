package com.bi.queryer.ssm.portal.upload.vo;

import lombok.Data;

@Data
public class PortalDynamicReportUploadPromptReq {

    private String parentMenuId;
    private String reportName;
    private String portalId;
    private String reportId;
}
