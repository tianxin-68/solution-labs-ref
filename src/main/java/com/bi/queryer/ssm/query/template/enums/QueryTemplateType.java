package com.bi.queryer.ssm.query.template.enums;

/**
 * 模版类型
 */
public enum QueryTemplateType {
    NORMAL("normal", "普通模版", true),
    SNAPSHOT("snapshot", "快照模版", false),
    ANALYSIS_TEMPLATE_SNAPSHOT("analysis_template_snapshot","看板快照",false);

    private String id;
    private String desc;
    private Boolean visible;

    private QueryTemplateType(String id, String desc, Boolean visible){
        this.id = id;
        this.desc = desc;
        this.visible = visible;
    }

    public static QueryTemplateType get(String ctgStr) {
        for(QueryTemplateType type : values()){
            if(type.toString().equalsIgnoreCase(ctgStr)) {
                return type;
            }
        }
        return NORMAL;
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

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }
}
