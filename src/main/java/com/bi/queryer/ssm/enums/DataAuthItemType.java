package com.bi.queryer.ssm.enums;

/**
 * @Author: contributor
 * @CreateTime: 2024-04-26  15:58
 * @Description: 数据权限子项类型
 */
public enum DataAuthItemType {

    ALL("all","全部","-9999"),
    DATASET("dataset","数据集",""),
    CTG("ctg","目录","");

    DataAuthItemType(String code, String name,String value) {
        this.code = code;
        this.name = name;
        this.value = value;
    }

    private String code;

    private String name;

    private String value;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public static DataAuthItemType get(String str) {
        for(DataAuthItemType m : DataAuthItemType.values()){
            if(m.toString().equalsIgnoreCase(str)) {
                return m;
            }
        }
        return CTG;
    }

}
