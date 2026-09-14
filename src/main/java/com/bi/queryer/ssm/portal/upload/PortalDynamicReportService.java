package com.bi.queryer.ssm.portal.upload;

import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.portal.PortalMenuService;
import com.bi.queryer.ssm.portal.PortalService;
import com.bi.queryer.ssm.portal.auth.service.PortalAuthService;
import com.bi.queryer.ssm.portal.entity.Portal;
import com.bi.queryer.ssm.portal.entity.PortalMenu;
import com.bi.queryer.ssm.portal.enums.AnalysisTplVisitType;
import com.bi.queryer.ssm.portal.enums.FuncType;
import com.bi.queryer.ssm.portal.enums.PortalMenuType;
import com.bi.queryer.ssm.portal.enums.ResType;
import com.bi.queryer.ssm.portal.template.AnalysisTemplateLogService;
import com.bi.queryer.ssm.portal.template.vo.AnalysisTemplateVisitLogReq;
import com.bi.queryer.ssm.portal.upload.entity.PortalDynamicReportEntity;
import com.bi.queryer.ssm.portal.upload.vo.*;
import com.bi.queryer.ssm.util.OssUtil;
import com.bi.queryer.ssm.util.WebUtil;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.UserTokenManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.Guid;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

/**
 * 专题分析报告（HTML/ZIP）上传：与 {@link com.bi.queryer.ssm.portal.template.AnalysisTemplateService#create} 类似，
 * 先校验门户编辑权限，再落库并挂载到所选父目录；文件走公网文件服务得到 ossUrl；
 * 若配置 {@code portal.dynamic.report.deploy.baseUrl}，则调用部署服务，将返回的 {@code reportRoot} 写入 resourceUrl。
 */
@Service
@Scope("prototype")
public class PortalDynamicReportService {

    private static final String OSS_DIR = "bigdata/portal_dynamic_report";

    @Autowired
    private BaseDao dao;

    @Autowired
    private PortalDynamicReportDeployClient portalDynamicReportDeployClient;

    @Autowired
    private PortalMenuService portalMenuService;

    @Autowired
    private PortalService portalService;

    @Autowired
    private PortalAuthService portalAuthService;

    /**
     * 新建上传：插入元数据并挂菜单（菜单类型 {@link PortalMenuType#DYNAMIC_ANALYSIS_REPORT}）。
     */
    public PortalDynamicReportUploadRsp upload(String portalId, String parentMenuId, String reportName, String userName, String reportType, MultipartFile file) {
        User user = requireUser(userName);
        checkArgument(StrUtil.isNotBlank(portalId), "门户不能为空");
        checkArgument(StrUtil.isNotBlank(parentMenuId), "分析报告路径不能为空");
        checkArgument(StrUtil.isNotBlank(reportName), "分析报告名称不能为空");
        checkArgument(file != null && !file.isEmpty(), "请上传分析报告文件");
        reportName = reportName.trim();
        checkArgument(reportName.length() <= 256, "分析报告名称过长");

        //checkPortalEdit(portalId);
        assertParentBelongsToPortal(portalId, parentMenuId);

        byte[] fileBytes = readUploadBytes(file);
        String originalName = Objects.toString(file.getOriginalFilename(), "report");
        String fileExt = detectFileExt(originalName);
        String indexPath = resolveIndexPath(originalName, fileExt, fileBytes);
        String ossUrl = uploadBytesToOss(fileBytes, fileExt);
        checkState(StrUtil.isNotBlank(ossUrl), "文件上传失败，未返回访问地址");

        String reportId = Guid.id();
        boolean zipPackage = "zip".equals(fileExt);
        String reportRoot = portalDynamicReportDeployClient.upload(reportId, fileBytes, originalName, reportType, indexPath, zipPackage);

        Date now = new Date();

        PortalMenu portalMenu = PortalMenu.builder()
                .portalId(portalId)
                .menuName(reportName)
                .menuDesc(null)
                .menuType(PortalMenuType.DYNAMIC_ANALYSIS_REPORT.getCode())
                .parentMenuId(parentMenuId)
                .contentRefId(reportId)
                .build();
        String menuId = portalMenuService.addMenu(portalMenu);

        PortalDynamicReportEntity entity = new PortalDynamicReportEntity();
        entity.setReportId(reportId);
        entity.setVersion(1);
        entity.setMenuId(menuId);
        entity.setPortalId(portalId);
        entity.setParentMenuId(parentMenuId);
        entity.setReportName(reportName);
        entity.setOssUrl(ossUrl);
        entity.setResourceUrl(StrUtil.nullToEmpty(reportRoot));
        entity.setFileExt(fileExt);
        entity.setReportType(reportType);
        entity.setIndexPath(indexPath);
        entity.setPublishedBy(user.getName());
        entity.setPublishedTime(now);
        entity.setCreatedBy(user.getName());
        dao.insert("ssm.portal.dynamicReport.insert", entity);

        PortalDynamicReportUploadRsp rsp = new PortalDynamicReportUploadRsp();
        rsp.setReportId(reportId);
        rsp.setVersion(1);
        rsp.setMenuId(menuId);
        rsp.setResourceUrl(StrUtil.nullToEmpty(reportRoot));
        rsp.setIndexPath(indexPath);

        UserManager.remove();
        return rsp;
    }

