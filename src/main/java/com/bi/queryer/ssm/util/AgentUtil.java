package com.bi.queryer.ssm.util;

import cn.hutool.core.text.UnicodeUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.enums.SseEventType;
import com.bi.queryer.ssm.llm.chatSession.req.ChatExecuteContext;
import com.bi.queryer.sys.config.SC;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public abstract class AgentUtil {

    /**
     * 已序列化的 JSON 文本须按 UTF-8 原样写出。
     * 若用 String + application/json，{@code MappingJackson2HttpMessageConverter} 会先于 String 转换器匹配，
     * 可能对字符串再次 JSON 序列化并影响编码；改用 byte[] + OCTET_STREAM 走 {@code ByteArrayHttpMessageConverter} 直写字节。
     */
    private static final MediaType SSE_JSON_PAYLOAD = MediaType.APPLICATION_OCTET_STREAM;

    public static void sendMessage(ChatExecuteContext cxt, SseEventType event, Object msg) throws IOException {
        Map<String, Object> map = new HashMap<>();
        if(event != null) {
            map.put("wfEvent", event.getCode());
        }
        if(msg instanceof JSONObject){
            map.putAll((Map) msg);
        }else {
            if (msg != null) {
                map.put("answer", msg);
            }
        }

        SseEmitter sseEmitter = cxt.getEmitter();
        if(sseEmitter == null){
            return;
        }
        String body = JSONObject.toJSONString(map);
        if(body != null && body.contains("\\u")){
            body = UnicodeUtil.toString(body);
        }
        byte[] payload = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
        synchronized (sseEmitter) {
            sseEmitter.send(SseEmitter.event().id(cxt.getChatId().toString()).data(payload, SSE_JSON_PAYLOAD));
        }


    }

    public static void closeMessage(ChatExecuteContext cxt){
        SseEmitter sseEmitter = cxt.getEmitter();
        if(sseEmitter == null){
            return;
        }
        try {
            AgentUtil.sendMessage(cxt, SseEventType.MESSAGE_END, "");
            //sseEmitter.complete();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void closeMessage(ChatExecuteContext cxt, Duration duration){
        SseEmitter sseEmitter = cxt.getEmitter();
        if(sseEmitter == null){
            return;
        }
        try {
            AgentUtil.sendMessage(cxt, SseEventType.MESSAGE_END, duration.getSeconds());
            sseEmitter.complete();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 读取配置 支持多个地址用英文逗号分隔，随机选取其一用于负载分散。
     */
    public static String getAgentChatServiceBaseUrl() {
        String raw = SC.v("ssm.agent.chat.base.url",
                "https://datastudio-test.example.com/gateway/agent-server-new");
        if (StrUtil.isBlank(raw)) {
            return raw;
        }
        String[] parts = raw.split(",");
        List<String> urls = new ArrayList<>(parts.length);
        for (String p : parts) {
            String t = StrUtil.trim(p);
            if (StrUtil.isNotEmpty(t)) {
                urls.add(t);
            }
        }
        if (urls.isEmpty()) {
            return StrUtil.trim(raw);
        }
        if (urls.size() == 1) {
            return urls.get(0);
        }
        return urls.get(ThreadLocalRandom.current().nextInt(urls.size()));
    }

}
