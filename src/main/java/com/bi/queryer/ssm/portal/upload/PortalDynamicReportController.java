package com.bi.queryer.ssm.portal.upload;

import com.bi.queryer.ssm.portal.upload.vo.*;
import com.bi.queryer.sys.common.BizBaseResponse;
import com.bi.queryer.sys.interceptor.FreeCheckAuthority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 看板工作台：专题分析报告（静态 HTML/ZIP）的 Web 接口。
 * <p>
 * 负责接收上传、改名、删除等请求；文件落公网文件服务后返回可访问的 {@code resourceUrl}，供门户内嵌或独立站点打开。
 */
@RestController
@Scope("prototype")
@RequestMapping("ssm/portal/analysisReport")
public class PortalDynamicReportController {

    private static final Logger LOG = LoggerFactory.getLogger(PortalDynamicReportController.class);

    @Autowired
    private PortalDynamicReportService portalDynamicReportService;

    /**
     * 首次上传：在指定门户目录下建菜单并落库，返回 reportId、menuId、资源 URL。
     */
    @FreeCheckAuthority
    @PostMapping(value = "upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BizBaseResponse<PortalDynamicReportUploadRsp> upload(@RequestParam("portalId") String portalId,
                                                                @RequestParam("parentMenuId") String parentMenuId,
                                                                @RequestParam("reportName") String reportName,
                                                                @RequestParam("userName") String userName,
                                                                @RequestParam(value = "reportType", required = false, defaultValue = "html") String reportType,
                                                                @RequestParam("file") MultipartFile file) {
        try {
            return BizBaseResponse.success(portalDynamicReportService.upload(portalId, parentMenuId, reportName, userName, reportType, file));
        } catch (Exception e) {
            LOG.error("upload analysis report error, portalId={}, parentMenuId={}", portalId, parentMenuId, e);
            return BizBaseResponse.fail(e.getMessage());
        }
    }

    /**
     * 重新上传：同一 reportId 下插入新版本（version 自增），菜单不变。
     */
    @FreeCheckAuthority
    @PostMapping(value = "reupload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BizBaseResponse<PortalDynamicReportUploadRsp> reupload(@RequestParam("portalId") String portalId,
                                                                  @RequestParam("reportId") String reportId,
                                                                  @RequestParam("userName") String userName,
                                                                  @RequestParam(value = "reportType", required = false, defaultValue = "html") String reportType,
                                                                  @RequestParam("file") MultipartFile file) {
        try {
            return BizBaseResponse.success(portalDynamicReportService.reupload(portalId, reportId, userName, reportType, file));
        } catch (Exception e) {
            LOG.error("reupload analysis report error, portalId={}, reportId={}", portalId, reportId, e);
            return BizBaseResponse.fail(e.getMessage());
        }
    }

    /**
     * 重命名：同步改库里的报告名与门户菜单展示名。
     */
    @PostMapping("rename")
    public BizBaseResponse<Boolean> rename(@RequestBody PortalDynamicReportRenameReq req) {
        try {
            portalDynamicReportService.rename(req);
            return BizBaseResponse.success(Boolean.TRUE);
        } catch (Exception e) {
            LOG.error("rename analysis report error, req={}", req, e);
            return BizBaseResponse.fail(e.getMessage());
        }
    }

    /**
     * 删除：软删菜单并联动软删报告元数据（需门户编辑权限）。
     */
    @PostMapping("delete")
    public BizBaseResponse<Boolean> delete(@RequestBody PortalDynamicReportDeleteReq req) {
        try {
            portalDynamicReportService.delete(req);
            return BizBaseResponse.success(Boolean.TRUE);
        } catch (Exception e) {
            LOG.error("delete analysis report error, req={}", req, e);
            return BizBaseResponse.fail(e.getMessage());
        }
    }

