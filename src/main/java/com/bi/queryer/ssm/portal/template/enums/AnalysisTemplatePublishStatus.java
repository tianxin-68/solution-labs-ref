package com.bi.queryer.ssm.portal.template.enums;

import java.util.Arrays;
import java.util.Objects;

/**
 * @Auther: contributor
 * @Date: 2024/6/19 15:13
 * @Description: 看板的发布状态，用于前端展示
 */
public enum AnalysisTemplatePublishStatus {
    UNPUBLISHED("unpublished", "未发布"),
    PUBLISHED("published", "已发布"),
    PUBLISHING("publishing","发布中"),
    OFFLINE("offline", "已下线");

    private final String code;

    private final String name;

    public static AnalysisTemplatePublishStatus codeOf(String code) {
        return Arrays.stream(values()).filter(v -> Objects.equals(v.getCode(), code))
                .findFirst().orElse(null);
    }

    AnalysisTemplatePublishStatus(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static String publishStatusCode(String onlineStatusCode) {
        AnalysisTemplateOnlineStatus onlineStatus = AnalysisTemplateOnlineStatus.codeOf(onlineStatusCode);
        if (AnalysisTemplateOnlineStatus.ONLINE.equals(onlineStatus)) {
            return PUBLISHED.getCode();
        } else if (AnalysisTemplateOnlineStatus.OFFLINE.equals(onlineStatus)) {
            return OFFLINE.getCode();
        } else {
            return UNPUBLISHED.getCode();
        }
    }
}
