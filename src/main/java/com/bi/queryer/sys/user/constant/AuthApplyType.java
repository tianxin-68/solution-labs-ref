package com.bi.queryer.sys.user.constant;

public enum AuthApplyType {

    RPT_AUTH_APPLY("rpt_auth"),
    RPT_ROLE_APPLY("rpt_role"),
    SSD("ssd"),
    SSM("ssm"),
    TABLE_AUTH("table_auth"),
    APP_AUTH_APPLY("app_auth_apply");


    private String type;

    AuthApplyType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public static AuthApplyType getType(String type) {
        for (AuthApplyType value : AuthApplyType.values()) {
            if (value.type.equals(type))
                return value;
        }
        return null;
    }

}
