package com.bi.queryer.ssm.enums;

/**
 * 数据环境
 */
public enum DataEnv {

    OLD_SSM("old_ssm", "旧版（多维分析）"),
    NEW_MGP("new_mgp", "新版（资产管理平台）");

    private String code;

    private String desc;

    DataEnv(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static DataEnv get(String code) {
        for (DataEnv envType : DataEnv.values()) {
            if (envType.getCode().equals(code)) {
                return envType;
            }
        }
        return OLD_SSM;
    }

}
