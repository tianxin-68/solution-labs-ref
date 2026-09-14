package com.bi.queryer.ssm.meta;

/**
 * @Auther: contributor
 * @Date: 2026/1/30 10:19
 * @Description:
 */
public class TableDataSliceCfg {
    // 切片类型 1min / 5min / 10min / 1h
    private String sliceType;
    // 切片字段
    private String sliceField;

    public String getSliceType() {
        return sliceType;
    }

    public void setSliceType(String sliceType) {
        this.sliceType = sliceType;
    }

    public String getSliceField() {
        return sliceField;
    }

    public void setSliceField(String sliceField) {
        this.sliceField = sliceField;
    }
}