    /**
     * 生成供 AI Agent 使用的本地上传提示词（打包 ZIP 并调用 upload 接口）。
     */
    public String buildUploadPrompt(PortalDynamicReportUploadPromptReq req) {
        checkArgument(req != null, "参数为空");
        checkArgument(StrUtil.isNotBlank(req.getPortalId()), "门户不能为空");
        String reportName = req.getReportName().trim();
        String userName = UserManager.get().getName();
        String apiBaseUrl = resolveUploadApiBaseUrl();
        if (StringUtils.isEmpty(req.getReportId())) {
            String template = SC.v("ssm.portal.analysisReport.upload.template", "");
            return render(template,
                    req.getPortalId().trim(),
                    req.getParentMenuId().trim(),
                    "",
                    reportName,
                    userName,
                    apiBaseUrl);
        } else {
            String template = SC.v("ssm.portal.analysisReport.reupload.template", "");
            return render(template,
                    req.getPortalId().trim(),
                    "",
                    req.getReportId().trim(),
                    req.getReportId().trim(),
                    userName,
                    apiBaseUrl);
        }
    }

    static String render(String template, String portalId, String parentMenuId, String reportId, String reportName, String userName, String apiBaseUrl) {
        return template
                .replace("{{portalId}}", portalId)
                .replace("{{parentMenuId}}", parentMenuId)
                .replace("{{reportId}}", reportId)
                .replace("{{reportName}}", reportName)
                .replace("{{userName}}", userName)
                .replace("{{apiBaseUrl}}", apiBaseUrl);
    }

    private static String resolveUploadApiBaseUrl() {
        return SC.v("ssm.portal.analysisReport.api.base.url", "");
    }

    /**
     * 工作台：按门户列出已上传报告。
     */
    public List<PortalDynamicReportDetailRsp> listByPortal(String portalId) {
        checkArgument(StrUtil.isNotBlank(portalId), "门户不能为空");
        checkPortalEdit(portalId);
        List<PortalDynamicReportEntity> rows = dao.queryObjectList("ssm.portal.dynamicReport.listByPortalId", portalId, PortalDynamicReportEntity.class);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<PortalDynamicReportDetailRsp> out = new ArrayList<>(rows.size());
        for (PortalDynamicReportEntity e : rows) {
            out.add(toDetailRsp(e));
        }
        return out;
    }

    /**
     * 门户端展示元数据：需门户查看权限。
     */
    public PortalDynamicReportDetailRsp getDetail(String reportId) {
        return getDetail(reportId, null);
    }

