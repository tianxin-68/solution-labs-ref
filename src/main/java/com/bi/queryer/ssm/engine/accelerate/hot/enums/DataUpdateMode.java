package com.bi.queryer.ssm.engine.accelerate.hot.enums;

/**
 * @Author: contributor
 * @CreateTime: 2024-09-09  17:30
 * @Description: 数据更新
 */
public enum DataUpdateMode {

    FULL("full","全量"),
    INCREMENTAL("incremental","增量");

    private String code;

    private String name;

    DataUpdateMode(String code,String name){
        this.code = code;
        this.name = name;
    }

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

    public static DataUpdateMode get(String str) {
        for(DataUpdateMode d : DataUpdateMode.values()){
            if(d.toString().equalsIgnoreCase(str)) {
                return d;
            }
        }
        return FULL;
    }

}
