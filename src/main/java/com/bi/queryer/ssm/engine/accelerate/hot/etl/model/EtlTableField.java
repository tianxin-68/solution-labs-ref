package com.bi.queryer.ssm.engine.accelerate.hot.etl.model;

import com.bi.queryer.sys.db.DataType;

/**
 * @Author contributor
 * @Date 15:52 2024/8/21
 * @Description TODO
 **/
public class EtlTableField {
    private String name;
    private String dataType;
    private String title;
    private boolean isPartition = false;

    public EtlTableField() {
    }

    public EtlTableField(String name, String dataType) {
        this.name = name;
        this.dataType = dataType;
    }

    public EtlTableField(String name, String dataType, String title) {
        this.name = name;
        this.dataType = dataType;
        this.title = title;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDataType() {
        DataType dt = DataType.getType(dataType);
        if(dt.isInteger()){
            dataType = DataType.BigInt.toString().toLowerCase();
        }
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isPartition() {
        return isPartition;
    }

    public void setPartition(boolean partition) {
        isPartition = partition;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null) return false;
        if(this.name == null) return false;
        return this.name.equalsIgnoreCase(((EtlTableField)obj).name);
    }
}
