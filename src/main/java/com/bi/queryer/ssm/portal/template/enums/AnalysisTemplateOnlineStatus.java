package com.bi.queryer.ssm.portal.template.enums;

import java.util.Arrays;
import java.util.Objects;

/**
 * @Auther: contributor
 * @Date: 2024/6/19 10:09
 * @Description: 看板的在线状态
 */
public enum AnalysisTemplateOnlineStatus {

    //初始状态，未上线过
    INIT("init"),
    //看板上线
    ONLINE("online"),
    //看板已经下线
    OFFLINE("offline");


    private final String code;

    public static AnalysisTemplateOnlineStatus codeOf(String code) {
        return Arrays.stream(values()).filter(v -> Objects.equals(v.getCode(), code))
                .findFirst().orElse(null);
    }

    AnalysisTemplateOnlineStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
