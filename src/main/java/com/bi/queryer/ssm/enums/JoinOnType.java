package com.bi.queryer.ssm.enums;

import java.util.Arrays;
import java.util.Objects;

/**
 * @Auther: contributor
 * @Date: 2025/11/11 13:47
 * @Description:
 */
public enum JoinOnType {
    EQUAL("equal", "等值匹配"),
    BETWEEN("between", "范围匹配");

    private final String code;

    private final String name;

    public static JoinOnType codeOf(String code) {
        return Arrays.stream(values()).filter(v -> Objects.equals(v.getCode(), code))
                .findFirst().orElse(EQUAL);
    }

    JoinOnType(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
