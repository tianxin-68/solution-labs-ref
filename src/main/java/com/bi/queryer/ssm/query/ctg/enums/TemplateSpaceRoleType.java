package com.bi.queryer.ssm.query.ctg.enums;

/**
 * @Author contributor
 * @Date 20:24 2023-11-17
 * @Description 模板共享空间角色类型
 **/
public enum TemplateSpaceRoleType {
    ADMIN("99-admin", "管理员"),MEMBER("01-member", "成员");

    private String code;

    private String desc;

    private TemplateSpaceRoleType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static TemplateSpaceRoleType get(String roleStr) {
        for(TemplateSpaceRoleType type : TemplateSpaceRoleType.values()){
            if(type.toString().equals(roleStr) || type.getCode().equalsIgnoreCase(roleStr)) {
                return type;
            }
        }
        return MEMBER;
    }

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
