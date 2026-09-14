package com.bi.queryer.ssm.meta.targetValue;

/**
 * @Author contributor
 * @Date 16:08 2024/12/3
 * @Description 数据集单元格
 **/
public class TargetDataSetCell {
    private String code;

    private Object value;

    public TargetDataSetCell() {
    }

    public TargetDataSetCell(String code) {
        this.code = code;
    }

    public TargetDataSetCell(String code, Object value) {
        this.code = code;
        this.value = value;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getKey(){
        return (code + "@" + value).trim();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null){
            return false;
        }
        if(obj instanceof TargetDataSetCell){
            return this.getKey().equals(((TargetDataSetCell)obj).getKey());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.getKey().hashCode();
    }
}
