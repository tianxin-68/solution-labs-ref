package com.bi.queryer.ssm.portal.upload.vo;

import lombok.Data;

@Data
public class PortalDynamicReportRenameReq {

    private String menuId;
    private String portalId;
    private String newName;
}
