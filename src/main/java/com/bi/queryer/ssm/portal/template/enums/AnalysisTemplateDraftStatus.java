package com.bi.queryer.ssm.portal.template.enums;


import java.util.Arrays;
import java.util.Objects;

/**
 * @Auther: contributor
 * @Date: 2024/6/19 10:09
 * @Description: 草稿的状态, 用户后端存储
 */
public enum AnalysisTemplateDraftStatus {

    //看板刚创建，未有草稿
    INIT("init"),
    //看板创建，有草稿
    UNPUBLISHED("unpublished"),
    //看板在发布中，没有草稿
    PUBLISHING("publishing"),
    //看板创建，发布完成，此时没有草稿了
    PUBLISHED("published");

    private final String code;

    public static AnalysisTemplateDraftStatus codeOf(String code) {
        return Arrays.stream(values()).filter(v -> Objects.equals(v.getCode(), code))
                .findFirst().orElse(null);
    }

    AnalysisTemplateDraftStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static int hasDraft(String code) {
        AnalysisTemplateDraftStatus draftStatus = codeOf(code);
        return draftStatus == null || draftStatus == UNPUBLISHED ? 1 : 0;
    }
}