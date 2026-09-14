package com.bi.queryer.ssm.portal.upload.vo;

import lombok.Data;

import java.util.Date;

/**
 * 门户展示用详情：名称、发布人、发布时间、资源地址。
 */
@Data
public class PortalDynamicReportDetailRsp {

    private String reportId;
    /** 当前版本号 */
    private Integer version;
    private String menuId;
    private String portalId;
    private String parentMenuId;
    private String reportName;
    private String resourceUrl;
    private String fileExt;
    /** 报告类型（html 等） */
    private String reportType;
    /** 首页 HTML 相对路径，供门户拼接资源根 URL 打开首页 */
    private String indexPath;
    private String ossUrl;
    private String publishedBy;
    private Date publishedTime;
}
