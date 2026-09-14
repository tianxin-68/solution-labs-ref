package com.bi.queryer.ssm.portal.upload.vo;

import lombok.Data;

/**
 * 下载：直接返回已存储的资源 URL（由浏览器或前端另存）。
 */
@Data
public class PortalDynamicReportDownloadRsp {

    private String resourceUrl;
    private String fileExt;
}
