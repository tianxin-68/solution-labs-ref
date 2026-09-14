package com.bi.queryer.ssm.query.ctg.enums;

/**
 * @Author contributor
 * @Date 10:57 2023-11-11
 * @Description 模板分类目录类型
 **/
public enum QueryTemplateCategoryType {
    FAV("fav", "我的收藏", true),
    MY("my", "我的空间", true),
    SHARE("share", "他人分享", true),
    SPACE("space", "共享空间", true),
    SNAPSHOT("snapshot", "快照", false);

    private String id;
    private String desc;
    private Boolean visible;

    private QueryTemplateCategoryType(String id, String desc, Boolean visible){
        this.id = id;
        this.desc = desc;
        this.visible = visible;
    }

    public static QueryTemplateCategoryType get(String ctgStr) {
        for(QueryTemplateCategoryType type : values()){
            if(type.toString().equalsIgnoreCase(ctgStr)) {
                return type;
            }
        }
        return MY;
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
