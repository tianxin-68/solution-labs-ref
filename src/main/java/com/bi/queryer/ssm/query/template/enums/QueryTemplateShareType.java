package com.bi.queryer.ssm.query.template.enums;

/**
 * 模版类型
 */
public enum QueryTemplateShareType {
    USER("user", "用户"),
    DEPT("dept", "部门");

    private String id;
    private String desc;

    private QueryTemplateShareType(String id, String desc){
        this.id = id;
        this.desc = desc;
    }

    public static QueryTemplateShareType get(String ctgStr) {
        for(QueryTemplateShareType type : values()){
            if(type.toString().equalsIgnoreCase(ctgStr)) {
                return type;
            }
        }
        return USER;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
