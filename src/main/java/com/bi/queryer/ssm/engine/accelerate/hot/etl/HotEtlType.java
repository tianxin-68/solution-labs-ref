package com.bi.queryer.ssm.engine.accelerate.hot.etl;

/**
 * @Author contributor
 * @Date 18:10 2024/8/22
 * @Description etl管理器类型
 **/
public enum HotEtlType {
    Hive("trino->hive"),
    Doris("hive->doris");
    private String desc;

    private HotEtlType(String desc){
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
