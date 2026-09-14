package com.bi.queryer.ssm.portal.template.enums;

import java.util.Arrays;
import java.util.Objects;

/**
 * @Auther: contributor
 * @Date: 2024/6/28 13:45
 * @Description:
 */
public enum AnalysisTemplateOperationType {
    CREATE("create"),
    UPDATE("update"),
    PUBLISH("publish"),
    OFFLINE("offline"),
    DELETE("delete"),
    ACQUIRE_LOCK("acquireLock"),
    RELEASE_LOCK("releaseLock"),
    ;


    private final String code;

    public static AnalysisTemplateOperationType codeOf(String code) {
        return Arrays.stream(values()).filter(v -> Objects.equals(v.getCode(), code))
                .findFirst().orElse(null);
    }

    AnalysisTemplateOperationType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
