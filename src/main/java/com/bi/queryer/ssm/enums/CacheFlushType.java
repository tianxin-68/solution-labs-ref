package com.bi.queryer.ssm.enums;

public enum CacheFlushType {

    ALL("all","全部"),
    FLUSH_ALL_NEW_MGP("flush_all_new_mgp","刷新来源于资产管理平台的数据"),
    FLUSH_FIELD_BY_CODE("flush_field_by_code","通过编码刷新字段属性"),
    FLUSH_CTG_SORT_ID("flush_ctg_sort_id","刷新目录排序");

    private String code;

    private String desc;

    CacheFlushType(String code, String desc){
        this.code = code;
        this.desc = desc;
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

    public static CacheFlushType get(String code) {
        for (CacheFlushType t : values()) {
            if (t.getCode().equalsIgnoreCase(code)) {
                return t;
            }
        }
        return ALL;
    }

}