    public PortalDynamicReportDetailRsp getDetail(String reportId, Integer version) {
        checkArgument(StrUtil.isNotBlank(reportId), "reportId 不能为空");
        PortalDynamicReportEntity e;
        if (version != null && version > 0) {
            Map<String, Object> params = new HashMap<>(4);
            params.put("reportId", reportId);
            params.put("version", version);
            e = dao.queryObject("ssm.portal.dynamicReport.getByReportIdAndVersion", params, PortalDynamicReportEntity.class);
        } else {
            e = dao.queryObject("ssm.portal.dynamicReport.getLatestByReportId", reportId, PortalDynamicReportEntity.class);
        }
        checkState(e != null, "报告不存在或已删除");
        checkPortalView(e.getPortalId());
        PortalDynamicReportDetailRsp rsp = toDetailRsp(e);
        addDetailVisitLog(e);
        return rsp;
    }

    /**
     * detail 访问成功后的异步访问日志（写入 ssm_analysis_tpl_visit_log）。
     */
    private static void addDetailVisitLog(PortalDynamicReportEntity e) {
        AnalysisTemplateVisitLogReq visitLogReq = AnalysisTemplateVisitLogReq.builder()
                .analysisTplId(e.getReportId())
                .portalId(e.getPortalId())
                .visitType(AnalysisTplVisitType.DYNAMIC_ANALYSIS_REPORT.getCode())
                .resourceVersion(e.getVersion())
                .build();
        AnalysisTemplateLogService.addVisitLog(visitLogReq);
    }

    public PortalDynamicReportDownloadRsp download(String reportId) {
        PortalDynamicReportDetailRsp d = getDetail(reportId);
        PortalDynamicReportDownloadRsp rsp = new PortalDynamicReportDownloadRsp();
        rsp.setResourceUrl(d.getResourceUrl());
        rsp.setFileExt(d.getFileExt());
        return rsp;
    }

    public void rename(PortalDynamicReportRenameReq req) {
        checkArgument(req != null, "参数为空");
        checkArgument(StrUtil.isNotBlank(req.getMenuId()), "reportId 不能为空");
        checkArgument(StrUtil.isNotBlank(req.getPortalId()), "门户不能为空");
        checkArgument(StrUtil.isNotBlank(req.getNewName()), "新名称不能为空");
        String newName = req.getNewName().trim();
        checkArgument(newName.length() <= 256, "名称过长");

        PortalMenu e = portalMenuService.getMenuById(req.getMenuId());
        checkState(Objects.equals(req.getPortalId(), e.getPortalId()), "门户与报告不匹配");
        checkPortalEdit(e.getPortalId());

        User user = requireUser(null);
        Map<String, Object> m = new HashMap<>(8);
        m.put("reportId", e.getContentRefId());
        m.put("reportName", newName);
        m.put("updatedBy", user.getName());
        dao.update("ssm.portal.dynamicReport.updateReportName", m);

        PortalMenu portalMenu = PortalMenu.builder()
                .contentRefId(e.getContentRefId())
                .menuName(newName)
                .updatedBy(user.getName())
                .build();
        portalMenuService.updateMenuNameByContentRefId(portalMenu);
    }

