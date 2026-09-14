package com.bi.queryer.ssm.meta;

import com.bi.queryer.ssm.enums.DataEnv;
import com.bi.queryer.util.JSONSerializable;
import com.alibaba.fastjson.JSONObject;

/**
 * 主子表配置
 */
public class MetaTablePriSubCfg implements JSONSerializable, Cloneable{

    /**
     * 子数据表ID
     */
    private String subTableId;

    private String subTableName;

    /**
     * 主数据表ID
     */
    private String priTableId;

    private String priTableName;

    private String dataEnv = DataEnv.OLD_SSM.getCode();

    public String getSubTableId() {
        return subTableId;
    }

    public void setSubTableId(String subTableId) {
        this.subTableId = subTableId;
    }

    public String getSubTableName() {
        return subTableName;
    }

    public void setSubTableName(String subTableName) {
        this.subTableName = subTableName;
    }

    public String getPriTableId() {
        return priTableId;
    }

    public void setPriTableId(String priTableId) {
        this.priTableId = priTableId;
    }

    public String getPriTableName() {
        return priTableName;
    }

    public void setPriTableName(String priTableName) {
        this.priTableName = priTableName;
    }

    public String getDataEnv() {
        return dataEnv;
    }

    public void setDataEnv(String dataEnv) {
        this.dataEnv = dataEnv;
    }

    @Override
    public JSONObject toJSON() {
        return null;

    }
}
