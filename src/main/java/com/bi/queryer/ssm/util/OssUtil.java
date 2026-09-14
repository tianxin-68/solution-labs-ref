package com.bi.queryer.ssm.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.util.Guid;
import com.bi.queryer.util.StringUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class OssUtil {

    private static final int OSS_URL_FETCH_CONCURRENCY = 5;

    public static String upload(byte[] content, String fileName,String extension, String directoryName){
        String fileUrl = "";
        if(content == null || content.length == 0){
            return fileUrl;
        }

       String ossUploadUrl = SC.v("oss.upload.url","http://gateway.example.com:9010/file-upload/v2/file/upload/stream/public/file");

        OkHttpClient client = new OkHttpClient();
        HttpUrl httpUrl = HttpUrl.parse(ossUploadUrl)
                .newBuilder()
                .addQueryParameter("extension", extension)
                .addQueryParameter("directoryName", directoryName)
                .build();
        String tmpDir = "/tmp/agent_new";
        FileUtil.mkdir(tmpDir);

        if(StrUtil.isEmpty(fileName)){
            fileName = Guid.id();
        }

        fileName +=  "." + extension;
        File file = new File(tmpDir + "/" + fileName);
        FileUtil.writeBytes(content, file);

        // 5. 构建 Multipart 请求体
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(), RequestBody.create(MediaType.parse("text/plain"), file))
                .build();

        // 6. 构建请求头
        Request.Builder requestBuilder = new Request.Builder()
                .url(httpUrl)
                .post(requestBody)
                .addHeader("Accept", "*/*")
                .addHeader("Connection", "keep-alive");

        // 如果是鉴权接口，取消注释并填上 token
        // requestBuilder.addHeader("auth-type", "boss");
        // requestBuilder.addHeader("Authorization", "Bearer your-jwt-token-here");

        // 公网上传鉴权需要该请求头
        // requestBuilder.addHeader("orion_biz_fileUpload_appKey", "ext-file-client-ios-materiel");

        Request request = requestBuilder.build();
        // 7. 发送请求
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                JSONObject responseBody = JSONObject.parseObject(response.body().string());
                JSONObject dataJson = responseBody.getJSONObject("data");
                if(dataJson != null){
                    fileUrl = dataJson.getString("Path");
                }
                log.info("上传成功！响应状态码: {},响应内容:{}", response.code(), responseBody);
            }else {
                log.error("上传失败，状态码: {}, 响应内容: {}" , response.code(),  response.body().string());
            }
        } catch (IOException e) {
            log.error("网络请求出错" , e);
        }

        return fileUrl;
    }

    public static String fetchOssUrlBody(String datasetUrl) {
        String csvDataset = "";
        try {
            csvDataset = HttpUtil.createGet(datasetUrl).execute().body();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return csvDataset;
    }

    /**
     * 获取oss链接的内容
     * @param datasetUrl
     * @return
     */
    public String getDataByOssUrl(String datasetUrl) {
        return fetchOssUrlBody(datasetUrl);
    }

    /**
     * 批量按 OSS URL 拉取内容，与 {@link #getDataByOssUrl(String)} 行为一致；线程最大并发为 5。
     *
     * @param datasetUrls URL 集合，null 或空则返回空 Map
     * @return key 为 url，value 为响应体（异常时为空字符串）
     */
    public static Map<String, String> getDataByOssUrls(Collection<String> datasetUrls) {
        if (datasetUrls == null || datasetUrls.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new ConcurrentHashMap<>();
        ExecutorService executor = Executors.newFixedThreadPool(OSS_URL_FETCH_CONCURRENCY);
        try {
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (String url : datasetUrls) {
                if (url == null) {
                    continue;
                }
                final String u = url;
                futures.add(CompletableFuture.runAsync(() -> result.put(u, fetchOssUrlBody(u)), executor));
            }
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } finally {
            executor.shutdown();
        }
        return result;
    }

    public static String getDatasetWithLimit(String csvDataset, int limit) {
        if (!StringUtil.isEmpty(csvDataset)) {
            int lines = 0;
            for (int i = 0; i < csvDataset.length(); i++) {
                if (csvDataset.charAt(i) == '\n') {
                    if (++lines >= limit) {
                        return csvDataset.substring(0, i);
                    }
                }
            }
        }
        return csvDataset;
    }

}