    /**
     * 重新上传：在保留同一 reportId、菜单的前提下插入新版本（version 自增）。
     */
    public PortalDynamicReportUploadRsp reupload(String portalId, String reportId,String userName, String reportType, MultipartFile file) {
        User user = requireUser(userName);
        checkArgument(StrUtil.isNotBlank(portalId), "门户不能为空");
        checkArgument(StrUtil.isNotBlank(reportId), "reportId 不能为空");
        checkArgument(file != null && !file.isEmpty(), "请上传分析报告文件");

        PortalDynamicReportEntity latest = loadActiveReport(reportId);
        checkState(Objects.equals(portalId, latest.getPortalId()), "门户与报告不匹配");
        //checkPortalEdit(portalId);

        byte[] fileBytes = readUploadBytes(file);
        String originalName = Objects.toString(file.getOriginalFilename(), "report");
        String fileExt = detectFileExt(originalName);
        String indexPath = resolveIndexPath(originalName, fileExt, fileBytes);
        String ossUrl = uploadBytesToOss(fileBytes, fileExt);
        checkState(StrUtil.isNotBlank(ossUrl), "文件上传失败，未返回访问地址");

        boolean zipPackage = "zip".equals(fileExt);
        String reportRoot = portalDynamicReportDeployClient.update(reportId, fileBytes, originalName, reportType, indexPath, zipPackage);

        Integer nextVersion = dao.queryObject("ssm.portal.dynamicReport.getNextVersion", reportId, Integer.class);
        if (nextVersion == null || nextVersion < 1) {
            nextVersion = 1;
        }

        Date now = new Date();
        PortalDynamicReportEntity entity = new PortalDynamicReportEntity();
        entity.setReportId(reportId);
        entity.setVersion(nextVersion);
        entity.setMenuId(latest.getMenuId());
        entity.setPortalId(latest.getPortalId());
        entity.setParentMenuId(latest.getParentMenuId());
        entity.setReportName(latest.getReportName());
        entity.setOssUrl(ossUrl);
        entity.setResourceUrl(StrUtil.nullToEmpty(reportRoot));
        entity.setFileExt(fileExt);
        entity.setReportType(reportType);
        entity.setIndexPath(indexPath);
        entity.setPublishedBy(user.getName());
        entity.setPublishedTime(now);
        entity.setCreatedBy(user.getName());
        dao.insert("ssm.portal.dynamicReport.insert", entity);

        PortalDynamicReportUploadRsp rsp = new PortalDynamicReportUploadRsp();
        rsp.setReportId(reportId);
        rsp.setVersion(nextVersion);
        rsp.setMenuId(latest.getMenuId());
        rsp.setResourceUrl(StrUtil.nullToEmpty(reportRoot));
        rsp.setIndexPath(indexPath);

        UserManager.remove();
        return rsp;
    }

    /**
     * 查询报告全部有效版本（按版本号降序）。
     */
    public List<PortalDynamicReportDetailRsp> listVersions(String reportId) {
        checkArgument(StrUtil.isNotBlank(reportId), "reportId 不能为空");
        PortalDynamicReportEntity latest = loadActiveReport(reportId);
        checkPortalView(latest.getPortalId());
        List<PortalDynamicReportEntity> rows = dao.queryObjectList(
                "ssm.portal.dynamicReport.listVersionsByReportId", reportId, PortalDynamicReportEntity.class);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<PortalDynamicReportDetailRsp> out = new ArrayList<>(rows.size());
        for (PortalDynamicReportEntity e : rows) {
            out.add(toDetailRsp(e));
        }
        return out;
    }

    /**
     * 删除：软删菜单（走 {@link PortalMenuService#delete}）并在其事务内联动软删元数据表。
     */
    public void delete(PortalDynamicReportDeleteReq req) {
        checkArgument(req != null, "参数为空");
        checkArgument(StrUtil.isNotBlank(req.getPortalId()), "门户不能为空");

        PortalMenu e = portalMenuService.getMenuById(req.getMenuId());
        checkState(Objects.equals(req.getPortalId(), e.getPortalId()), "门户与报告不匹配");
        checkPortalEdit(e.getPortalId());

        portalDynamicReportDeployClient.delete(e.getContentRefId());
        portalMenuService.delete(e.getMenuId());
    }

    /**
     * 启动报告服务（转发 agent /api/reports/{reportId}/start）。
     */
    public void start(PortalDynamicReportLifecycleReq req) {
        checkArgument(req != null && StrUtil.isNotBlank(req.getReportId()), "reportId不能为空");
        portalDynamicReportDeployClient.start(req.getReportId(), req.getUserName());
    }

