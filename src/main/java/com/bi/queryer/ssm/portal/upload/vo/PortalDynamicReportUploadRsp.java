package com.bi.queryer.ssm.portal.upload.vo;

import lombok.Data;

/**
 * 上传 / 重新上传成功后的结果：业务主键、菜单节点、静态资源访问地址。
 */
@Data
public class PortalDynamicReportUploadRsp {

    private String reportId;
    /** 本次上传对应的版本号 */
    private Integer version;
    private String menuId;
    /** 文件服务返回的可访问 URL */
    private String resourceUrl;
    /** 首页 HTML 相对路径（与库中一致） */
    private String indexPath;
}
