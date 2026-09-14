package com.bi.queryer.ssm.portal.upload.entity;

import lombok.Data;

import java.util.Date;

/**
 * 门户「专题分析报告」静态站点元数据（文件本体在文件服务 / 另一 Web 容器可访问的 URL）。
 */
@Data
public class PortalDynamicReportEntity {

    private String reportId;
    /** 同一 report_id 下从 1 自增的版本号 */
    private Integer version;
    private String menuId;
    private String portalId;
    private String parentMenuId;
    private String reportName;
    /** 表扩展字段，当前写入空串占位 */
    private String ossUrl;
    /** 部署服务返回的 reportRoot（静态站点根路径）；未启用部署时可为空 */
    private String resourceUrl;
    /** html 或 zip */
    private String fileExt;
    /** 报告类型，上传时传入（html 等） */
    private String reportType;
    /** 首页 HTML 相对路径：单文件为上传文件名；ZIP 为包内离根最近的 html/htm（同深度取 ZIP 条目顺序先出现者） */
    private String indexPath;
    private String publishedBy;
    private Date publishedTime;
    private Integer isActive;
    private String createdBy;
    private Date createdTime;
    private String updatedBy;
    private Date updatedTime;
}