    /**
     * 重启报告服务（转发 agent /api/reports/{reportId}/restart）。
     */
    public void restart(PortalDynamicReportLifecycleReq req) {
        checkArgument(req != null && StrUtil.isNotBlank(req.getReportId()), "reportId不能为空");
        portalDynamicReportDeployClient.restart(req.getReportId(), req.getUserName());
    }

    /**
     * 停止报告服务（转发 agent /api/reports/{reportId}/stop）。
     */
    public void stop(PortalDynamicReportLifecycleReq req) {
        checkArgument(req != null && StrUtil.isNotBlank(req.getReportId()), "reportId不能为空");
        portalDynamicReportDeployClient.stop(req.getReportId(), req.getUserName());
    }

    private PortalDynamicReportEntity loadActiveReport(String reportId) {
        PortalDynamicReportEntity e = dao.queryObject("ssm.portal.dynamicReport.getByReportId", reportId, PortalDynamicReportEntity.class);
        checkState(e != null, "报告不存在或已删除");
        return e;
    }

    private static PortalDynamicReportDetailRsp toDetailRsp(PortalDynamicReportEntity e) {
        PortalDynamicReportDetailRsp d = new PortalDynamicReportDetailRsp();
        d.setReportId(e.getReportId());
        d.setVersion(e.getVersion());
        d.setMenuId(e.getMenuId());
        d.setPortalId(e.getPortalId());
        d.setParentMenuId(e.getParentMenuId());
        d.setReportName(e.getReportName());
        d.setResourceUrl(e.getResourceUrl());
        d.setFileExt(e.getFileExt());
        d.setReportType(e.getReportType());
        d.setOssUrl(e.getOssUrl());
        d.setIndexPath(e.getIndexPath());
        d.setPublishedBy(e.getPublishedBy());
        d.setPublishedTime(e.getPublishedTime());
        return d;
    }

    private void assertParentBelongsToPortal(String portalId, String parentMenuId) {
        PortalMenu parent = portalMenuService.getMenuById(parentMenuId);
        checkState(Objects.equals(portalId, parent.getPortalId()), "所选路径不属于当前门户");
    }

    private void checkPortalEdit(String portalId) {
        assertPortalExists(portalId);
        boolean ok = portalAuthService.check(portalId, ResType.PORTAL.getCode(), FuncType.EDIT.getCode());
        checkState(ok, "您没有该门户的编辑权限");
    }

    private void checkPortalView(String portalId) {
        assertPortalExists(portalId);
        boolean ok = portalAuthService.check(portalId, ResType.PORTAL.getCode(), FuncType.VIEW.getCode());
        checkState(ok, "您没有该门户的查看权限");
    }

    private void assertPortalExists(String portalId) {
        Portal portal = portalService.get(portalId);
        checkArgument(portal != null, "业务门户不存在");
        checkArgument(Enabled.value(portal.getIsActive()), "业务门户已下线或删除");
    }

    private static User requireUser(String userName) {
        if (StrUtil.isNotBlank(userName)) {
            User u = new User(userName);
            UserManager.set(u);
            return u;
        } else {
            User u = UserManager.get();
            if (u == null) {
                throw new BIException("用户未登录");
            }
            return u;
        }
    }

