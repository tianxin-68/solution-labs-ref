package com.bi.queryer.ssm.query.template.enums;

import java.util.Arrays;
import java.util.Objects;

public enum DataTypeEnum {
    OFFLINE("offline", "离线数据"),
    REAL_TIME("rt", "实时数据");

    private final String code;

    private final String name;

    public static DataTypeEnum codeOf(String code) {
        return Arrays.stream(values()).filter(v -> Objects.equals(v.getCode(), code))
                .findFirst().orElse(OFFLINE);
    }

    DataTypeEnum(String code, String name) {
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
