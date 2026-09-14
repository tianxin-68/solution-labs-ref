package com.bi.queryer.ssm.engine.session;

/**
 * @Author contributor
 * @Date 15:48 2024-04-15
 * @Description 查询设置范围
 **/
public enum QuerySessionSettingScope {
    All("所有范围",1),
    USER("用户级别",2),
    TEMPLATE("模板基本",3),
    NONE("无", -1);

    private String desc;

    private Integer sortId;

    QuerySessionSettingScope(String desc, Integer sortId) {
        this.desc = desc;
        this.sortId = sortId;
    }

    public static QuerySessionSettingScope get(String str){
        for(QuerySessionSettingScope s : QuerySessionSettingScope.values()){
            if(s.toString().equalsIgnoreCase(str)){
                return s;
            }
        }
        return NONE;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public Integer getSortId() {
        return sortId;
    }

    public void setSortId(Integer sortId) {
        this.sortId = sortId;
    }
}
