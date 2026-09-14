package com.bi.queryer.ssm.enums;

/**
 * @Author: contributor
 * @CreateTime: 2024-07-09  10:50
 * @Description: 调度周期
 */
public enum JobType {

    D("D","日"),
    W("W","周"),
    M("M","月"),
    R("R","准实时");

    JobType(String code,String desc) {
        this.code = code;
        this.desc = desc;
    }

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

    public static JobType get(String code) {
        for (JobType t : values()) {
            if (t.getCode().equalsIgnoreCase(code)) {
                return t;
            }
        }
        return D;
    }

}
