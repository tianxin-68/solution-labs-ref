package com.bi.queryer.ssm.enums;

public enum DataAuthMode {
    no_cfg_none("未配置无权限"),
    no_cfg_all("未配置有权限");

    private String desc;

    private DataAuthMode(String desc) {
        this.desc = desc;
    }


    public static DataAuthMode get(String str) {
        for(DataAuthMode m : DataAuthMode.values()){
            if(m.toString().equalsIgnoreCase(str)) {
                return m;
            }
        }
        return no_cfg_none;
    }


    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
