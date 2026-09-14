package com.bi.queryer.sys.enums;

/**
 * 权限复制类型
 * @author contributor
 */
public enum AuthCopyType {

    COPY_TO_NEW_RPT("copy_to_new_rpt", "将下线报表人员权限复制给新报表"),
    NOT_COPY_AUTH("not_copy_auth", "不复制权限");

    private AuthCopyType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static AuthCopyType get(String code) {
        for (AuthCopyType type : AuthCopyType.values()) {
            if (type.getCode().equalsIgnoreCase(code)) {
                return type;
            }
        }
        return NOT_COPY_AUTH;
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
}