    private static byte[] readUploadBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new BIException("读取上传文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * @return 小写 html 或 zip
     */
    private static String detectFileExt(String originalFilename) {
        String lower = Objects.toString(originalFilename, "report").toLowerCase(Locale.ROOT);
        if (lower.endsWith(".html") || lower.endsWith(".htm")) {
            return "html";
        }
        if (lower.endsWith(".zip")) {
            return "zip";
        }
        throw new BIException("仅支持 .html、.htm 或 .zip 文件");
    }

    private static String uploadBytesToOss(byte[] bytes, String fileExt) {
        return OssUtil.upload(bytes, Guid.id(), String.format(".%s", fileExt), OSS_DIR);
    }

    /**
     * HTML：首页即上传文件名；ZIP：若存在 {@code index.html}/{@code index.htm}（任一路径深度）则在其中取离根最近的一条；否则在全部 .html/.htm 中取离根最近者；同深度取 ZIP 条目顺序先出现者。
     */
    private static String resolveIndexPath(String originalFilename, String fileExt, byte[] bytes) {
        if ("html".equals(fileExt)) {
            return basename(originalFilename);
        }
        return resolveZipNearestIndexHtml(bytes);
    }

    private static String basename(String path) {
        String p = Objects.toString(path, "report");
        int i = Math.max(p.lastIndexOf('/'), p.lastIndexOf('\\'));
        return i >= 0 ? p.substring(i + 1) : p;
    }

    private static String normalizeZipEntryPath(String entryName) {
        String n = entryName.replace('\\', '/');
        while (n.startsWith("./")) {
            n = n.substring(2);
        }
        if (n.startsWith("/")) {
            n = n.substring(1);
        }
        return n;
    }

    private static boolean isHtmlFileName(String name) {
        String low = name.toLowerCase(Locale.ROOT);
        return low.endsWith(".html") || low.endsWith(".htm");
    }

    private static String zipEntryBasename(String normalizedPath) {
        int i = normalizedPath.lastIndexOf('/');
        return i >= 0 ? normalizedPath.substring(i + 1) : normalizedPath;
    }

    /**
     * 条目文件名为 index.html / index.htm（不区分大小写）。
     */
    private static boolean isIndexHtmlEntry(String normalizedPath) {
        if (StrUtil.isBlank(normalizedPath) || normalizedPath.endsWith("/")) {
            return false;
        }
        String base = zipEntryBasename(normalizedPath).toLowerCase(Locale.ROOT);
        return "index.html".equals(base) || "index.htm".equals(base);
    }

    /**
     * 路径相对 ZIP 根的目录深度：根下文件为 0，每多一级目录 +1。
     */
    private static int zipPathDepthFromRoot(String normalizedPath) {
        if (StrUtil.isBlank(normalizedPath) || normalizedPath.endsWith("/")) {
            return Integer.MAX_VALUE;
        }
        int depth = 0;
        for (int i = 0; i < normalizedPath.length(); i++) {
            if (normalizedPath.charAt(i) == '/') {
                depth++;
            }
        }
        return depth;
    }

    /**
     * ZIP 首页：若存在 index.html / index.htm，则在所有匹配项中取路径深度最小者（离根最近）；否则在全部 .html/.htm 中取深度最小者。
     * 同深度时保留 {@link ZipInputStream} 中先出现的条目。
     */
    private static String resolveZipNearestIndexHtml(byte[] zipBytes) {
        String bestIndex = null;
        int bestIndexDepth = Integer.MAX_VALUE;
        String bestAny = null;
        int bestAnyDepth = Integer.MAX_VALUE;
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                try {
                    if (!entry.isDirectory()) {
                        String norm = normalizeZipEntryPath(entry.getName());
                        if (isHtmlFileName(norm)) {
                            int d = zipPathDepthFromRoot(norm);
                            if (isIndexHtmlEntry(norm) && d < bestIndexDepth) {
                                bestIndexDepth = d;
                                bestIndex = norm;
                            }
                            if (d < bestAnyDepth) {
                                bestAnyDepth = d;
                                bestAny = norm;
                            }
                        }
                    }
                } finally {
                    drainZipEntry(zis);
                    zis.closeEntry();
                }
            }
        } catch (IOException e) {
            throw new BIException("解析压缩包失败: " + e.getMessage(), e);
        }
        if (bestIndex != null) {
            return bestIndex;
        }
        if (bestAny == null || bestAnyDepth == Integer.MAX_VALUE) {
            throw new BIException("压缩包中未找到 HTML 文件");
        }
        return bestAny;
    }

    private static void drainZipEntry(ZipInputStream zin) throws IOException {
        byte[] buf = new byte[4096];
        int n;
        while ((n = zin.read(buf)) != -1) {
            // discard
        }
    }
}
