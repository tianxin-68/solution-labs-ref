package com.bi.queryer.ssm.portal.template.enums;

import com.bi.queryer.sys.exception.BIException;

import java.util.Arrays;
import java.util.Objects;

public enum AnalysisTemplateEditExitType {
    QUIT("quit"), SAVE("save");

    private final String code;

    public static AnalysisTemplateEditExitType codeOf(String code) {
        return Arrays.stream(values()).filter(v -> Objects.equals(v.getCode(), code))
                .findFirst().orElseThrow(() -> new BIException("不支持的操作类型: " + code));
    }

    AnalysisTemplateEditExitType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
