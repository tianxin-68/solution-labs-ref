package com.bi.queryer.ssm.portal.template.enums;

import java.util.Arrays;
import java.util.Objects;

/**
 * @Auther: contributor
 * @Date: 2024/6/19 14:06
 * @Description:
 */
public enum AnalysisTemplateExecMode {
    LOCAL("local"), PROD("prod");

    private final String code;

    public static AnalysisTemplateExecMode codeOf(String code) {
        return Arrays.stream(values()).filter(v -> Objects.equals(v.getCode(), code))
                .findFirst().orElse(LOCAL);
    }

    AnalysisTemplateExecMode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
