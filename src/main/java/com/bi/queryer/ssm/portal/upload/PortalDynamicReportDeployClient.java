package com.bi.queryer.ssm.portal.upload;

import cn.hutool.core.util.StrUtil;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.config.SystemConfig;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.sys.user.UserManager;
import net.sf.json.JSONObject;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 专题分析报告静态资源部署服务：在配置了 {@code portal.dynamic.report.deploy.baseUrl} 时，
 * 上传 / 更新 / 删除时调用远端 {@code /api/reports/{reportId}/...} 接口。
 */
@Service
public class PortalDynamicReportDeployClient {

    private static final MediaType ZIP = MediaType.parse("application/zip");
    private static final MediaType HTML = MediaType.parse("text/html");

    private static volatile OkHttpClient httpClient;

    private static OkHttpClient client() {
        if (httpClient == null) {
            synchronized (PortalDynamicReportDeployClient.class) {
                if (httpClient == null) {
                    httpClient = new OkHttpClient.Builder()
                            .connectTimeout(30, TimeUnit.SECONDS)
                            .readTimeout(120, TimeUnit.SECONDS)
                            .writeTimeout(120, TimeUnit.SECONDS)
                            .build();
                }
            }
        }
        return httpClient;
    }

    public boolean isEnabled() {
        return StrUtil.isNotBlank(resolveBaseUrl());
    }

    private static String resolveBaseUrl() {
        return SC.v("ssm.agent.platform.base.url", "http://127.0.0.1:2026");
    }

    private static String resolveDfBaseUrl() {
        return SC.v("ssm.agent.df.base.url", "https://datastudio-test.example.com/df-reports/mnt/gfs_share/reports");
    }

    /**
     * POST /api/reports/{reportId}/upload
     *
     * @return 响应中的 reportRoot，写入库表 resource_url
     */
    public String upload(String reportId, byte[] fileBytes, String filename, String reportType, String entry, boolean zipPackage) {
        return postMultipart("/api/reports/" + reportId + "/upload", fileBytes, filename, reportType, entry, zipPackage);
    }

    /**
     * POST /api/reports/{reportId}/update
     */
    public String update(String reportId, byte[] fileBytes, String filename, String reportType, String entry, boolean zipPackage) {
        return postMultipart("/api/reports/" + reportId + "/update", fileBytes, filename, reportType, entry, zipPackage);
    }

    /**
     * POST /api/reports/{reportId}/delete
     */
    public void delete(String reportId) {
        String base = resolveBaseUrl();
        if (StrUtil.isBlank(base)) {
            return;
        }
        String url = base + "/api/reports/" + reportId + "/delete";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("X-Deerflow-Username", UserManager.get().getName())
                .post(RequestBody.create(new byte[0], MediaType.parse("application/octet-stream")))
                .build();
        try (Response response = client().newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new BIException("删除部署报告失败 HTTP " + response.code() + ": " + body);
            }
            parseSuccess(body, "删除部署报告");
        } catch (IOException e) {
            throw new BIException("调用删除部署报告接口失败: " + e.getMessage(), e);
        }
    }

    /**
     * POST /api/reports/{reportId}/start
     */
    public void start(String reportId, String userName) {
        lifecycle(reportId, "start", "启动报告", userName);
    }

    /**
     * POST /api/reports/{reportId}/restart
     */
    public void restart(String reportId, String userName) {
        lifecycle(reportId, "restart", "重启报告", userName);
    }

    /**
     * POST /api/reports/{reportId}/stop
     */
    public void stop(String reportId, String userName) {
        lifecycle(reportId, "stop", "停止报告", userName);
    }

    private void lifecycle(String reportId, String action, String actionLabel, String userName) {
        String base = resolveBaseUrl();
        if (StrUtil.isBlank(base)) {
            return;
        }
        String url = base + "/api/reports/" + reportId + "/" + action;
        Request request = new Request.Builder()
                .url(url)
                .addHeader("X-Deerflow-Username", StrUtil.blankToDefault(userName, UserManager.get().getName()))
                .post(RequestBody.create(new byte[0], MediaType.parse("application/octet-stream")))
                .build();
        try (Response response = client().newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new BIException(actionLabel + "失败 HTTP " + response.code() + ": " + body);
            }
            parseSuccess(body, actionLabel);
        } catch (IOException e) {
            throw new BIException("调用" + actionLabel + "接口失败: " + e.getMessage(), e);
        }
    }

    private static String postMultipart(String path, byte[] fileBytes, String filename, String reportType, String entry, boolean zipPackage) {
        String base = resolveBaseUrl();
        if (StrUtil.isBlank(base)) {
            return "";
        }
        String url = base + path;
        String safeName = StrUtil.blankToDefault(filename, "report");
        MediaType partType = zipPackage ? ZIP : HTML;
        MultipartBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("entry", entry)
                .addFormDataPart("isZipPackage", Boolean.toString(zipPackage))
                .addFormDataPart("packageType", reportType)
                .addFormDataPart("files", safeName, RequestBody.create(fileBytes, partType))
                .build();
        Request request = new Request.Builder()
                .url(url)
                .addHeader("X-Deerflow-Username", UserManager.get().getName())
                .post(body)
                .build();
        try (Response response = client().newCall(request).execute()) {
            String respBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new BIException("报告部署服务 HTTP " + response.code() + ": " + respBody);
            }
            return parseReportRoot(respBody, entry);
        } catch (IOException e) {
            throw new BIException("调用报告部署接口失败: " + e.getMessage(), e);
        }
    }

    private static String parseReportRoot(String responseBody, String filename) {
        if (StrUtil.isBlank(responseBody)) {
            throw new BIException("报告部署服务返回空响应");
        }
        JSONObject jo = JSONObject.fromObject(responseBody.trim());
        if (!jo.optBoolean("success", false)) {
            throw new BIException("报告部署失败: " + jo.optString("message", ""));
        }
        String reportUrl = firstNonBlank(jo.optString("reportUrl", null), jo.optString("report_url", null));
        if (StrUtil.isNotBlank(reportUrl)) {
            return reportUrl;
        }
        String reportRoot = firstNonBlank(jo.optString("reportRoot", null), jo.optString("report_root", null));
        return resolveDfBaseUrl() + reportRoot.trim() + "/" + filename;
    }

    private static void parseSuccess(String responseBody, String actionLabel) {
        if (StrUtil.isBlank(responseBody)) {
            return;
        }
        JSONObject jo;
        try {
            jo = JSONObject.fromObject(responseBody.trim());
        } catch (Exception e) {
            return;
        }
        if (!jo.optBoolean("success", true)) {
            throw new BIException(actionLabel + "失败: " + jo.optString("message", ""));
        }
    }

    private static String firstNonBlank(String a, String b) {
        if (StrUtil.isNotBlank(a)) {
            return a;
        }
        return Objects.toString(b, "");
    }
}
