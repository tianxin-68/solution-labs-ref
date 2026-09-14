package com.bi.queryer.ssm.portal.ai;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.portal.ai.vo.AiSkillPortalCtgBatchReq;
import com.bi.queryer.ssm.portal.ai.vo.AiSkillPortalCtgPortalRsp;
import com.bi.queryer.ssm.portal.ai.vo.AiSkillPortalCtgVO;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.UserTokenManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.network.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 调用 AI Skill 服务，批量获取各门户技能集目录树。
 */
@Service
public class AiSkillPortalCtgClient {

    private static final Logger LOG = LoggerFactory.getLogger(AiSkillPortalCtgClient.class);

    private static final String BATCH_PATH = "/ai/skill/portal/ctg/list/batch";

    /**
     * 批量查询门户技能集目录，返回 portalId -> 根目录列表。
     */
    public Map<String, List<AiSkillPortalCtgVO>> listBatchByPortalIds(List<String> portalIds) {
        if (CollUtil.isEmpty(portalIds)) {
            return Collections.emptyMap();
        }
        List<String> distinctIds = portalIds.stream()
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(distinctIds)) {
            return Collections.emptyMap();
        }
        String baseUrl = resolveBaseUrl();
        if (StrUtil.isBlank(baseUrl)) {
            LOG.warn("ai skill base url not configured, skip portal ctg batch query");
            return Collections.emptyMap();
        }
        String url = trimTrailingSlash(baseUrl) + BATCH_PATH;
        try {
            AiSkillPortalCtgBatchReq req = new AiSkillPortalCtgBatchReq();
            req.setPortalIds(distinctIds);
            Map<String, Object> reqMap = JSON.parseObject(JSON.toJSONString(req), Map.class);
            Map<String, String> headers = buildHeaders();
            String response = HttpUtil.doPost(url, reqMap, "application/json", headers, 30_000);
            return parseBatchResponse(response);
        } catch (Exception e) {
            LOG.error("batch query ai skill portal ctg failed, portalIds={}", distinctIds, e);
            return Collections.emptyMap();
        }
    }

    private static Map<String, List<AiSkillPortalCtgVO>> parseBatchResponse(String response) {
        if (StrUtil.isBlank(response)) {
            return Collections.emptyMap();
        }
        JSONObject root = JSON.parseObject(response);
        if (root == null) {
            return Collections.emptyMap();
        }
        if (!isSuccessResponse(root)) {
            LOG.warn("ai skill portal ctg batch response not success: {}", response);
            return Collections.emptyMap();
        }
        JSONArray data = root.getJSONArray("data");
        if (data == null || data.isEmpty()) {
            return Collections.emptyMap();
        }
        List<AiSkillPortalCtgPortalRsp> rows = data.toJavaList(AiSkillPortalCtgPortalRsp.class);
        Map<String, List<AiSkillPortalCtgVO>> result = new HashMap<>(rows.size());
        for (AiSkillPortalCtgPortalRsp row : rows) {
            if (row == null || StrUtil.isBlank(row.getPortalId())) {
                continue;
            }
            result.put(row.getPortalId(), row.getCtgs() == null ? Collections.emptyList() : row.getCtgs());
        }
        return result;
    }

    private static boolean isSuccessResponse(JSONObject root) {
        if (root.getBooleanValue("success")) {
            return true;
        }
        Integer code = root.getInteger("code");
        return code != null && code == 200;
    }

    private static Map<String, String> buildHeaders() {
        Map<String, String> headers = new HashMap<>(2);
        User user = UserManager.get();
        if (user != null && StrUtil.isNotBlank(user.getName())) {
            headers.put("u_token", UserTokenManager.createByUserName(user.getName()));
        }
        return headers;
    }

    private static String resolveBaseUrl() {
        return SC.v("ssm.ai.skill.base.url", "https://datastudio-test.example.com/analysis-agent-server");
    }

    private static String trimTrailingSlash(String url) {
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }
}
