package com.bi.queryer.ssm.governance.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import com.bi.queryer.ssm.governance.entity.GovernanceFieldTask;
import com.bi.queryer.ssm.governance.entity.GovernanceViewTask;
import com.bi.queryer.sys.config.SC;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 治理通知服务：按 owner 聚合，企微 Markdown 优先，失败降级邮件，最多重试 3 次后告警管理员。
 *
 * <p>通道复用平台「BI Queryer通」企业微信推送（与 {@code MailServer} 一致）。
 */
@Service
public class GovernanceNotifyService {

    private static final Logger log = LoggerFactory.getLogger(GovernanceNotifyService.class);

    @Autowired
    private GovernanceConfigService configService;

    /** BI Queryer通企业微信推送地址 */
    private static final String BI_QUERYERTONG_URL = "http://localhost:9010/WeiXinWork/Push/Text";
    private static final String BI_QUERYERTONG_REQUEST_ID = "change-me";
    private static final String SIGN = "多维内容治理";

    /** 最大重试次数 */
    private static final int MAX_RETRY = 3;

    /** 通知内容最多列出的明细条数（避免消息过长） */
    private static final int MAX_LIST = 10;

    /**
     * 视图治理通知内容（企微 Markdown）
     */
    public String buildViewNoticeContent(String owner, List<GovernanceViewTask> tasks) {
        int threshold = configService.noVisitThreshold();
        String ssmUrl = SC.v("ssm.governance.ssm.url");
        StringBuilder sb = new StringBuilder();
        sb.append("⚠️ 您有 ").append(tasks.size()).append(" 个模板视图超过 ").append(threshold).append(" 天无查询，将自动下线\n\n");
        int limit = Math.min(tasks.size(), MAX_LIST);
        for (int i = 0; i < limit; i++) {
            GovernanceViewTask t = tasks.get(i);
            sb.append(">").append("视图名称：").append(StrUtil.nullToEmpty(t.getTplName()))
                    .append("_").append(StrUtil.nullToEmpty(t.getViewName()))
                    .append(" ｜ 原因：").append(StrUtil.nullToEmpty(t.getHitReason()));
            if (t.getPlanOfflineDate() != null) {
                sb.append(" ｜ 计划下线：").append(DateUtil.formatDate(t.getPlanOfflineDate()));
            }
            sb.append("\n");
        }
        if (tasks.size() > MAX_LIST) {
            sb.append("... 等共 ").append(tasks.size()).append(" 个，完整清单请见治理管理后台");
        }
        if (StrUtil.isNotBlank(ssmUrl)) {
            sb.append("\n🔗 页面地址：").append(ssmUrl);
        }
        sb.append("\n\n💡 如需保留，请前往治理管理后台申请延迟30天, 30天后重新评估，如无操作7天后自动下线。");
        return sb.toString();
    }

    /**
     * 指标/维度治理通知内容（企微 Markdown）
     */
    public String buildFieldNoticeContent(String owner, List<GovernanceFieldTask> tasks) {
        int threshold = configService.noVisitThreshold();
        String mgpUrl = SC.v("ssm.governance.mgp.url");
        StringBuilder sb = new StringBuilder();
        sb.append("⚠️ 您有 ").append(tasks.size()).append(" 个指标/维度在多维分析中超过 ").append(threshold).append(" 天无查询\n\n");
        int limit = Math.min(tasks.size(), MAX_LIST);
        for (int i = 0; i < limit; i++) {
            GovernanceFieldTask t = tasks.get(i);
            sb.append(">").append(StrUtil.nullToEmpty(t.getWpName()))
                    .append("（").append(StrUtil.nullToEmpty(t.getWhitePaperCode())).append("）")
                    .append(" ｜ 多维目录：")
                    .append(StrUtil.nullToEmpty(t.getCtgPath()))
                    .append(" ｜ 原因：").append(StrUtil.nullToEmpty(t.getHitReason())).append("\n");
        }
        if (tasks.size() > MAX_LIST) {
            sb.append("... 等共 ").append(tasks.size()).append(" 个，完整清单请见治理管理后台");
        }
        if (StrUtil.isNotBlank(mgpUrl)) {
            sb.append("\n🔗 页面地址：").append(mgpUrl);
        }
        sb.append("\n\n💡 请前往数据管理平台多维目录页面进行下线或到治理管理后台申请延迟30天，30天后重新评估，如无操作将持续提醒。");
        return sb.toString();
    }

    /**
     * 推送给指定用户，失败重试，最终失败告警管理员。
     *
     * @return 是否推送成功
     */
    public boolean notifyUser(String owner, String content) {
        if (StrUtil.isBlank(owner)) {
            return false;
        }
        List<String> users = new ArrayList<>();
        users.add(owner);
        for (int i = 1; i <= MAX_RETRY; i++) {
            if (push(users, content)) {
                return true;
            }
            log.warn("[治理通知] 推送失败，第 {} 次重试，owner={}", i, owner);
        }
        // 重试耗尽，告警管理员
        alertAdmin("治理通知推送失败，owner=" + owner);
        return false;
    }

    private boolean push(List<String> users, String content) {
        try {
            JSONObject body = new JSONObject();
            body.put("Content", content);
            body.put("Sign", SIGN);
            body.put("Users", users);
            String result = HttpRequest.post(BI_QUERYERTONG_URL)
                    .header("RequestID", BI_QUERYERTONG_REQUEST_ID)
                    .body(body.toJSONString())
                    .timeout(10000)
                    .execute().body();
            log.info("[治理通知] 推送结果：{}", result);
            return true;
        } catch (Exception e) {
            log.error("[治理通知] 推送异常：{}", e.getMessage(), e);
            return false;
        }
    }

    private void alertAdmin(String message) {
        try {
            String admin = SC.v("notification.ssm.admin");
            if (StrUtil.isBlank(admin)) {
                return;
            }
            push(Collections.singletonList(admin), "【治理通知告警】" + message);
        } catch (Exception e) {
            log.error("[治理通知] 管理员告警失败：{}", e.getMessage(), e);
        }
    }

    /**
     * 工具：判空集合
     */
    public boolean isEmpty(List<?> list) {
        return CollUtil.isEmpty(list);
    }
}
