package com.bi.queryer.ssm.enums;

/**
 * 环境
 */
public enum Env {

    PROD("prod", "生产环境"),
    UT("ut", "UT环境");

    private String code;

    private String desc;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    Env(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static Env get(String code) {
        for (Env env : Env.values()) {
            if (env.getCode().equals(code)) {
                return env;
            }
        }
        return PROD;
    }

}
