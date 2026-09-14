package com.bi.queryer.ssm.test;

import com.bi.queryer.sys.base.BaseController;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.sys.interceptor.FreeCheckAuthority;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 10:25 2025/12/2
 * @Description TODO
 **/
@Controller
@Scope("prototype")
@RequestMapping("test/stress")
public class StressTestController extends BaseController {

    private static final OkHttpClient httpClient;
    private AtomicInteger count = new AtomicInteger();
    static {
        try {
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                        @Override public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                        @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    }
            };
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            httpClient = new OkHttpClient.Builder()
                    .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true)
                    .readTimeout(300, TimeUnit.SECONDS)
                    .writeTimeout(300, TimeUnit.SECONDS)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 模拟http请求做压力测试
     * {
     *     "concurrent": 1,
     *     "url": "xx",
     *     "u_token":"xxx"
     * }
     * @return
     */
    @RequestMapping("test")
    @FreeCheckAuthority
    @org.springframework.web.bind.annotation.ResponseBody
    public ResponseMessage test() {
        ResponseMessage result = new ResponseMessage();
        this.initStreamParameters();
        // 并发数
        Integer concurrent = this.intValue("concurrent", 1);
        ExecutorService exec = Executors.newFixedThreadPool(concurrent);
        List<JSONObject> templates = loadQueryConfigs();
        List<CompletableFuture<TestQueryResult>> futures = new ArrayList<>();
        int count = concurrent / templates.size();
        for(int i = 0; i < count; i++){
            for(JSONObject templateJson : templates){
                futures.add(CompletableFuture.supplyAsync(()-> query(templateJson), exec));
            }
        }
        List<TestQueryResult> results =  futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
        printResult(results);
        exec.shutdown();
        return result;
    }

    /**
     * 打印压测结果
     * 1、总体响应时长（p90、p95、均值）、成功率、失败率
     * 2、每个接口的压测结果。表格展示：接口名称、调用次数、响应时长（p90、p95、均值）、成功率、失败率
     * 3、若有失败的接口，表格列出明细（名称、失败原因）
     * @param results
     */
    protected void printResult(List<TestQueryResult> results){
        if (results == null || results.isEmpty()) {
            System.out.println("无测试结果");
            return;
        }

        // 1. 总体统计数据
        long totalRequests = results.size();
        long successfulRequests = results.stream().filter(r -> r.success).count();
        long failedRequests = totalRequests - successfulRequests;
        double successRate = (double) successfulRequests / totalRequests * 100;
        double failureRate = (double) failedRequests / totalRequests * 100;

        // 响应时间统计（仅计算成功的请求）
        List<Long> durations = results.stream()
                .filter(r -> r.success)
                .map(r -> r.duration)
                .sorted()
                .collect(Collectors.toList());

        double avgDuration = durations.stream().mapToLong(Long::longValue).average().orElse(0);
        long p90Duration = durations.isEmpty() ? 0 : durations.get((int) (durations.size() * 0.9));
        long p95Duration = durations.isEmpty() ? 0 : durations.get((int) (durations.size() * 0.95));

        System.out.println("=== 压力测试总体结果 ===");
        System.out.println("并发数 \t 成功率 \t 失败率\t p90(s)\t");
        System.out.printf("%s\t %s\t %s\t %s\t ", totalRequests, successRate + "%(" + successfulRequests + ")", failureRate + "%(" + failedRequests + ")", p90Duration/1000.0);

        // 2. 按接口分类统计
        Map<String, List<TestQueryResult>> groupedByInterface = results.stream()
                .collect(Collectors.groupingBy(result -> result.templateName));

        System.out.println("\n=== 各接口详细统计 ===");
        System.out.printf("%s\t %s\t %s\t %s\t %s\t %s\t %s\t\n",
                "接口名称", "调用次数", "成功率(%)", "失败率(%)", "平均耗时(s)", "P90耗时(s)", "P95耗时(s)");

        for (Map.Entry<String, List<TestQueryResult>> entry : groupedByInterface.entrySet()) {
            String interfaceName = entry.getKey();
            List<TestQueryResult> interfaceResults = entry.getValue();

            long interfaceTotal = interfaceResults.size();
            long interfaceSuccess = interfaceResults.stream().filter(r -> r.success).count();
            long interfaceFailed = interfaceTotal - interfaceSuccess;

            double interfaceSuccessRate = (double) interfaceSuccess / interfaceTotal * 100;
            double interfaceFailureRate = (double) interfaceFailed / interfaceTotal * 100;

            List<Long> interfaceDurations = interfaceResults.stream()
                    .filter(r -> r.success)
                    .map(r -> r.duration)
                    .sorted()
                    .collect(Collectors.toList());

            double interfaceAvgDuration = interfaceDurations.stream().mapToLong(Long::longValue).average().orElse(0);
            long interfaceP90Duration = interfaceDurations.isEmpty() ? 0 :
                    interfaceDurations.get(Math.max(0, (int) (interfaceDurations.size() * 0.9) - 1));
            long interfaceP95Duration = interfaceDurations.isEmpty() ? 0 :
                    interfaceDurations.get(Math.max(0, (int) (interfaceDurations.size() * 0.95) - 1));

            System.out.printf("%s\t %s\t %s\t %s\t %s\t %s\t %s\t\n",
                    interfaceName,





                    interfaceTotal,
                    interfaceSuccessRate,
                    interfaceFailureRate,
                    interfaceAvgDuration/1000.0,
                    interfaceP90Duration/1000.0,
                    interfaceP95Duration/1000.0);
        }

        // 3. 失败详情
        List<TestQueryResult> failedResults = results.stream()
                .filter(r -> !r.success)
                .collect(Collectors.toList());

        if (!failedResults.isEmpty()) {
            System.out.println("\n=== 失败请求详情 ===");
            System.out.printf("%s\t %s\t %s\t\n", "序号", "接口名称", "错误信息");

            for (TestQueryResult failedResult : failedResults) {
                System.out.printf("%s\t %s\t %s\t\n",
                        failedResult.index,
                        failedResult.templateName,
                        failedResult.errorMessage);
            }
        }

        System.out.println("\n=== 压测结束 ===");
    }

    protected List<JSONObject> loadQueryConfigs(){
        List<JSONObject> queryConfigs = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JSONObject[] templates = objectMapper.readValue(new File("/Users/contributor/Documents/workspace/bi_queryer/src/main/java/cn/bi_queryer/bi/ssm/test/test_case.json"), JSONObject[].class);
            return Arrays.asList(templates);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return queryConfigs;
    }

    protected TestQueryResult query(JSONObject templateJson){
        TestQueryResult result = new TestQueryResult();
        /*
        String config = templateJson.getString("config");
        UIQueryConfigure queryConfigure = JSON.parseObject(config, UIQueryConfigure.class);
        // 重新设置session id，避免session冲突
        queryConfigure.setSessionId(Guid.id());
        config = BIUtil.toJSONString(queryConfigure);
        templateJson.put("config", config);
         */
        JSONObject cloneTemplateJson = new JSONObject(templateJson);
        cloneTemplateJson.putAll(templateJson);
        cloneTemplateJson.put("isStressTest", "true");
        RequestBody body = RequestBody.create(cloneTemplateJson.toJSONString(), MediaType.get("application/json; charset=utf-8"));
        Request req = new Request.Builder().url(this.stringValue("url"))
                .addHeader("Content-Type", "application/json")
                .addHeader("u_token", this.stringValue("u_token"))
                .post(body).build();

        result.index = count.incrementAndGet();
        result.templateName = cloneTemplateJson.getString("name");
        result.templateId = cloneTemplateJson.getString("id");

        long startNs = System.nanoTime();
        try (Response resp = httpClient.newCall(req).execute()) {
            String bodyStr = resp.body() != null ? resp.body().string() : "{}";
            ResponseMessage queryResult = JSON.parseObject(bodyStr, ResponseMessage.class);
            result.result = queryResult;
            result.success = queryResult.getSuccess();
            result.errorMessage = queryResult.getMessage();
            result.duration = (System.nanoTime() - startNs) / 1_000_000;
        }catch (Exception e){
            e.printStackTrace();
            result.success = false;
            result.errorMessage = e.getMessage();
        }
        return result;
    }

    static class TestQueryResult {
        public ResponseMessage result;
        public boolean success = false;
        public String errorMessage = "未知";
        // 耗时(ms)
        public long duration = -1;
        public Integer index = -1;
        public String templateId;
        public String templateName;

        public TestQueryResult() {
        }

        public TestQueryResult(ResponseMessage result) {
            this.result = result;
            this.success = result.getSuccess();
            this.errorMessage = result.getMessage();
        }
    }
}