    /**
     * 下载：返回已存文件的访问地址（前端另存或直连拉取）。
     */
    @GetMapping("download")
    public BizBaseResponse<PortalDynamicReportDownloadRsp> download(@RequestParam String reportId) {
        try {
            return BizBaseResponse.success(portalDynamicReportService.download(reportId));
        } catch (Exception e) {
            LOG.error("download analysis report error, reportId={}", reportId, e);
            return BizBaseResponse.fail(e.getMessage());
        }
    }

    /**
     * 详情：供门户展示名称、发布人、发布时间、资源 URL 等（需门户查看权限）。
     */
    @GetMapping("detail")
    public BizBaseResponse<PortalDynamicReportDetailRsp> detail(@RequestParam String reportId,
                                                                @RequestParam(required = false) Integer version) {
        try {
            return BizBaseResponse.success(portalDynamicReportService.getDetail(reportId, version));
        } catch (Exception e) {
            LOG.error("get analysis report detail error, reportId={}, version={}", reportId, version, e);
            return BizBaseResponse.fail(e.getMessage());
        }
    }

    /**
     * 版本列表：按版本号降序返回该报告全部有效版本。
     */
    @GetMapping("versions")
    public BizBaseResponse<List<PortalDynamicReportDetailRsp>> versions(@RequestParam String reportId) {
        try {
            return BizBaseResponse.success(portalDynamicReportService.listVersions(reportId));
        } catch (Exception e) {
            LOG.error("list analysis report versions error, reportId={}", reportId, e);
            return BizBaseResponse.fail(e.getMessage());
        }
    }

    /**
     * 列表：工作台按门户查询已上传报告（需门户编辑权限）。
     */
    @GetMapping("list")
    public BizBaseResponse<List<PortalDynamicReportDetailRsp>> list(@RequestParam String portalId) {
        try {
            List<PortalDynamicReportDetailRsp> list = portalDynamicReportService.listByPortal(portalId);
            return BizBaseResponse.success(list);
        } catch (Exception e) {
            LOG.error("list analysis report error, portalId={}", portalId, e);
            return BizBaseResponse.fail(e.getMessage());
        }
    }

    /**
     * 启动报告服务。
     */
    @FreeCheckAuthority
    @PostMapping("start")
    public BizBaseResponse<Boolean> start(@RequestBody PortalDynamicReportLifecycleReq req) {
        try {
            portalDynamicReportService.start(req);
            return BizBaseResponse.success(Boolean.TRUE);
        } catch (Exception e) {
            LOG.error("start analysis report error, req={}", req, e);
            return BizBaseResponse.fail(e.getMessage());
        }
    }

    /**
     * 重启报告服务。
     */
    @FreeCheckAuthority
    @PostMapping("restart")
    public BizBaseResponse<Boolean> restart(@RequestBody PortalDynamicReportLifecycleReq req) {
        try {
            portalDynamicReportService.restart(req);
            return BizBaseResponse.success(Boolean.TRUE);
        } catch (Exception e) {
            LOG.error("restart analysis report error, req={}", req, e);
            return BizBaseResponse.fail(e.getMessage());
        }
    }

    /**
     * 停止报告服务。
     */
    @FreeCheckAuthority
    @PostMapping("stop")
    public BizBaseResponse<Boolean> stop(@RequestBody PortalDynamicReportLifecycleReq req) {
        try {
            portalDynamicReportService.stop(req);
            return BizBaseResponse.success(Boolean.TRUE);
        } catch (Exception e) {
            LOG.error("stop analysis report error, req={}", req, e);
            return BizBaseResponse.fail(e.getMessage());
        }
    }

    /**
     * 获取上传提示词。
     */
    @PostMapping("prompt/get")
    public BizBaseResponse<String> getUploadPrompt(@RequestBody PortalDynamicReportUploadPromptReq req) {
        try {
            return BizBaseResponse.success(portalDynamicReportService.buildUploadPrompt(req));
        } catch (Exception e) {
            LOG.error("get upload prompt error, req={}", req, e);
            return BizBaseResponse.fail(e.getMessage());
        }
    }
}
