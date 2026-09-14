package com.bi.queryer.ssm.portal.template.enums;

import com.bi.queryer.sys.exception.BIException;

import java.util.Arrays;
import java.util.Objects;

/**
 * @Auther: contributor
 * @Date: 2024/6/18 20:28
 * @Description:
 */
public enum AnalysisTemplateLockOpType {

    RELEASE("release"), ACQUIRE("acquire");

    private final String code;

    public static AnalysisTemplateLockOpType codeOf(String code) {
        return Arrays.stream(values()).filter(v -> Objects.equals(v.getCode(), code))
                .findFirst().orElseThrow(() -> new BIException("不支持的操作类型: " + code));
    }

    AnalysisTemplateLockOpType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
